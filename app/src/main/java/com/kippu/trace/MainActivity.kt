package com.kippu.trace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kippu.trace.model.DateEvent
import com.kippu.trace.model.DisplayMode
import com.kippu.trace.ui.theme.KIPPU_TraceTheme
import com.kippu.trace.utils.ThemeMode
import com.kippu.trace.utils.ThemePreferences
import com.kippu.trace.viewmodel.EventViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val eventViewModel: EventViewModel = viewModel()
            val events by eventViewModel.allEvents.collectAsState()
            val context = this
            var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            KIPPU_TraceTheme(darkTheme = darkTheme) {
                MainApp(
                    events = events,
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        ThemePreferences.setThemeMode(context, mode)
                    },
                    onAddEvent = { eventViewModel.addEvent(it) },
                    onDeleteEvent = { eventViewModel.deleteEvent(it) }
                )
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "日子", Icons.Default.DateRange)
    data object Detail : Screen("detail/{eventId}", "详情", Icons.Default.AutoAwesomeMotion) {
        fun createRoute(eventId: Long) = "detail/$eventId"
    }
    data object Settings : Screen("settings", "我的", Icons.Default.Settings)
    data object Editor : Screen("editor", "编辑", Icons.Default.Add)
}

@Composable
fun MainApp(
    events: List<DateEvent> = emptyList(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onAddEvent: (DateEvent) -> Unit = {},
    onDeleteEvent: (DateEvent) -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val isMainScreen = (currentDestination?.route == Screen.Home.route) || (currentDestination?.route == Screen.Settings.route)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(400)) },
                exitTransition = { fadeOut(tween(400)) },
            ) {
            composable(route = Screen.Home.route) {
                com.kippu.trace.ui.screens.HomeScreen(
                    events = events,
                    onAddClick = { navController.navigate(Screen.Editor.route) },
                    onEventClick = { event -> 
                        navController.navigate(Screen.Detail.createRoute(event.id))
                    },
                    onDeleteEvent = onDeleteEvent,
                )
            }
            composable(
                route = Screen.Editor.route,
                enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
                exitTransition = { fadeOut() + slideOutVertically(targetOffsetY = { it }) },
            ) {
                com.kippu.trace.ui.screens.EditorScreen(
                    onDismiss = { navController.popBackStack() },
                    onSave = { newEvent ->
                        onAddEvent(newEvent)
                        navController.popBackStack()
                    },
                )
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(androidx.navigation.navArgument("eventId") { type = androidx.navigation.NavType.LongType }),
                enterTransition = { fadeIn(tween(500)) },
                exitTransition = { fadeOut(tween(500)) },
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0L
                com.kippu.trace.ui.screens.DetailScreen(
                    events = events,
                    initialEventId = eventId,
                    onBack = { navController.popBackStack() },
                    onUpdateEvent = { onAddEvent(it) }
                )
            }
            composable(route = Screen.Settings.route) {
                com.kippu.trace.ui.screens.SettingsScreen(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange
                )
            }
        }

        AnimatedVisibility(
            visible = isMainScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(80.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item 1: Home
                    val isHomeSelected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true
                    CustomNavBarItem(
                        icon = { NavIconWithPulse(icon = Screen.Home.icon, isSelected = isHomeSelected) },
                        label = { 
                            Text(
                                text = Screen.Home.label,
                                color = if (isHomeSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            ) 
                        },
                        selected = isHomeSelected,
                        onClick = {
                            if (!isHomeSelected) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )

                    // Item 2: Detail
                    val isDetailSelected = (currentDestination?.route?.startsWith("detail") == true)
                    CustomNavBarItem(
                        icon = { NavIconWithPulse(icon = Screen.Detail.icon, isSelected = isDetailSelected) },
                        label = { 
                            Text(
                                Screen.Detail.label, 
                                color = if (isDetailSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium
                            ) 
                        },
                        selected = isDetailSelected,
                        onClick = {
                            if (events.isNotEmpty()) {
                                navController.navigate(Screen.Detail.createRoute(events.first().id)) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )

                    // Item 3: Settings
                    val isSettingsSelected = currentDestination?.hierarchy?.any { it.route == Screen.Settings.route } == true
                    CustomNavBarItem(
                        icon = { NavIconWithPulse(icon = Screen.Settings.icon, isSelected = isSettingsSelected) },
                        label = { 
                            Text(
                                text = Screen.Settings.label,
                                color = if (isSettingsSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            ) 
                        },
                        selected = isSettingsSelected,
                        onClick = {
                            if (!isSettingsSelected) {
                                navController.navigate(Screen.Settings.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
fun RowScope.CustomNavBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .weight(1f)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(modifier = Modifier.height(4.dp))
        label()
    }
}

@Composable
fun NavIconWithPulse(icon: ImageVector, isSelected: Boolean) {
    val pulseScale = remember { Animatable(1f) }
    val pulseAlpha = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            pulseScale.snapTo(1f)
            pulseAlpha.snapTo(0.5f)
            
            launch {
                pulseScale.animateTo(2f, tween(400, easing = LinearOutSlowInEasing))
            }
            launch {
                pulseAlpha.animateTo(0f, tween(400))
            }
        }
    }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(pulseScale.value)
                .alpha(pulseAlpha.value)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
        )
        Icon(
            imageVector = icon, 
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    KIPPU_TraceTheme {
        MainApp(
            events = listOf(
                DateEvent(
                    id = 1, 
                    title = "测试事件", 
                    targetDate = System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L,
                    isFuture = true,
                    mode = DisplayMode.COUNT_DOWN,
                )
            ),
        )
    }
}
