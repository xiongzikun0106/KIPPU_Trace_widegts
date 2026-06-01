package com.kippu.trace.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.kippu.trace.MainActivity
import com.kippu.trace.R
import com.kippu.trace.data.AppDatabase
import com.kippu.trace.model.DateEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 小组件数据更新逻辑
object TraceWidgetUpdater {
    private const val PREFS_NAME = "trace_widget_prefs"
    private const val PREF_PREFIX_KEY = "appwidget_"
    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 保存小组件绑定的事件ID
    fun saveWidgetEventId(context: Context, appWidgetId: Int, eventId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(PREF_PREFIX_KEY + appWidgetId, eventId)
            .apply()
    }

    // 获取小组件绑定的事件ID
    private fun getWidgetEventId(context: Context, appWidgetId: Int): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(PREF_PREFIX_KEY + appWidgetId, -1L)
    }

    // 移除小组件绑定关系
    fun removeWidgetPreference(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_PREFIX_KEY + appWidgetId)
            .apply()
    }

    // 请求更新所有小组件
    fun requestAllUpdate(context: Context) {
        val appContext = context.applicationContext
        updateScope.launch {
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            updateProvider(appContext, appWidgetManager, TraceWidget2x2Provider::class.java, TraceWidgetSize.TWO_BY_TWO)
            updateProvider(appContext, appWidgetManager, TraceWidget3x2Provider::class.java, TraceWidgetSize.THREE_BY_TWO)
            updateProvider(appContext, appWidgetManager, TraceWidget4x2Provider::class.java, TraceWidgetSize.FOUR_BY_TWO)
        }
    }

    // 请求更新指定类型的组件
    fun requestProviderUpdate(
        context: Context,
        providerClass: Class<out TraceWidgetProvider>,
        widgetSize: TraceWidgetSize,
        appWidgetIds: IntArray? = null,
        onComplete: (() -> Unit)? = null,
    ) {
        val appContext = context.applicationContext
        updateScope.launch {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(appContext)
                updateProvider(appContext, appWidgetManager, providerClass, widgetSize, appWidgetIds)
            } finally {
                onComplete?.invoke()
            }
        }
    }

    private suspend fun updateProvider(
        context: Context,
        appWidgetManager: AppWidgetManager,
        providerClass: Class<out TraceWidgetProvider>,
        widgetSize: TraceWidgetSize,
        requestedIds: IntArray? = null,
    ) {
        val appWidgetIds = requestedIds
            ?.takeIf { it.isNotEmpty() }
            ?: appWidgetManager.getAppWidgetIds(ComponentName(context, providerClass))

        for (appWidgetId in appWidgetIds) {
            val eventId = getWidgetEventId(context, appWidgetId)
            val event = if (eventId != -1L) {
                AppDatabase.getDatabase(context).eventDao().getEventById(eventId)
            } else null
            
            appWidgetManager.updateAppWidget(
                appWidgetId,
                buildRemoteViews(context, widgetSize, event, appWidgetId),
            )
        }
    }

    // 构建远程视图并填充数据
    private fun buildRemoteViews(context: Context, widgetSize: TraceWidgetSize, event: DateEvent?, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, widgetSize.layoutRes)
        
        // 检测系统是否处于暗色模式
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 渲染背景如果是空事件则渲染加号
        views.setImageViewBitmap(
            R.id.widget_background,
            TraceWidgetBackgroundRenderer.render(event, widgetSize, isDark),
        )

        // 强制隐藏左上角标识（置顶/应用名）
        views.setViewVisibility(R.id.widget_label, View.GONE)

        if (event == null) {
            // 空状态隐藏所有文字内容
            views.setViewVisibility(R.id.widget_title, View.GONE)
            views.setViewVisibility(R.id.widget_prefix, View.GONE)
            views.setViewVisibility(R.id.widget_date, View.GONE)
            views.setViewVisibility(R.id.widget_days, View.GONE)
            views.setViewVisibility(R.id.widget_day_unit, View.GONE)
            
            // 点击打开选择界面
            views.setOnClickPendingIntent(R.id.widget_root, createConfigIntent(context, appWidgetId))
        } else {
            // 有效状态显示并填充数据
            views.setViewVisibility(R.id.widget_title, View.VISIBLE)
            views.setViewVisibility(R.id.widget_days, View.VISIBLE)
            views.setViewVisibility(R.id.widget_day_unit, View.VISIBLE)

            val dateText = formatTargetDate(event.targetDate)
            val days = calculateDays(event.targetDate).toString()
            val prefix = context.getString(if (event.isFuture) R.string.label_until else R.string.label_since)

            views.setTextViewText(R.id.widget_title, event.title)
            views.setTextViewText(R.id.widget_prefix, prefix)
            views.setTextViewText(R.id.widget_date, dateText)
            views.setTextViewText(R.id.widget_days, days)
            views.setTextViewText(R.id.widget_day_unit, context.getString(R.string.day_unit))
            
            views.setViewVisibility(R.id.widget_prefix, View.VISIBLE)
            views.setViewVisibility(R.id.widget_date, View.VISIBLE)
            
            applySizeTuning(views, widgetSize)
            // 无论是否有内容点击均进入更换卡片选择界面
            views.setOnClickPendingIntent(R.id.widget_root, createConfigIntent(context, appWidgetId))
        }
        
        return views
    }

    private fun applySizeTuning(views: RemoteViews, widgetSize: TraceWidgetSize) {
        when (widgetSize) {
            TraceWidgetSize.TWO_BY_TWO -> {
                views.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, 16f)
                views.setTextViewTextSize(R.id.widget_days, TypedValue.COMPLEX_UNIT_SP, 40f)
                views.setTextViewTextSize(R.id.widget_day_unit, TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            TraceWidgetSize.THREE_BY_TWO -> {
                views.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, 17f)
                views.setTextViewTextSize(R.id.widget_days, TypedValue.COMPLEX_UNIT_SP, 42f)
                views.setTextViewTextSize(R.id.widget_day_unit, TypedValue.COMPLEX_UNIT_SP, 13f)
            }
            TraceWidgetSize.FOUR_BY_TWO -> {
                views.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, 22f)
                views.setTextViewTextSize(R.id.widget_days, TypedValue.COMPLEX_UNIT_SP, 48f)
                views.setTextViewTextSize(R.id.widget_day_unit, TypedValue.COMPLEX_UNIT_SP, 14f)
            }
        }
    }

    private fun createOpenAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createConfigIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, com.kippu.trace.WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun calculateDays(targetDateMillis: Long): Long {
        val targetLocalDate = Instant.ofEpochMilli(targetDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(today, targetLocalDate).let { if (it < 0) -it else it }
    }

    private fun formatTargetDate(targetDateMillis: Long): String {
        return Instant.ofEpochMilli(targetDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    }
}
