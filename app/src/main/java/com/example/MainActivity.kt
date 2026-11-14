package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ThemeMode
import com.example.ui.AtomaticViewModel
import com.example.ui.components.AdMobBanner
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReplyConfigScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.AtomaticTheme
import kotlinx.coroutines.launch

object AtomaticRoutes {
    const val HOME = "home"
    const val TEMPLATES = "templates"
    const val SETTINGS = "settings"
    const val REPLY_CONFIG = "reply_config"
}

private data class TopLevelTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelTabs = listOf(
    TopLevelTab(
        route = AtomaticRoutes.HOME,
        labelRes = R.string.tab_home,
        selectedIcon = Icons.Filled.Bolt,
        unselectedIcon = Icons.Outlined.Bolt,
        testTag = "nav_tab_home"
    ),
    TopLevelTab(
        route = AtomaticRoutes.TEMPLATES,
        labelRes = R.string.tab_templates,
        selectedIcon = Icons.AutoMirrored.Filled.LibraryBooks,
        unselectedIcon = Icons.AutoMirrored.Outlined.LibraryBooks,
        testTag = "nav_tab_templates"
    ),
    TopLevelTab(
        route = AtomaticRoutes.SETTINGS,
        labelRes = R.string.tab_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: AtomaticViewModel = viewModel(
                factory = AtomaticViewModel.factory(context.applicationContext as Application)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val useDarkTheme = when (uiState.settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            AtomaticTheme(darkTheme = useDarkTheme) {
                AtomaticRootApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AtomaticRootApp(viewModel: AtomaticViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiTestState by viewModel.aiTestState.collectAsStateWithLifecycle()
    val simulationState by viewModel.simulationState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshServiceStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.refreshServiceStatus()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevel = currentDestination?.route in setOf(
        AtomaticRoutes.HOME,
        AtomaticRoutes.TEMPLATES,
        AtomaticRoutes.SETTINGS
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (isTopLevel) {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Bolt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "  Atomatic",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { navController.navigate(AtomaticRoutes.REPLY_CONFIG) },
                                modifier = Modifier.testTag("top_bar_reply_config_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Configure auto-reply and AI"
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (isTopLevel && !useNavRail) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AdMobBanner()
                        NavigationBar {
                            topLevelTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevel && useNavRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        topLevelTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = AtomaticRoutes.HOME,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(AtomaticRoutes.HOME) {
                            HomeScreen(
                                uiState = uiState,
                                simulationState = simulationState,
                                onToggleMaster = viewModel::setMasterEnabled,
                                onOpenReplyConfig = { navController.navigate(AtomaticRoutes.REPLY_CONFIG) },
                                onSetContactFilterMode = viewModel::setContactFilterMode,
                                onAddContactRule = viewModel::addContactRule,
                                onRemoveContactRule = viewModel::removeContactRule,
                                onToggleDirectMessagesOnly = viewModel::setDirectMessagesOnly,
                                onToggleApp = viewModel::setAppEnabled,
                                onSetCooldownSeconds = viewModel::setReplyCooldownSeconds,
                                onRunSimulation = viewModel::runPipelineSimulation,
                                onResetSimulation = viewModel::resetSimulationState
                            )
                        }

                        composable(AtomaticRoutes.TEMPLATES) {
                            TemplatesScreen(
                                templates = uiState.templates,
                                onApplyTemplate = { template ->
                                    viewModel.applyTemplate(template) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Applied template: ${template.title}")
                                        }
                                        navigateToTab(AtomaticRoutes.HOME)
                                    }
                                },
                                onToggleFavorite = viewModel::toggleTemplateFavorite,
                                onCreateTemplate = { title, category, body, prompt, isAi ->
                                    viewModel.createUserTemplate(title, category, body, prompt, isAi)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Saved template \"$title\"")
                                    }
                                },
                                onDeleteTemplate = viewModel::deleteUserTemplate
                            )
                        }

                        composable(AtomaticRoutes.SETTINGS) {
                            SettingsScreen(
                                uiState = uiState,
                                simulationState = simulationState,
                                onToggleFallback = viewModel::setFallbackToCustomOnAiError,
                                onToggleInstantZeroMbFallback = viewModel::setInstantZeroMbFallback,
                                onSetThemeMode = viewModel::setThemeMode,
                                onClearCooldowns = {
                                    viewModel.clearCooldowns()
                                    scope.launch { snackbarHostState.showSnackbar("Cooldown cache cleared") }
                                },
                                onClearLogs = {
                                    viewModel.clearLogs()
                                    scope.launch { snackbarHostState.showSnackbar("Reply history cleared") }
                                },
                                onOpenReplyConfig = { navController.navigate(AtomaticRoutes.REPLY_CONFIG) },
                                onRunSimulation = viewModel::runPipelineSimulation,
                                onResetSimulation = viewModel::resetSimulationState
                            )
                        }

                        composable(AtomaticRoutes.REPLY_CONFIG) {
                            ReplyConfigScreen(
                                currentSettings = uiState.settings,
                                aiTestState = aiTestState,
                                onTestAi = viewModel::testAiConfiguration,
                                onResetAiTest = viewModel::resetAiTestState,
                                onSave = { customReply, aiEnabled, provider, apiKey, model, endpoint, prompt ->
                                    viewModel.saveReplyAndAiConfig(
                                        customReplyMessage = customReply,
                                        isAiEnabled = aiEnabled,
                                        aiProvider = provider,
                                        aiApiKey = apiKey,
                                        aiModel = model,
                                        aiEndpointUrl = endpoint,
                                        aiSystemPrompt = prompt
                                    ) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Auto-reply settings saved locally")
                                        }
                                        navController.popBackStack()
                                    }
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                    if (isTopLevel && useNavRail) {
                        AdMobBanner()
                    }
                }
            }
        }
    }
}
