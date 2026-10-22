package com.example.onlinelearning.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.onlinelearning.BuildConfig
import com.example.onlinelearning.data.AvatarCodec
import com.example.onlinelearning.data.ThemeMode
import com.example.onlinelearning.ui.CoursesViewModel
import com.example.onlinelearning.ui.SessionViewModel
import com.example.onlinelearning.ui.components.CourseRow
import com.example.onlinelearning.ui.components.EmptyState
import com.example.onlinelearning.ui.components.PrimaryButton
import com.example.onlinelearning.ui.components.UserAvatar
import com.example.onlinelearning.ui.theme.Primary
import kotlinx.coroutines.launch

/** Account tab: photo, name and the menu from the design. */
@Composable
fun AccountScreen(
    session: SessionViewModel,
    onFavourites: () -> Unit,
    onEditAccount: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onLogIn: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val user by session.user.collectAsStateWithLifecycle()
    val profile by session.profile.collectAsStateWithLifecycle()
    val name = session.displayName(profile, user)

    // System photo picker: no storage permission needed; falls back to the file picker on old devices.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savingPhoto by remember { mutableStateOf(false) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                savingPhoto = true
                runCatching { AvatarCodec.encode(context, uri) }
                    .onSuccess { session.setAvatar(it) }
                    .onFailure { session.showMessage("Не удалось открыть фото") }
                savingPhoto = false
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        Text("Account", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp))
        Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.clickable {
                    if (user == null) session.showMessage("Войдите, чтобы выбрать фото профиля")
                    else pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            ) {
                UserAvatar(profile?.avatar.orEmpty(), name, 88.dp)
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Primary)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (savingPhoto) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    } else {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "Выбрать фото", tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(name, style = MaterialTheme.typography.titleLarge)
            user?.email?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        MenuRow("Favourite", onFavourites)
        MenuRow("Edit Account", onEditAccount)
        MenuRow("Settings and Privacy", onSettings)
        MenuRow("Help", onHelp)
        HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        if (user == null) {
            ListItem(
                headlineContent = { Text("Log in") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Login, null) },
                modifier = Modifier.clickable(onClick = onLogIn).padding(horizontal = 8.dp),
            )
        } else {
            ListItem(
                headlineContent = { Text("Log out", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier
                    .clickable {
                        session.signOut()
                        onSignedOut()
                    }
                    .padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun MenuRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Top bar with a back arrow and a centered title, used by the Account sub-screens. */
@Composable
fun BackTitleBar(title: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
        }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun FavouritesScreen(session: SessionViewModel, courses: CoursesViewModel, onCourse: (String) -> Unit, onBack: () -> Unit) {
    val profile by session.profile.collectAsStateWithLifecycle()
    val state by courses.state.collectAsStateWithLifecycle()
    val favorites = state.courses.filter { profile?.favorites?.contains(it.id) == true }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackTitleBar("Favourite", onBack)
        if (favorites.isEmpty()) {
            EmptyState(
                Icons.Filled.Favorite,
                "No favourites yet",
                "Tap ☆ on a course to keep it here",
                illustration = "illustrations/empty_products.webp",
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favorites, key = { it.id }) { CourseRow(it, { onCourse(it.id) }) }
            }
        }
    }
}

@Composable
fun EditAccountScreen(session: SessionViewModel, onBack: () -> Unit) {
    val user by session.user.collectAsStateWithLifecycle()
    val profile by session.profile.collectAsStateWithLifecycle()
    var name by rememberSaveable(profile?.name) { mutableStateOf(profile?.name ?: user?.displayName.orEmpty()) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackTitleBar("Edit Account", onBack)
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val contact = user?.email ?: user?.phoneNumber
            if (!contact.isNullOrBlank()) {
                OutlinedTextField(
                    value = contact,
                    onValueChange = {},
                    label = { Text(if (user?.email != null) "Your Email" else "Phone") },
                    readOnly = true,
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                "Photo can be changed by tapping it on the Account screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton("Save", { session.updateName(name, onBack) }, enabled = user != null)
            if (!profile?.avatar.isNullOrBlank()) {
                TextButton(onClick = { session.setAvatar(null) }) { Text("Удалить фото профиля") }
            }
        }
    }
}

@Composable
fun SettingsScreen(themeMode: ThemeMode, onThemeMode: (ThemeMode) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        BackTitleBar("Settings and Privacy", onBack)
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            ThemeSelector(themeMode, onThemeMode)
            Spacer(Modifier.height(12.dp))
            Text("Privacy", style = MaterialTheme.typography.titleMedium)
            Text(
                "Your name, photo, favourites, purchases and lesson progress are stored in your own Firebase " +
                    "document (users/{your id}); only you can read or change it. Payments in this app are a demo: " +
                    "no money is charged and no card data is stored.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
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

private val FAQ = listOf(
    "How do lessons work?" to "Tap a lesson to play it. Audio lessons and narrated lessons play in a player at the bottom; " +
        "video lectures play at the top of the course screen. A lesson is marked done after 90% of it.",
    "A narrated lesson is silent" to "Narrated lessons use your phone's text-to-speech voice. Open Settings → Text-to-speech " +
        "and install an English (US) voice.",
    "Is payment real?" to "No. Buy Now opens a demo checkout: any 6-digit password works and no money is charged.",
    "Where is my progress?" to "In your account in Firebase. Log in on another phone and your courses, progress and photo come with you.",
    "How do I contact you?" to "Open an issue in the project repository on GitHub (justikusX/OnlineLearning).",
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        BackTitleBar("Help", onBack)
        FAQ.forEach { (question, answer) ->
            Column(Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
                Text(question, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
