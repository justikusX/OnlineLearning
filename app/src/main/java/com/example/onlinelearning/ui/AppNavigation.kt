package com.example.onlinelearning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.onlinelearning.ui.screens.AccountScreen
import com.example.onlinelearning.ui.screens.ClockingInDialog
import com.example.onlinelearning.ui.screens.CourseDetailScreen
import com.example.onlinelearning.ui.screens.CoursesScreen
import com.example.onlinelearning.ui.screens.EditAccountScreen
import com.example.onlinelearning.ui.screens.FavouritesScreen
import com.example.onlinelearning.ui.screens.HelpScreen
import com.example.onlinelearning.ui.screens.HomeScreen
import com.example.onlinelearning.ui.screens.LoginScreen
import com.example.onlinelearning.ui.screens.MessagesScreen
import com.example.onlinelearning.ui.screens.MyCoursesScreen
import com.example.onlinelearning.ui.screens.OnboardingScreen
import com.example.onlinelearning.ui.screens.PhoneAuthScreen
import com.example.onlinelearning.ui.screens.SearchScreen
import com.example.onlinelearning.ui.screens.SettingsScreen
import com.example.onlinelearning.ui.screens.SignUpScreen
import com.example.onlinelearning.ui.theme.Primary
import com.example.onlinelearning.ui.theme.isDark
import kotlinx.coroutines.launch

private object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val PHONE = "phone"
    const val HOME = "home"
    const val COURSES = "courses"
    const val SEARCH = "search"
    const val MESSAGES = "messages"
    const val ACCOUNT = "account"
    const val MY_COURSES = "my_courses"
    const val FAVOURITES = "favourites"
    const val EDIT_ACCOUNT = "edit_account"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val COURSE = "course/{id}"
    fun course(id: String) = "course/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/** Bottom bar from the design: Home, Course, Search (round button in the middle), Message, Account. */
private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.COURSES, "Course", Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.SEARCH, "Search", Icons.Filled.Search),
    Tab(Routes.MESSAGES, "Message", Icons.AutoMirrored.Filled.Chat),
    Tab(Routes.ACCOUNT, "Account", Icons.Filled.Person),
)

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val factory = AppViewModelFactory.Factory
    val session: SessionViewModel = viewModel(factory = factory)
    val courses: CoursesViewModel = viewModel(factory = factory)
    val auth: AuthViewModel = viewModel(factory = factory)
    val phoneAuth: PhoneAuthViewModel = viewModel(factory = factory)
    val theme: ThemeViewModel = viewModel(factory = factory)
    val themeMode by theme.mode.collectAsStateWithLifecycle()
    val isDark = themeMode.isDark()
    val profile by session.profile.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { scope.launch { snackbar.showSnackbar(it) } }
    LaunchedEffect(session) { session.messages.collect { snackbar.showSnackbar(it) } }

    var showClockIn by remember { mutableStateOf(false) }
    LaunchedEffect(session) { session.clockIn.collect { showClockIn = true } }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val start = remember { if (session.user.value != null) Routes.HOME else Routes.ONBOARDING }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (tabs.any { it.route == currentRoute }) {
                AppBottomBar(currentRoute) { nav.switchTab(it) }
            }
        },
    ) { padding ->
        val openCourse: (String) -> Unit = { nav.navigate(Routes.course(it)) }
        val goHome: () -> Unit = {
            nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
        }

        // Screens draw under the status bar themselves; only the bottom bar inset is applied here.
        NavHost(nav, startDestination = start, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onSignUp = { nav.navigate(Routes.SIGN_UP) },
                    onLogIn = { nav.navigate(Routes.LOGIN) },
                    onSkip = goHome,
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    auth,
                    onLoggedIn = goHome,
                    onSignUp = { nav.navigate(Routes.SIGN_UP) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                    onPhone = { nav.navigate(Routes.PHONE) },
                )
            }
            composable(Routes.SIGN_UP) {
                SignUpScreen(
                    auth,
                    onSignedUp = goHome,
                    onLogIn = { nav.navigate(Routes.LOGIN) { popUpTo(Routes.SIGN_UP) { inclusive = true } } },
                    onPhone = { nav.navigate(Routes.PHONE) },
                )
            }
            composable(Routes.PHONE) {
                PhoneAuthScreen(phoneAuth, onDone = goHome, onClose = { nav.popBackStack() })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    session,
                    courses,
                    onCourse = openCourse,
                    onSeeAll = { nav.switchTab(Routes.COURSES) },
                    onMyCourses = { nav.navigate(Routes.MY_COURSES) },
                    onClockIn = { showClockIn = true },
                    isDark = isDark,
                    onToggleTheme = { theme.toggle(isDark) },
                    onAvatarClick = { nav.switchTab(Routes.ACCOUNT) },
                )
            }
            composable(Routes.COURSES) {
                CoursesScreen(
                    courses,
                    session,
                    onCourse = openCourse,
                    onOpenSearch = { nav.switchTab(Routes.SEARCH) },
                    onAvatarClick = { nav.switchTab(Routes.ACCOUNT) },
                )
            }
            composable(Routes.SEARCH) { SearchScreen(courses, openCourse) }
            composable(Routes.MESSAGES) { MessagesScreen(session) }
            composable(Routes.ACCOUNT) {
                AccountScreen(
                    session,
                    onFavourites = { nav.navigate(Routes.FAVOURITES) },
                    onEditAccount = { nav.navigate(Routes.EDIT_ACCOUNT) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                    onHelp = { nav.navigate(Routes.HELP) },
                    onLogIn = { nav.navigate(Routes.LOGIN) },
                    onSignedOut = { nav.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } } },
                )
            }
            composable(Routes.MY_COURSES) {
                MyCoursesScreen(
                    session,
                    courses,
                    onCourse = openCourse,
                    onBrowse = { nav.switchTab(Routes.COURSES) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.FAVOURITES) {
                FavouritesScreen(session, courses, onCourse = openCourse, onBack = { nav.popBackStack() })
            }
            composable(Routes.EDIT_ACCOUNT) {
                EditAccountScreen(session, onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(themeMode = themeMode, onThemeMode = theme::set, onBack = { nav.popBackStack() })
            }
            composable(Routes.HELP) {
                HelpScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.COURSE) { entry ->
                CourseDetailScreen(
                    courseId = entry.arguments?.getString("id").orEmpty(),
                    session = session,
                    courses = courses,
                    onBack = { nav.popBackStack() },
                    showMessage = showMessage,
                )
            }
        }
    }

    if (showClockIn) {
        ClockingInDialog(profile) { showClockIn = false }
    }
}

@Composable
private fun AppBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    Surface(shadowElevation = 12.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val color = if (selected) Primary else MaterialTheme.colorScheme.outline
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(tab.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (tab.route == Routes.SEARCH) {
                        // The round Search button in the middle of the bar.
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(tab.icon, tab.label, tint = Primary)
                        }
                    } else {
                        Icon(tab.icon, tab.label, tint = color, modifier = Modifier.padding(top = 12.dp))
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(tab.label, style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
