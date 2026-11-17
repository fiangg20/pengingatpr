package com.bases.pengingatpr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Beranda", Icons.Rounded.Home),
    Tasks("PR", Icons.Rounded.Assignment),
    Schedule("Jadwal", Icons.Rounded.Schedule),
    Calendar("Kalender", Icons.Rounded.CalendarMonth),
    More("Lainnya", Icons.Rounded.Apps),
}

class Nav(
    val stack: SnapshotStateList<Route>,
    val host: SnackbarHostState,
    val scope: CoroutineScope,
    val selectTab: (Tab) -> Unit,
) {
    fun push(r: Route) {
        stack.add(r)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun toast(msg: String, action: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val r = host.showSnackbar(msg, action, duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(this)
        Notifier.createChannels(this)
        Notifier.rescheduleAll(this)
        handle(intent)
        enableEdgeToEdge()
        setContent {
            ExpressiveTheme { Root() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent?) {
        i?.getStringExtra(Notifier.EXTRA_ID)?.let { Repo.pendingOpen = it }
    }
}

private class Holder {
    var last: Route? = null
}

@Composable
fun Root() {
    val ctx = LocalContext.current
    val act = ctx as ComponentActivity
    val dark = isDark()
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    val stack = remember { mutableStateListOf<Route>() }
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val nav = remember { Nav(stack, host, scope) { tab = it } }
    val holder = remember { Holder() }

    LaunchedEffect(dark) {
        act.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
        )
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val pending = Repo.pendingOpen
    LaunchedEffect(pending) {
        if (pending != null) {
            if (Repo.get(pending) != null) {
                stack.clear()
                stack.add(Route.Detail(pending))
            }
            Repo.pendingOpen = null
        }
    }

    BackHandler(enabled = stack.isNotEmpty()) { nav.pop() }

    val top = stack.lastOrNull()
    if (top != null) holder.last = top

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TabsHost(nav, tab) { tab = it }

        AnimatedVisibility(
            visible = top != null,
            enter = fadeIn() +
                scaleIn(initialScale = 0.92f, animationSpec = spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) +
                slideInVertically(spring<IntOffset>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it / 8 },
            exit = fadeOut() + scaleOut(targetScale = 0.95f),
        ) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (val r = holder.last) {
                    is Route.Editor -> EditorScreen(nav, r)
                    is Route.Detail -> DetailScreen(nav, r.id)
                    is Route.Focus -> FocusScreen(nav, r.prId)
                    Route.Import -> ImportScreen(nav)
                    Route.Stats -> StatsScreen(nav)
                    Route.Settings -> SettingsScreen(nav)
                    null -> {}
                }
            }
        }

        SnackbarHost(
            hostState = host,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 88.dp),
        )
    }
}

@Composable
private fun TabsHost(nav: Nav, tab: Tab, onTab: (Tab) -> Unit) {
    var fabOpen by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { fabOpen = false; onTab(t) },
                        icon = { Icon(t.icon, t.label) },
                        label = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab != Tab.More) {
                FabMenu(
                    open = fabOpen,
                    onToggle = { fabOpen = !fabOpen },
                    onNew = { fabOpen = false; nav.push(Route.Editor(null)) },
                    onImport = { fabOpen = false; nav.push(Route.Import) },
                    onFocus = { fabOpen = false; nav.push(Route.Focus(null)) },
                )
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (tab) {
                Tab.Home -> HomeScreen(nav)
                Tab.Tasks -> TasksScreen(nav)
                Tab.Schedule -> ScheduleScreen(nav)
                Tab.Calendar -> CalendarScreen(nav)
                Tab.More -> MoreScreen(nav)
            }
            if (fabOpen) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { fabOpen = false },
                )
            }
        }
    }
}

@Composable
private fun FabMenu(open: Boolean, onToggle: () -> Unit, onNew: () -> Unit, onImport: () -> Unit, onFocus: () -> Unit) {
    val rot by animateFloatAsState(
        if (open) 135f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "fabrot",
    )
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedVisibility(
            visible = open,
            enter = fadeIn() + scaleIn(initialScale = 0.6f, animationSpec = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = onImport,
                    icon = { Icon(Icons.Rounded.PictureAsPdf, null) },
                    text = { Text("Impor jadwal PDF") },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = RoundedCornerShape(22.dp),
                )
                ExtendedFloatingActionButton(
                    onClick = onFocus,
                    icon = { Icon(Icons.Rounded.Timer, null) },
                    text = { Text("Mulai fokus") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(22.dp),
                )
                ExtendedFloatingActionButton(
                    onClick = onNew,
                    icon = { Icon(Icons.Rounded.Assignment, null) },
                    text = { Text("PR baru") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(22.dp),
                )
            }
        }
        FloatingActionButton(
            onClick = onToggle,
            modifier = Modifier.size(68.dp),
            shape = RoundedCornerShape(if (open) 34.dp else 22.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Rounded.Add, "Menu", Modifier.size(30.dp).rotate(rot))
        }
    }
}
