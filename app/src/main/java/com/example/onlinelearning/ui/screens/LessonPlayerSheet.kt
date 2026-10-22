package com.example.onlinelearning.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.onlinelearning.data.model.Course
import com.example.onlinelearning.data.model.Lesson
import com.example.onlinelearning.data.model.LessonKind
import com.example.onlinelearning.ui.player.LessonPlayback
import com.example.onlinelearning.ui.player.MediaLessonPlayback
import com.example.onlinelearning.ui.theme.PinkLight
import com.example.onlinelearning.ui.theme.Primary
import kotlinx.coroutines.delay

private val SPEEDS = listOf(1f, 1.25f, 1.5f, 2f)

/** A lesson counts as done once this share of it has been played. */
private const val COMPLETE_AT = 0.9f

/**
 * Bottom-sheet player for every lesson kind: streamed audio, streamed video (shown above the controls)
 * and on-device narration of the lesson text. Playback stops when the sheet is closed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerSheet(
    course: Course,
    lesson: Lesson,
    hasNext: Boolean,
    onNext: () -> Unit,
    onCompleted: (Lesson) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val playback = remember(lesson.id) { LessonPlayback.create(context, lesson) }
    DisposableEffect(playback) { onDispose { playback.release() } }
    val currentOnCompleted by rememberUpdatedState(onCompleted)

    var speedIndex by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    // Keep the chosen speed when moving to the next lesson.
    LaunchedEffect(playback) { playback.setSpeed(SPEEDS[speedIndex]) }

    // Refresh the position and report completion once.
    LaunchedEffect(playback) {
        var reported = false
        while (true) {
            playback.tick()
            val d = playback.durationMs
            if (!reported && (playback.ended || (d > 0 && playback.positionMs >= d * COMPLETE_AT))) {
                reported = true
                currentOnCompleted(lesson)
            }
            delay(500)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (lesson.kind == LessonKind.VIDEO && playback is MediaLessonPlayback) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            keepScreenOn = true
                            player = playback.player
                        }
                    },
                    update = { it.player = playback.player },
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(Color.Black),
                )
            } else {
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(PinkLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (lesson.kind == LessonKind.NARRATION) Icons.Filled.RecordVoiceOver else Icons.Filled.Headphones,
                        null,
                        tint = Primary,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(lesson.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                "${course.title} · ${course.author}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            val duration = playback.durationMs
            val total = duration.coerceAtLeast(1L)
            Slider(
                value = if (dragging) dragValue else (playback.positionMs.toFloat() / total).coerceIn(0f, 1f),
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    playback.seekTo((dragValue * total).toLong())
                    dragging = false
                },
                enabled = duration > 0,
                colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary),
            )
            Row(Modifier.fillMaxWidth()) {
                val shown = if (dragging) (dragValue * total).toLong() else playback.positionMs
                Text(formatTime(shown), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                val prefix = if (lesson.kind == LessonKind.NARRATION) "≈ " else ""
                Text(if (duration > 0) prefix + formatTime(duration) else "--:--", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    speedIndex = (speedIndex + 1) % SPEEDS.size
                    playback.setSpeed(SPEEDS[speedIndex])
                }) {
                    Text("${SPEEDS[speedIndex]}x".replace(".0x", "x"), fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { playback.seekTo(playback.positionMs - 10_000) }) {
                    Icon(Icons.Filled.Replay10, "Назад на 10 секунд")
                }
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    if (playback.buffering && playback.error == null) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    } else {
                        IconButton(onClick = { if (playback.isPlaying) playback.pause() else playback.play() }) {
                            Icon(
                                if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                if (playback.isPlaying) "Пауза" else "Слушать",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = { playback.seekTo(playback.positionMs + 10_000) }) {
                    Icon(Icons.Filled.Forward10, "Вперёд на 10 секунд")
                }
                IconButton(onClick = onNext, enabled = hasNext) {
                    Icon(Icons.Filled.SkipNext, "Следующий урок")
                }
            }

            playback.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            val credit = when {
                lesson.kind == LessonKind.NARRATION -> "Озвучено синтезом речи на устройстве"
                course.sourceName.isNotBlank() -> "Запись: ${course.sourceName}"
                else -> null
            }
            credit?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
