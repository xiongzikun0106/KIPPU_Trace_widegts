package com.kippu.trace.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

object TraceWidgetUpdater {
    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun requestAllUpdate(context: Context) {
        val appContext = context.applicationContext
        updateScope.launch {
            updateAll(appContext)
        }
    }

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
                updateProvider(appContext, providerClass, widgetSize, appWidgetIds)
            } finally {
                onComplete?.invoke()
            }
        }
    }

    private suspend fun updateAll(context: Context) {
        val event = loadDisplayEvent(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateProviderWithEvent(context, appWidgetManager, TraceWidget2x2Provider::class.java, TraceWidgetSize.TWO_BY_TWO, event)
        updateProviderWithEvent(context, appWidgetManager, TraceWidget3x2Provider::class.java, TraceWidgetSize.THREE_BY_TWO, event)
        updateProviderWithEvent(context, appWidgetManager, TraceWidget4x2Provider::class.java, TraceWidgetSize.FOUR_BY_TWO, event)
    }

    private suspend fun updateProvider(
        context: Context,
        providerClass: Class<out TraceWidgetProvider>,
        widgetSize: TraceWidgetSize,
        appWidgetIds: IntArray?,
    ) {
        val event = loadDisplayEvent(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateProviderWithEvent(context, appWidgetManager, providerClass, widgetSize, event, appWidgetIds)
    }

    private suspend fun loadDisplayEvent(context: Context): DateEvent? = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(context).eventDao().getAllEventsOnce().firstOrNull()
    }

    private fun updateProviderWithEvent(
        context: Context,
        appWidgetManager: AppWidgetManager,
        providerClass: Class<out TraceWidgetProvider>,
        widgetSize: TraceWidgetSize,
        event: DateEvent?,
        requestedIds: IntArray? = null,
    ) {
        val appWidgetIds = requestedIds
            ?.takeIf { it.isNotEmpty() }
            ?: appWidgetManager.getAppWidgetIds(ComponentName(context, providerClass))

        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(
                appWidgetId,
                buildRemoteViews(context, widgetSize, event),
            )
        }
    }

    private fun buildRemoteViews(context: Context, widgetSize: TraceWidgetSize, event: DateEvent?): RemoteViews {
        val views = RemoteViews(context.packageName, widgetSize.layoutRes)
        val dateText = event?.let { formatTargetDate(it.targetDate) } ?: context.getString(R.string.widget_no_date)
        val days = event?.let { calculateDays(it.targetDate).toString() } ?: "--"
        val prefix = event?.let {
            context.getString(if (it.isFuture) R.string.label_until else R.string.label_since)
        } ?: ""

        views.setImageViewBitmap(
            R.id.widget_background,
            TraceWidgetBackgroundRenderer.render(event, widgetSize),
        )
        views.setTextViewText(
            R.id.widget_label,
            if (event?.isPinned == true) context.getString(R.string.pinned_label) else context.getString(R.string.app_name),
        )
        views.setTextViewText(R.id.widget_title, event?.title ?: context.getString(R.string.widget_no_event))
        views.setTextViewText(R.id.widget_prefix, prefix)
        views.setTextViewText(R.id.widget_date, dateText)
        views.setTextViewText(R.id.widget_days, days)
        views.setTextViewText(R.id.widget_day_unit, if (event == null) "" else context.getString(R.string.day_unit))
        views.setViewVisibility(R.id.widget_prefix, if (event == null) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_date, if (event == null) View.GONE else View.VISIBLE)
        applySizeTuning(views, widgetSize)
        views.setOnClickPendingIntent(R.id.widget_root, createOpenAppIntent(context))
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
                views.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, 19f)
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
