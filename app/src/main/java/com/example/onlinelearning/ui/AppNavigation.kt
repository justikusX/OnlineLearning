package com.example.onlinelearning.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.onlinelearning.ui.theme.isDark
import com.example.onlinelearning.ui.screens.AccountScreen
import com.example.onlinelearning.ui.screens.CourseDetailScreen
import com.example.onlinelearning.ui.screens.CoursesScreen
import com.example.onlinelearning.ui.screens.HomeScreen
import com.example.onlinelearning.ui.screens.LoginScreen
import com.example.onlinelearning.ui.screens.MyCoursesScreen
import com.example.onlinelearning.ui.screens.OnboardingScreen
import com.example.onlinelearning.ui.screens.SignUpScreen
import kotlinx.coroutines.launch

private object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val HOME = "home"
    const val COURSES = "courses"
    const val MY_COURSES = "my_courses"
    const val ACCOUNT = "account"
    const val COURSE = "course/{id}"
    fun course(id: String) = "course/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.COURSES, "Course", Icons.Filled.Search),
    Tab(Routes.MY_COURSES, "My courses", Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.ACCOUNT, "Account", Icons.Filled.Person),
)

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val factory = AppViewModelFactory.Factory
    val session: SessionViewModel = viewModel(factory = factory)
    val courses: CoursesViewModel = viewModel(factory = factory)
    val auth: AuthViewModel = viewModel(factory = factory)
    val theme: ThemeViewModel = viewModel(factory = factory)
    val themeMode by theme.mode.collectAsStateWithLifecycle()
    val isDark = themeMode.isDark()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { scope.launch { snackbar.showSnackbar(it) } }
    LaunchedEffect(session) { session.messages.collect { snackbar.showSnackbar(it) } }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val start = remember { if (session.user.value != null) Routes.HOME else Routes.ONBOARDING }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (tabs.any { it.route == currentRoute }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
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
                LoginScreen(auth, onLoggedIn = goHome, onSignUp = { nav.navigate(Routes.SIGN_UP) { popUpTo(Routes.LOGIN) { inclusive = true } } })
            }
            composable(Routes.SIGN_UP) {
                SignUpScreen(auth, onSignedUp = goHome, onLogIn = { nav.navigate(Routes.LOGIN) { popUpTo(Routes.SIGN_UP) { inclusive = true } } })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    session,
                    courses,
                    onCourse = openCourse,
                    onSeeAll = { nav.switchTab(Routes.COURSES) },
                    isDark = isDark,
                    onToggleTheme = { theme.toggle(isDark) },
                    onAvatarClick = { nav.switchTab(Routes.ACCOUNT) },
                )
            }
            composable(Routes.COURSES) { CoursesScreen(courses, openCourse) }
            composable(Routes.MY_COURSES) {
                MyCoursesScreen(session, courses, openCourse, onBrowse = { nav.switchTab(Routes.COURSES) })
            }
            composable(Routes.ACCOUNT) {
                AccountScreen(
                    session,
                    courses,
                    openCourse,
                    onLogIn = { nav.navigate(Routes.LOGIN) },
                    onSignedOut = { nav.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } } },
                    themeMode = themeMode,
                    onThemeMode = theme::set,
                )
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
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
