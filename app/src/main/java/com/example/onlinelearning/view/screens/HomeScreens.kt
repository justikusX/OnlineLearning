package com.example.onlinelearning.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.onlinelearning.model.entity.Course
import com.example.onlinelearning.view.components.AssetImage
import com.example.onlinelearning.view.components.CourseImage
import com.example.onlinelearning.view.components.CourseRow
import com.example.onlinelearning.view.components.EmptyState
import com.example.onlinelearning.view.components.LearnedTodayCard
import com.example.onlinelearning.view.components.LoadingBox
import com.example.onlinelearning.view.components.PrimaryButton
import com.example.onlinelearning.view.components.UserAvatar
import com.example.onlinelearning.view.theme.Ink
import com.example.onlinelearning.view.theme.Orange
import com.example.onlinelearning.view.theme.Primary
import com.example.onlinelearning.viewmodel.CourseFilter
import com.example.onlinelearning.viewmodel.CourseProgress
import com.example.onlinelearning.viewmodel.CourseTab
import com.example.onlinelearning.viewmodel.CoursesViewModel
import com.example.onlinelearning.viewmodel.HomeViewModel
import com.example.onlinelearning.viewmodel.SessionViewModel

/** Home tab: greeting, "Learned today", promo cards, learning plan, Meetup and popular courses. */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCourse: (String) -> Unit,
    onSeeAll: () -> Unit,
    onMyCourses: () -> Unit,
    onClockIn: () -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column {
            Box {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Primary)
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 8.dp, top = 24.dp, bottom = 72.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Hi, ${state.name}", style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Let's start learning", color = Color.White.copy(alpha = 0.8f))
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (isDark) "Светлая тема" else "Тёмная тема",
                            tint = Color.White,
                        )
                    }
                    UserAvatar(
                        state.avatar,
                        state.name,
                        40.dp,
                        Modifier
                            .padding(start = 4.dp, end = 12.dp, top = 4.dp)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable(onClick = onAvatarClick),
                    )
                }
                LearnedTodayCard(
                    minutes = state.todayMinutes,
                    goalMinutes = state.goalMinutes,
                    onMyCourses = onMyCourses,
                    onClick = onClockIn,
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = 40.dp).padding(horizontal = 20.dp),
                )
            }
            Spacer(Modifier.height(56.dp))
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    PromoCard(
                        title = "What do you want to learn today?",
                        button = "Get Started",
                        background = Color(0xFFCEECFE),
                        image = "illustrations/promo_learn.webp",
                        onClick = onSeeAll,
                    )
                }
                item {
                    PromoCard(
                        title = "Create your own study plan",
                        button = "My courses",
                        background = Color(0xFFDDF4EE),
                        image = "illustrations/onboarding_3.webp",
                        onClick = onMyCourses,
                    )
                }
            }
        }
        item { SectionTitle("Learning Plan") }
        item {
            LearningPlanCard(state.myCourses, onCourse, onSeeAll, Modifier.padding(horizontal = 20.dp))
        }
        item {
            MeetupBanner(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp))
        }
        item {
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Popular courses", Modifier.weight(1f))
                TextButton(onClick = onSeeAll) { Text("See all") }
            }
        }
        when {
            state.loading -> item { LoadingBox(Modifier.height(160.dp)) }
            else -> items(state.popular, key = { it.id }) { course ->
                CourseRow(course, { onCourse(course.id) }, Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun PromoCard(title: String, button: String, background: Color, image: String, onClick: () -> Unit) {
    Box(
        Modifier
            .width(280.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick),
    ) {
        AssetImage(image, Modifier.align(Alignment.BottomEnd).size(width = 130.dp, height = 120.dp))
        Column(Modifier.padding(16.dp).width(160.dp)) {
            // Promo cards keep light backgrounds in both themes, so the text stays dark.
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.weight(1f))
            Text(
                button,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Orange)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun LearningPlanCard(
    plan: List<CourseProgress>,
    onCourse: (String) -> Unit,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            if (plan.isEmpty()) {
                Text(
                    "Start any free course or buy one — it will appear here with your progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                TextButton(onClick = onBrowse, modifier = Modifier.padding(start = 4.dp)) { Text("Browse courses") }
            }
            plan.forEach { (course, done) ->
                val total = course.lessons.size.coerceAtLeast(1)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onCourse(course.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        progress = { done.toFloat() / total },
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        strokeWidth = 3.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(course.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$done", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("/$total", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MeetupBanner(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEFE0FF)),
    ) {
        AssetImage("illustrations/meetup.webp", Modifier.align(Alignment.CenterEnd).size(width = 150.dp, height = 110.dp))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp).width(170.dp)) {
            Text("Meetup", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF440687))
            Text(
                "Off-line exchange of learning experiences",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF440687),
            )
        }
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

/** Course tab: avatar, search entry, category cards, All / Popular / New and the list. */
@Composable
fun CoursesScreen(
    viewModel: CoursesViewModel,
    session: SessionViewModel,
    onCourse: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val covers by viewModel.categoryCovers.collectAsStateWithLifecycle()
    val maxPrice by viewModel.maxPrice.collectAsStateWithLifecycle()
    val name by session.name.collectAsStateWithLifecycle()
    val profile by session.profile.collectAsStateWithLifecycle()
    // Whether the filter sheet is open is pure UI state, so it stays in the View.
    var showFilter by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Course", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            UserAvatar(profile?.avatar.orEmpty(), name, 40.dp, Modifier.clickable(onClick = onAvatarClick))
        }
        SearchEntry(onClick = onOpenSearch, filterActive = filter.isActive, onFilter = { showFilter = true })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(categories) { category ->
                        CategoryCard(
                            category = category,
                            image = covers[category].orEmpty(),
                            selected = category in filter.categories,
                            onClick = { viewModel.toggleCategory(category) },
                        )
                    }
                }
            }
            item {
                Column {
                Text("Choice your course", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
                Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourseTab.entries.forEach { t ->
                        val selected = t == tab
                        Text(
                            t.label,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) Primary else Color.Transparent)
                                .clickable { viewModel.selectTab(t) }
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                }
                }
            }
            if (state.offline) {
                item {
                    Text(
                        "API недоступен — показан встроенный каталог",
                        style = MaterialTheme.typography.bodySmall,
                        color = Orange,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            when {
                state.loading -> item { LoadingBox(Modifier.height(200.dp)) }
                state.error != null -> item {
                    EmptyState(Icons.Filled.CloudOff, "No Network!", "Please check your internet connection and try again", Modifier.height(420.dp), illustration = "illustrations/empty_network.webp") {
                        PrimaryButton("Try again", { viewModel.refresh(force = true) })
                    }
                }
                catalog.isEmpty() -> item {
                    EmptyState(Icons.Filled.Search, "No courses!", "Here is no course you want at the moment", Modifier.height(420.dp), illustration = "illustrations/empty_videos.webp") {
                        PrimaryButton("Show all", { viewModel.showAll() })
                    }
                }
                else -> items(catalog, key = { it.id }) { course ->
                    CourseRow(course, { onCourse(course.id) }, Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                }
            }
        }
    }
    if (showFilter) {
        FilterSheet(
            categories = categories,
            maxPrice = maxPrice,
            current = filter,
            onApply = {
                viewModel.applyFilter(it)
                showFilter = false
            },
            onDismiss = { showFilter = false },
        )
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit, filterActive: Boolean, onFilter: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text("Find Course", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        FilterButton(filterActive, onFilter)
    }
}

@Composable
private fun FilterButton(active: Boolean, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) { Icon(Icons.Filled.Tune, "Фильтр", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (active) {
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(8.dp).clip(CircleShape).background(Orange))
        }
    }
}

@Composable
private fun CategoryCard(category: String, image: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 150.dp, height = 84.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 0.dp, if (selected) Primary else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        CourseImage(image, Modifier.fillMaxSize())
        Text(
            category,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** Search tab: text field with clear and filter buttons, quick suggestions and "Results". */
@Composable
fun SearchScreen(viewModel: CoursesViewModel, onCourse: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val maxPrice by viewModel.maxPrice.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    var showFilter by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (query.isEmpty()) runCatching { focus.requestFocus() } }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Search", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Find Course") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) { Icon(Icons.Filled.Close, "Очистить") }
                    }
                    FilterButton(filter.isActive) { showFilter = true }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).focusRequester(focus),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions) { s ->
                Text(
                    s,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { viewModel.onQueryChange(s) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Results", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (filter.isActive) TextButton(onClick = { viewModel.applyFilter(CourseFilter()) }) { Text("Clear filter") }
        }
        when {
            state.loading -> LoadingBox()
            results.isEmpty() -> EmptyState(
                Icons.Filled.Search,
                "No courses!",
                "Here is no course you want at the moment",
                illustration = "illustrations/empty_videos.webp",
            ) {
                PrimaryButton("Search more", { viewModel.clearSearch() })
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(results, key = { it.id }) { CourseRow(it, { onCourse(it.id) }) }
            }
        }
    }
    if (showFilter) {
        FilterSheet(
            categories = categories,
            maxPrice = maxPrice,
            current = filter,
            onApply = {
                viewModel.applyFilter(it)
                showFilter = false
            },
            onDismiss = { showFilter = false },
        )
    }
}

private data class CardColors(val background: Color, val accent: Color)

private val MY_COURSE_COLORS = listOf(
    CardColors(Color(0xFFFFE4EC), Color(0xFFEE6A8F)),
    CardColors(Color(0xFFBAD6FF), Primary),
    CardColors(Color(0xFFBDE7D9), Color(0xFF2E8A72)),
)

/** My courses: "Learned today" on top and a two-column grid of colored course cards. */
@Composable
fun MyCoursesScreen(
    viewModel: HomeViewModel,
    onCourse: (String) -> Unit,
    onBrowse: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mine = state.myCourses

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
            }
            Text("My courses", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Center))
        }
        if (mine.isEmpty()) {
            EmptyState(
                Icons.Filled.Search,
                "No products",
                "You don't have any products yet!",
                illustration = "illustrations/empty_products.webp",
            ) {
                PrimaryButton("Browse courses", onBrowse)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LearnedTodayCard(minutes = state.todayMinutes, goalMinutes = state.goalMinutes, modifier = Modifier.padding(bottom = 8.dp))
                }
                itemsIndexed(mine, key = { _, c -> c.course.id }) { index, item ->
                    MyCourseCard(item.course, item.done, item.total, MY_COURSE_COLORS[index % MY_COURSE_COLORS.size]) { onCourse(item.course.id) }
                }
            }
        }
    }
}

@Composable
private fun MyCourseCard(course: Course, done: Int, total: Int, colors: CardColors, onClick: () -> Unit) {
    Column(
        Modifier
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        val progress = if (total == 0) 0f else done.toFloat() / total
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.7f))) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(colors.accent))
        }
        Spacer(Modifier.height(8.dp))
        Text("Completed", style = MaterialTheme.typography.bodySmall, color = Ink.copy(alpha = 0.7f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$done/$total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(CircleShape).background(colors.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, "Продолжить", tint = Color.White)
            }
        }
    }
}
