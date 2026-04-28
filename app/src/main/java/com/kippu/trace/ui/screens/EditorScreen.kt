package com.kippu.trace.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kippu.trace.model.DateEvent
import com.kippu.trace.model.DisplayMode
import com.kippu.trace.ui.components.PinnedEventCard
import com.kippu.trace.ui.theme.KIPPU_TraceTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onDismiss: () -> Unit,
    onSave: (DateEvent) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var backgroundUri by remember { mutableStateOf<String?>(null) }
    var isPinned by remember { mutableStateOf(false) }
    var maskOpacity by remember { mutableFloatStateOf(0.4f) }
    var showDatePicker by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(DisplayMode.COUNT_DOWN) }

    val scrollState = rememberScrollState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { backgroundUri = it.toString() }
    }

    val targetLocalDate = Instant.ofEpochMilli(selectedDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(today, targetLocalDate).let { if (it < 0) -it else it }
    
    val isFuture = selectedDate > System.currentTimeMillis()
    LaunchedEffect(selectedDate) {
        mode = if (isFuture) DisplayMode.COUNT_DOWN else DisplayMode.ACCUMULATE
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: selectedDate
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "编辑时痕", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(painter = rememberVectorPainter(Icons.Default.Close), contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onSave(DateEvent(
                            title = title.ifEmpty { "未命名" },
                            targetDate = selectedDate,
                            isFuture = isFuture,
                            mode = mode,
                            isPinned = isPinned,
                            backgroundUri = backgroundUri,
                            maskOpacity = maskOpacity
                        ))
                    }) {
                        Icon(painter = rememberVectorPainter(Icons.Default.Check), contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                placeholder = { Text("给这一刻起个名字") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(if (mode == DisplayMode.COUNT_DOWN) "目标日期" else "起始日期", style = MaterialTheme.typography.labelMedium)
                            Text(targetLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Card(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("背景图片", style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (backgroundUri == null) "点击选择" else "已选择", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = mode == DisplayMode.COUNT_DOWN,
                        onClick = { mode = DisplayMode.COUNT_DOWN },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Default.HourglassEmpty, null, Modifier.size(16.dp)) }
                    ) { Text("倒数模式") }
                    SegmentedButton(
                        selected = mode == DisplayMode.ACCUMULATE,
                        onClick = { mode = DisplayMode.ACCUMULATE },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Default.History, null, Modifier.size(16.dp)) }
                    ) { Text("累计模式") }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("在首页置顶展示", style = MaterialTheme.typography.titleSmall)
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("遮罩强度", style = MaterialTheme.typography.titleSmall)
                        Text("${(maskOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary))
                    }
                    Slider(
                        value = maskOpacity,
                        onValueChange = { maskOpacity = it },
                        valueRange = 0.1f..0.9f,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("置顶效果", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    PinnedEventCard(
                        event = DateEvent(
                            title = title.ifEmpty { "示例标题" },
                            targetDate = selectedDate,
                            isFuture = isFuture,
                            mode = mode,
                            isPinned = true,
                            backgroundUri = backgroundUri,
                            maskOpacity = maskOpacity
                        ),
                        onClick = {}
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("全屏展示", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black)
                    ) {
                        FullScreenPreviewContent(
                            title = title.ifEmpty { "示例标题" },
                            days = days.toString(),
                            imageUri = backgroundUri,
                            opacity = maskOpacity,
                            isFuture = mode == DisplayMode.COUNT_DOWN,
                            date = targetLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FullScreenPreviewContent(title: String, days: String, imageUri: String?, opacity: Float, isFuture: Boolean, date: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = opacity)))
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val prefix = if (isFuture) "还有" else "已经"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
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
            
            Text(
                text = days,
                color = Color.White,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = com.kippu.trace.ui.theme.NumberFontFamily
                )
            )
            
            val datePrefix = if (isFuture) "距离" else "自从"
            Text(
                text = "$datePrefix $date",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    letterSpacing = 2.sp
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditorScreenPreview() {
    KIPPU_TraceTheme {
        EditorScreen(onDismiss = {}, onSave = {})
    }
}
