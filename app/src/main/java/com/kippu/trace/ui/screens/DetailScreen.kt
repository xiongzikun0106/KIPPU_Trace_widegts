package com.kippu.trace.ui.screens

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.kippu.trace.model.DateEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun DetailScreen(
    events: List<DateEvent>,
    initialEventId: Long,
    onBack: () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 1. Force fully transparent status and navigation bars
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            // 2. Enable drawing in the cutout (punch hole) area
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            
            // 3. Setup bar icons color (White text/icons for dark immersive background)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无数据")
        }
        return
    }

    val initialIndex = events.indexOfFirst { it.id == initialEventId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { events.size })

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { events[it].id }
        ) { pageIndex ->
            EventDetailItem(event = events[pageIndex])
        }
    }
}

@Composable
fun EventDetailItem(event: DateEvent) {
    val targetLocalDate = Instant.ofEpochMilli(event.targetDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(LocalDate.now(), targetLocalDate).let { if (it < 0) -it else it }

    val animatedDays = remember { Animatable(0f) }
    LaunchedEffect(event.id) {
        animatedDays.snapTo(0f)
        animatedDays.animateTo(
            targetValue = days.toFloat(),
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (event.backgroundUri != null) {
            AsyncImage(
                model = event.backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = event.maskOpacity)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() 
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val prefix = if (event.isFuture) "还有" else "已经"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = prefix,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = animatedDays.value.toInt().toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = com.kippu.trace.ui.theme.NumberFontFamily,
                        color = Color.White
                    )
                )
            }
            
            val datePrefix = if (event.isFuture) "距离" else "自从"
            Text(
                text = "$datePrefix $targetLocalDate",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 2.sp
                )
            )
        }
    }
}
