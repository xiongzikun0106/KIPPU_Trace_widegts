package com.kippu.trace.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kippu.trace.model.DateEvent
import kotlinx.coroutines.launch
import com.kippu.trace.ui.components.NormalEventCard
import com.kippu.trace.ui.components.PinnedEventCard
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    events: List<DateEvent>,
    onAddClick: () -> Unit,
    onEventClick: (DateEvent) -> Unit,
    onDeleteEvent: (DateEvent) -> Unit,
) {
    val pinnedEvents = events.filter { it.isPinned }
    val otherEvents = events.filter { !it.isPinned }
    
    var eventToDelete by remember { mutableStateOf<DateEvent?>(null) }

    if (eventToDelete != null) {
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除“${eventToDelete?.title}”吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        eventToDelete?.let { onDeleteEvent(it) }
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TimeTrace",
                            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text = "时间轴",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "点击右上角 + 记录你的 TimeTrace",
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.secondary)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 120.dp // Increased bottom padding to ensure last item is above nav bar
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pinned Cards List
                items(pinnedEvents, key = { "pinned_${it.id}" }) { event ->
                    SwipeToDeleteWrapper(
                        isResetRequested = eventToDelete == null,
                        onTrashClick = { eventToDelete = event }
                    ) {
                        PinnedEventCard(
                            event = event,
                            onClick = { onEventClick(event) }
                        )
                    }
                }

                // Normal Cards List
                items(otherEvents, key = { it.id }) { event ->
                    SwipeToDeleteWrapper(
                        isResetRequested = eventToDelete == null,
                        onTrashClick = { eventToDelete = event }
                    ) {
                        NormalEventCard(
                            event = event,
                            onClick = { onEventClick(event) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeToDeleteWrapper(
    isResetRequested: Boolean,
    onTrashClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val revealWidth = with(density) { 80.dp.toPx() }
    
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isResetRequested) {
        if (isResetRequested && (offsetX.value != 0f)) {
            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val currentX = offsetX.value
                        val newX = currentX + dragAmount
                        
                        // Apply rubber-band resistance
                        val effectiveDrag = if (newX < -revealWidth) {
                            val overflow = abs(newX + revealWidth)
                            dragAmount * (1f / (1f + overflow / 150f))
                        } else if (newX > 0) {
                            dragAmount * (1f / (1f + newX / 100f))
                        } else {
                            dragAmount
                        }
                        
                        scope.launch {
                            offsetX.snapTo(offsetX.value + effectiveDrag)
                        }
                    },
                    onDragEnd = {
                        val target = if (offsetX.value < -revealWidth * 0.5f) -revealWidth else 0f
                        scope.launch {
                            offsetX.animateTo(target, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                        }
                    }
                )
            }
    ) {
        // Background: Action Button
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 16.dp), // Adjusted from 24dp to 16dp to move it slightly more to the right but still breathable
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                onClick = onTrashClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Foreground: The Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
fun HomeScreenPreview() {
    com.kippu.trace.ui.theme.KIPPU_TraceTheme {
        HomeScreen(events = emptyList(), onAddClick = {}, onEventClick = {}, onDeleteEvent = {})
    }
}
