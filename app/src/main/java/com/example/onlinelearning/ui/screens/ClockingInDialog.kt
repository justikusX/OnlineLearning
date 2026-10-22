package com.example.onlinelearning.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.onlinelearning.data.model.UserProfile
import com.example.onlinelearning.ui.components.PrimaryButton
import com.example.onlinelearning.ui.theme.Primary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** "Clocking in! GOOD JOB!" card: today, total hours, active days and this week's record. */
@Composable
fun ClockingInDialog(profile: UserProfile?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val todayMinutes = profile?.minutesOn(today) ?: 0
    val totalHours = (profile?.totalMinutes ?: 0L) / 60.0
    val days = profile?.activity?.count { it.value > 0 } ?: 0
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val week = (0..6).map { monday.plusDays(it.toLong()) }

    Dialog(onDismissRequest = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp)) {
                    Text("Clocking in!", style = MaterialTheme.typography.headlineSmall)
                    Text("GOOD JOB!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    Row {
                        Stat("Learned today", "$todayMinutes", "min", Modifier.weight(1f))
                        Stat("Totally hours", "%.1f".format(totalHours), "hrs", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Stat("Totally days", "$days", "days")
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Record of this week",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        week.forEachIndexed { i, day ->
                            val active = (profile?.minutesOn(day) ?: 0) > 0
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (active) Primary else MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${i + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton("Share", {
                        val text = "I learned $todayMinutes min today and ${"%.1f".format(totalHours)} hours in total " +
                            "in Online Learning — $days days of study!"
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                        context.startActivity(Intent.createChooser(send, null))
                    })
                }
            }
            Spacer(Modifier.height(20.dp))
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)),
            ) {
                Icon(Icons.Filled.Close, "Закрыть", tint = Color.White)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(" $unit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
