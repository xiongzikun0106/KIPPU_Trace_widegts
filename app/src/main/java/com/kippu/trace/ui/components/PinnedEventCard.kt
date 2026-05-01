package com.kippu.trace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kippu.trace.model.DateEvent
import com.kippu.trace.model.DisplayMode
import com.kippu.trace.utils.TextUtils
import com.kippu.trace.utils.fadeRightEdge
import com.kippu.trace.utils.fadeLastLineEdge
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
fun PinnedEventCard(
    event: DateEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetLocalDate = Instant.ofEpochMilli(event.targetDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(today, targetLocalDate).let { if (it < 0) -it else it }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (event.backgroundUri != null) {
                AsyncImage(
                    model = event.backgroundUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = event.maskOpacity)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                val visualWidth = TextUtils.getVisualWidth(event.title)
                
                if (visualWidth > 15.0f) {
                    // "Seriously Excessive" Layout: Split Left/Right with Title Stacking
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Title on the Left (Stacks and Wraps)
                        Text(
                            text = TextUtils.forceCharacterWrap(event.title),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 16.dp)
                                .fadeLastLineEdge(0.3f, 0.25f),
                            maxLines = 4,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 8f),
                                fontWeight = FontWeight.Bold,
                                lineHeight = 28.sp,
                                lineBreak = LineBreak.Simple,
                                hyphens = Hyphens.None
                            )
                        )

                        // Days and Date on the Right (Right-Aligned)
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy((-4).dp)
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = days.toString(),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        color = Color.White,
                                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 12f),
                                        fontSize = 48.sp 
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "天",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 16.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                            Text(
                                text = targetLocalDate.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                    }
                } else {
                    // Intermediate Collision: title > 5.5 chars OR title > 4.0 chars and many days
                    val isCollision = visualWidth > 5.5f || (visualWidth >= 4.0f && days >= 1000)
                    
                    if (isCollision) {
                        // Optimized Layout: Title ON TOP of Days
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart),
                            verticalArrangement = Arrangement.spacedBy(4.dp) // Reasonable gap between blocks
                        ) {
                            Text(
                                text = event.title,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fadeRightEdge(0.25f),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = Color.White,
                                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 8f),
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp // Tighter line spacing for title text
                                )
                            )

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = days.toString(),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        color = Color.White,
                                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 12f),
                                        fontSize = 48.sp 
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "天",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 16.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }

                            Text(
                                text = targetLocalDate.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Normal
                                ),
                                modifier = Modifier.offset(y = (-4).dp) // Slight adjustment to pull date closer to unit
                            )
                        }
                    } else {
                        // Standard Layout
                        Column(modifier = Modifier.align(Alignment.BottomStart)) {
                            Text(
                                text = event.title,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fadeRightEdge(0.25f),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = Color.White,
                                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 8f)
                                )
                            )
                            Text(
                                text = targetLocalDate.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = days.toString(),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    color = Color.White,
                                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 12f)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "天",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 16.sp
                                ),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }
            }

            // Pinned Badge moved outside to allow finer padding control
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp) // Closer to edge to align with 24dp corners
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "▷ 置顶",
                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                )
            }
        }
    }
}

@Preview
@Composable
fun PinnedEventCardPreview() {
    val mockEvent = DateEvent(
        title = "我的生日",
        targetDate = System.currentTimeMillis() + 86400000 * 23,
        isFuture = true,
        mode = DisplayMode.COUNT_DOWN,
        isPinned = true,
        backgroundUri = "https://images.unsplash.com/photo-1490730141103-6cac27aaab94"
    )
    com.kippu.trace.ui.theme.KIPPU_TraceTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PinnedEventCard(event = mockEvent, onClick = {})
        }
    }
}
