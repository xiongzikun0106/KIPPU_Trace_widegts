package com.kippu.trace.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.kippu.trace.model.DateEvent
import com.kippu.trace.model.DisplayMode
import com.kippu.trace.ui.components.PinnedEventCard
import com.kippu.trace.ui.theme.KIPPU_TraceTheme
import com.kippu.trace.utils.FileUtils
import com.kippu.trace.utils.TextUtils
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
    val context = LocalContext.current
    
    // 使用新版 TextFieldState，这是解决光标拖拽顺滑滚动的关键
    val titleState = rememberTextFieldState("")
    
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var backgroundUri by remember { mutableStateOf<String?>(null) }
    var isPinned by remember { mutableStateOf(false) }
    var maskOpacity by remember { mutableFloatStateOf(0.4f) }
    val showDatePicker = remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(DisplayMode.COUNT_DOWN) }

    val scrollState = rememberScrollState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localPath = FileUtils.saveImageToInternalStorage(context, it)
            if (localPath != null) {
                backgroundUri = localPath
            }
        }
    }

    val targetLocalDate = remember(selectedDate) {
        Instant.ofEpochMilli(selectedDate).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    
    val days = remember(targetLocalDate) {
        val today = LocalDate.now()
        val d = ChronoUnit.DAYS.between(today, targetLocalDate)
        if (d < 0) -d else d
    }

    val formattedDate = remember(targetLocalDate) {
        targetLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
    
    LaunchedEffect(selectedDate) {
        mode = if (selectedDate > System.currentTimeMillis()) DisplayMode.COUNT_DOWN else DisplayMode.ACCUMULATE
    }

    if (showDatePicker.value) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

        Dialog(
            onDismissRequest = { showDatePicker.value = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp, start = 8.dp, end = 8.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DatePicker(
                        state = datePickerState,
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        colors = DatePickerDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            dividerColor = Color.Transparent
                        ),
                        modifier = Modifier.scale(1.0f)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDatePicker.value = false }) {
                            Text("取消")
                        }
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { selectedDate = it }
                            showDatePicker.value = false
                        }) {
                            Text("确定")
                        }
                    }
                }
            }
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
                            title = titleState.text.toString().ifEmpty { "无题" },
                            targetDate = selectedDate,
                            isFuture = mode == DisplayMode.COUNT_DOWN,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 自定义输入框实现方案：锁定宽度，允许内部滚动
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (titleState.text.isEmpty()) {
                        Text(
                            text = "给这一刻起个名字",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    // 使用新版 BasicTextField (TextField2 API)
                    BasicTextField(
                        state = titleState,
                        lineLimits = TextFieldLineLimits.SingleLine,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 辅助文本部分
                val currentTitle = titleState.text.toString()
                val visualWidth = TextUtils.getVisualWidth(currentTitle)
                val isPureEnglish = currentTitle.all { char -> char.code in 0..127 }
                val typeStr = if (isPureEnglish) "英文/数字" else "中文字符"
                Text(
                    text = "当前视觉宽度: ${visualWidth.toInt()} ($typeStr)",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        onClick = { showDatePicker.value = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(if (mode == DisplayMode.COUNT_DOWN) "目标日期" else "起始日期", style = MaterialTheme.typography.labelMedium)
                            Text(formattedDate, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Card(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
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

                ModeSwitcher(
                    selectedMode = mode,
                    onModeSelected = { mode = it }
                )
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
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("置顶效果", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            PinnedEventCard(
                                event = DateEvent(
                                    title = titleState.text.toString().ifEmpty { "示例标题" },
                                    targetDate = selectedDate,
                                    isFuture = mode == DisplayMode.COUNT_DOWN,
                                    mode = mode,
                                    isPinned = true,
                                    backgroundUri = backgroundUri,
                                    maskOpacity = maskOpacity
                                ),
                                onClick = {}
                            )
                        }
                    }
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
                            title = titleState.text.toString().ifEmpty { "示例标题" },
                            days = days.toString(),
                            imageUri = backgroundUri,
                            opacity = maskOpacity,
                            isFuture = mode == DisplayMode.COUNT_DOWN,
                            date = formattedDate
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ModeSwitcher(
    selectedMode: DisplayMode,
    onModeSelected: (DisplayMode) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                RoundedCornerShape(26.dp)
            )
            .padding(4.dp)
    ) {
        val scope = this
        val indicatorWidth = scope.maxWidth / 2
        val indicatorOffset by animateDpAsState(
            targetValue = if (selectedMode == DisplayMode.COUNT_DOWN) 0.dp else indicatorWidth,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "modeIndicator"
        )

        // Sliding Indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(indicatorWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            ModeOption(
                title = "倒数模式",
                icon = Icons.Default.HourglassEmpty,
                selected = selectedMode == DisplayMode.COUNT_DOWN,
                onClick = { onModeSelected(DisplayMode.COUNT_DOWN) },
                modifier = Modifier.weight(1f)
            )
            ModeOption(
                title = "累计模式",
                icon = Icons.Default.History,
                selected = selectedMode == DisplayMode.ACCUMULATE,
                onClick = { onModeSelected(DisplayMode.ACCUMULATE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ModeOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
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
            
            // 构造每9字换行且前缀紧跟末尾的标题（同步 DetailScreen 逻辑）
            val annotatedTitle = remember(title, prefix) {
                val displayTitle = if (title.length > 35) {
                    title.take(32) + "..."
                } else {
                    title
                }
                
                androidx.compose.ui.text.buildAnnotatedString {
                    val chunks = displayTitle.chunked(9)
                    chunks.forEachIndexed { index, chunk ->
                        append(chunk)
                        if (index < chunks.size - 1) append("\n")
                    }
                    append(" ")
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color.White.copy(alpha = 0.7f)))
                    append(prefix)
                    pop()
                }
            }

            Text(
                text = annotatedTitle,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 4.sp
                ),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 天数字号自适应逻辑
            val fontSize = when {
                days.length >= 8 -> 60.sp
                days.length >= 7 -> 72.sp
                days.length >= 6 -> 88.sp
                days.length >= 5 -> 100.sp
                else -> 120.sp
            }

            Text(
                text = days,
                color = Color.White,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
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
