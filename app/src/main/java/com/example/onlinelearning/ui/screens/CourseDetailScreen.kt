package com.example.onlinelearning.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.onlinelearning.data.model.Course
import com.example.onlinelearning.data.model.Lesson
import com.example.onlinelearning.ui.CoursesViewModel
import com.example.onlinelearning.ui.SessionViewModel
import com.example.onlinelearning.ui.components.CourseImage
import com.example.onlinelearning.ui.components.EmptyState
import com.example.onlinelearning.ui.components.LoadingBox
import com.example.onlinelearning.ui.components.PrimaryButton
import com.example.onlinelearning.ui.theme.Orange
import com.example.onlinelearning.ui.theme.PinkLight
import com.example.onlinelearning.ui.theme.Primary

@Composable
fun CourseDetailScreen(
    courseId: String,
    session: SessionViewModel,
    courses: CoursesViewModel,
    onBack: () -> Unit,
    showMessage: (String) -> Unit,
) {
    val state by courses.state.collectAsStateWithLifecycle()
    val profile by session.profile.collectAsStateWithLifecycle()
    val course = state.courses.firstOrNull { it.id == courseId }
    val context = LocalContext.current

    var showPayment by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    if (course == null) {
        if (state.loading) LoadingBox() else EmptyState(Icons.Filled.Lock, "Course not found", "")
        return
    }

    val purchased = course.price == 0.0 || profile?.purchased?.contains(course.id) == true
    val favorite = profile?.favorites?.contains(course.id) == true
    val completed = profile?.progress?.get(course.id).orEmpty()

    fun play(lesson: Lesson) {
        if (!purchased && !lesson.free) {
            showMessage("Купите курс, чтобы открыть этот урок")
            return
        }
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(lesson.videoUrl), "video/*")
            )
            session.completeLesson(course, lesson)
        } catch (e: ActivityNotFoundException) {
            showMessage("Нет приложения для воспроизведения видео")
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            item {
                Box(Modifier.fillMaxWidth().background(PinkLight)) {
                    CourseImage(course.imageUrl, Modifier.fillMaxWidth().height(260.dp))
                    IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            item {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(course.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Text(
                            if (course.price == 0.0) "Free" else course.priceLabel,
                            color = Primary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${course.durationLabel} · ${course.lessons.size} Lessons",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("About this course", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(course.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            itemsIndexed(course.lessons, key = { _, l -> l.id }) { index, lesson ->
                LessonRow(
                    number = index + 1,
                    lesson = lesson,
                    unlocked = purchased || lesson.free,
                    done = lesson.id in completed,
                    onClick = { play(lesson) },
                )
            }
        }
        Surface(shadowElevation = 8.dp) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(width = 70.dp, height = 50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PinkLight)
                        .clickable { session.toggleFavorite(course.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "В избранное",
                        tint = Orange,
                    )
                }
                if (purchased) {
                    PrimaryButton("Start learning", { course.lessons.firstOrNull { it.id !in completed }?.let(::play) }, Modifier.weight(1f))
                } else {
                    PrimaryButton("Buy Now", { showPayment = true }, Modifier.weight(1f))
                }
            }
        }
    }

    if (showPayment) {
        PaymentSheet(
            course = course,
            onDismiss = { showPayment = false },
            onPay = {
                session.purchase(course.id) {
                    showPayment = false
                    showSuccess = true
                }
            },
        )
    }
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false },
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = Primary, modifier = Modifier.size(56.dp)) },
            title = { Text("Purchase Success") },
            text = { Text("Course «${course.title}» is now available in My courses", textAlign = TextAlign.Center) },
            confirmButton = { TextButton(onClick = { showSuccess = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun LessonRow(number: Int, lesson: Lesson, unlocked: Boolean, done: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "%02d".format(number),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(48.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${lesson.durationMinutes} mins" + if (done) " ✓" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (done) Orange else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (unlocked) Primary else Primary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (unlocked) Icons.Filled.PlayArrow else Icons.Filled.Lock, null, tint = androidx.compose.ui.graphics.Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentSheet(course: Course, onDismiss: () -> Unit, onPay: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp))) {
            Text("Payment", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Row {
                Text(course.title, Modifier.weight(1f))
                Text(course.priceLabel, fontWeight = FontWeight.Bold, color = Primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Демо-оплата: деньги не списываются, покупка сохраняется в Firestore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Pay Now", onPay)
        }
    }
}
