package com.example.onlinelearning.view.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.onlinelearning.model.entity.AppNotification
import com.example.onlinelearning.view.components.EmptyState
import com.example.onlinelearning.view.theme.Orange
import com.example.onlinelearning.view.theme.Primary
import com.example.onlinelearning.viewmodel.MessagesViewModel

/** Message tab, titled "Notifications" in the design: "message" and "notification" sub-tabs. */
@Composable
fun MessagesScreen(viewModel: MessagesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tab = state.tab

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Notifications", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp))
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(positions[tab]).padding(horizontal = 48.dp), color = Primary)
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            listOf("message", "notification").forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { viewModel.selectTab(index) },
                    text = { Text(title, style = MaterialTheme.typography.bodyLarge) },
                    selectedContentColor = MaterialTheme.colorScheme.onSurface,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val shown = state.shown
        when {
            !state.signedIn -> EmptyState(
                Icons.Filled.Notifications,
                "No Notifications yet!",
                "Log in — we'll notify you once we have something for you",
                illustration = "illustrations/empty_notifications.webp",
            )
            shown.isEmpty() -> EmptyState(
                Icons.Filled.Notifications,
                "No Notifications yet!",
                "We'll notify you once we have something for you",
                illustration = "illustrations/empty_notifications.webp",
            )
            tab == 0 -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shown, key = { it.time.toString() + it.text.hashCode() }) { MessageCard(it) }
            }
            else -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shown, key = { it.time.toString() + it.text.hashCode() }) { NotificationRow(it) }
            }
        }
    }
}

@Composable
private fun MessageCard(item: AppNotification) {
    Surface(shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFBDE7D9)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.School, null, tint = Color(0xFF2E8A72))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Online", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(relativeTime(item.time), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Text(item.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotificationRow(item: AppNotification) {
    val (icon: ImageVector, tint: Color) = when (item.type) {
        "purchase" -> Icons.Filled.CreditCard to Orange
        "course" -> Icons.Filled.EmojiEvents to Orange
        else -> Icons.AutoMirrored.Filled.Chat to Primary
    }
    Surface(shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = tint)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (item.text.isNotBlank()) {
                    Text(item.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Text(relativeTime(item.time), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

/** "Just now", "5 min ago", "3 h ago", "2 days ago". */
private fun relativeTime(time: Long): String {
    val minutes = ((System.currentTimeMillis() - time) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        else -> "${minutes / (24 * 60)} days ago"
    }
}
