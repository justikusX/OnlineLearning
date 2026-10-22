package com.example.onlinelearning.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.example.onlinelearning.data.ThemeMode
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.onlinelearning.data.model.Course
import com.example.onlinelearning.data.model.UserProfile
import com.example.onlinelearning.ui.CoursesViewModel
import com.example.onlinelearning.ui.SessionViewModel
import com.example.onlinelearning.ui.components.CourseImage
import com.example.onlinelearning.ui.components.CourseRow
import com.example.onlinelearning.ui.components.EmptyState
import com.example.onlinelearning.ui.components.LoadingBox
import com.example.onlinelearning.ui.components.PrimaryButton
import com.example.onlinelearning.ui.theme.Ink
import com.example.onlinelearning.ui.theme.Orange
import com.example.onlinelearning.ui.theme.PinkLight
import com.example.onlinelearning.ui.theme.Primary
import com.example.onlinelearning.ui.theme.PrimaryLight

private const val DAILY_GOAL_MINUTES = 60

@Composable
fun HomeScreen(
    session: SessionViewModel,
    courses: CoursesViewModel,
    onCourse: (String) -> Unit,
    onSeeAll: () -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    val user by session.user.collectAsStateWithLifecycle()
    val profile by session.profile.collectAsStateWithLifecycle()
    val state by courses.state.collectAsStateWithLifecycle()
    val name = profile?.name?.takeIf { it.isNotBlank() } ?: user?.displayName ?: "Guest"

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Primary)
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 8.dp, top = 24.dp, bottom = 72.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Hi, $name", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Text("Let's start learning", color = Color.White.copy(alpha = 0.8f))
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (isDark) "Светлая тема" else "Тёмная тема",
                            tint = Color.White,
                        )
                    }
                }
                LearnedTodayCard(
                    minutes = profile?.learnedTodayMinutes ?: 0,
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 40.dp).padding(horizontal = 20.dp),
                )
            }
            Spacer(Modifier.height(56.dp))
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { PromoCard("What do you want to learn today?", "Get started", PinkLight, onSeeAll) }
                item { PromoCard("Meetup: off-line exchange of learning experiences", "Join", PrimaryLight, onSeeAll) }
            }
        }
        val purchased = state.courses.filter { profile?.purchased?.contains(it.id) == true }
        if (purchased.isNotEmpty()) {
            item { SectionTitle("Learning Plan") }
            items(purchased, key = { "plan-" + it.id }) { course ->
                LearningPlanRow(course, profile, onClick = { onCourse(course.id) })
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Popular courses", Modifier.weight(1f))
                TextButton(onClick = onSeeAll) { Text("See all") }
            }
        }
        when {
            state.loading -> item { LoadingBox(Modifier.height(160.dp)) }
            else -> items(state.courses.take(4), key = { it.id }) { course ->
                CourseRow(course, { onCourse(course.id) }, Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun LearnedTodayCard(minutes: Int, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Learned today", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${minutes}min", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(" / ${DAILY_GOAL_MINUTES}min", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (minutes.toFloat() / DAILY_GOAL_MINUTES).coerceIn(0f, 1f) },
                color = Orange,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun PromoCard(title: String, button: String, color: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(16.dp),
    ) {
        // Promo cards keep light backgrounds in both themes, so the text stays dark.
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(16.dp))
        Text(
            button,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Orange)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun LearningPlanRow(course: Course, profile: UserProfile?, onClick: () -> Unit) {
    val done = profile?.progress?.get(course.id)?.size ?: 0
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(course.title, Modifier.weight(1f))
        Text("$done/${course.lessons.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CoursesScreen(viewModel: CoursesViewModel, onCourse: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val filtered by viewModel.filtered.collectAsStateWithLifecycle()
    val categories = state.courses.map { it.category }.distinct()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Course", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(20.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.query.value = it },
            placeholder = { Text("Find Course") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { FilterChip(category == null, { viewModel.category.value = null }, { Text("All") }) }
            items(categories) { c ->
                FilterChip(category == c, { viewModel.category.value = c }, { Text(c) })
            }
        }
        if (state.offline) {
            Text(
                "API недоступен — показан встроенный каталог",
                style = MaterialTheme.typography.bodySmall,
                color = Orange,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        when {
            state.loading -> LoadingBox()
            state.error != null -> EmptyState(Icons.Filled.CloudOff, "No Network!", state.error.orEmpty()) {
                PrimaryButton("Try again", { viewModel.refresh(force = true) })
            }
            filtered.isEmpty() -> EmptyState(Icons.Filled.SearchOff, "Course not found", "Try searching the course with a different keyword")
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.id }) { CourseRow(it, { onCourse(it.id) }) }
            }
        }
    }
}

@Composable
fun MyCoursesScreen(session: SessionViewModel, courses: CoursesViewModel, onCourse: (String) -> Unit, onBrowse: () -> Unit) {
    val profile by session.profile.collectAsStateWithLifecycle()
    val state by courses.state.collectAsStateWithLifecycle()
    val purchased = state.courses.filter { profile?.purchased?.contains(it.id) == true }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("My courses", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(20.dp))
        if (purchased.isEmpty()) {
            EmptyState(Icons.AutoMirrored.Filled.MenuBook, "No Courses", "You don't have any courses yet") {
                PrimaryButton("Browse courses", onBrowse)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(purchased, key = { it.id }) { course ->
                    val done = profile?.progress?.get(course.id)?.size ?: 0
                    val total = course.lessons.size.coerceAtLeast(1)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().clickable { onCourse(course.id) },
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(course.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            Text("Completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LinearProgressIndicator(
                                progress = { done.toFloat() / total },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                            )
                            Text("$done/$total lessons", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountScreen(
    session: SessionViewModel,
    courses: CoursesViewModel,
    onCourse: (String) -> Unit,
    onLogIn: () -> Unit,
    onSignedOut: () -> Unit,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
) {
    val user by session.user.collectAsStateWithLifecycle()
    val profile by session.profile.collectAsStateWithLifecycle()
    val state by courses.state.collectAsStateWithLifecycle()
    val favorites = state.courses.filter { profile?.favorites?.contains(it.id) == true }
    val name = profile?.name?.takeIf { it.isNotBlank() } ?: user?.displayName ?: "Guest"

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Account", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(24.dp))
                Box(
                    Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = Primary)
                }
                Spacer(Modifier.height(12.dp))
                Text(name, style = MaterialTheme.typography.titleLarge)
                user?.email?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item {
            Text("Theme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            ThemeSelector(themeMode, onThemeMode, Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp))
        }
        item {
            Text("Favourite", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        }
        if (favorites.isEmpty()) {
            item {
                ListItem(
                    headlineContent = { Text("Нет избранных курсов") },
                    leadingContent = { Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.outline) },
                )
            }
        } else {
            items(favorites, key = { it.id }) { course ->
                ListItem(
                    headlineContent = { Text(course.title) },
                    supportingContent = { Text(course.author) },
                    leadingContent = { CourseImage(course.imageUrl, Modifier.size(48.dp)) },
                    modifier = Modifier.clickable { onCourse(course.id) },
                )
            }
        }
        item {
            if (user == null) {
                ListItem(
                    headlineContent = { Text("Log in") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Login, null) },
                    modifier = Modifier.clickable(onClick = onLogIn),
                )
            } else {
                ListItem(
                    headlineContent = { Text("Log out", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        session.signOut()
                        onSignedOut()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(mode: ThemeMode, onMode: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, "System", Icons.Filled.BrightnessAuto),
        Triple(ThemeMode.LIGHT, "Light", Icons.Filled.LightMode),
        Triple(ThemeMode.DARK, "Dark", Icons.Filled.DarkMode),
    )
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (option, label, icon) ->
            SegmentedButton(
                selected = mode == option,
                onClick = { onMode(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
            ) {
                Text(label)
            }
        }
    }
}
