package com.example.onlinelearning.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.onlinelearning.data.model.Course
import com.example.onlinelearning.data.model.Lesson
import com.example.onlinelearning.ui.theme.PinkLight
import com.example.onlinelearning.ui.theme.Primary
import kotlinx.coroutines.delay

private val SPEEDS = listOf(1f, 1.25f, 1.5f, 2f)

/** A lesson counts as listened once this share of it has been played. */
private const val COMPLETE_AT = 0.9f

/**
 * Bottom-sheet player for audio lessons. Streams [Lesson.audioUrl] with Media3 ExoPlayer;
 * playback stops when the sheet is closed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerSheet(
    course: Course,
    lesson: Lesson,
    hasNext: Boolean,
    onNext: () -> Unit,
    onCompleted: (Lesson) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    val currentLesson by rememberUpdatedState(lesson)
    val currentOnCompleted by rememberUpdatedState(onCompleted)

    var isPlaying by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var speedIndex by remember { mutableStateOf(0) }
    var reportedFor by remember { mutableStateOf<String?>(null) }

    fun markCompleted() {
        if (reportedFor != currentLesson.id) {
            reportedFor = currentLesson.id
            currentOnCompleted(currentLesson)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) duration = player.duration.coerceAtLeast(0L)
                if (state == Player.STATE_ENDED) markCompleted()
            }

            override fun onPlayerError(e: PlaybackException) {
                buffering = false
                error = "Не удалось загрузить урок. Проверьте подключение к интернету."
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Load a new lesson whenever the selection changes (also on "next lesson").
    LaunchedEffect(lesson.id) {
        error = null
        buffering = true
        position = 0L
        duration = 0L
        player.setMediaItem(MediaItem.fromUri(lesson.audioUrl))
        player.prepare()
        player.playWhenReady = true
    }

    // Poll the position for the progress bar and completion tracking.
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition
            val d = player.duration
            if (d != C.TIME_UNSET && d > 0) {
                duration = d
                if (position >= d * COMPLETE_AT) markCompleted()
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
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(PinkLight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Headphones, null, tint = Primary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                lesson.title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                "${course.title} · ${course.author}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            val total = duration.coerceAtLeast(1L)
            Slider(
                value = if (dragging) dragValue else (position.toFloat() / total).coerceIn(0f, 1f),
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    player.seekTo((dragValue * total).toLong())
                    position = (dragValue * total).toLong()
                    dragging = false
                },
                enabled = duration > 0,
                colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatTime(if (dragging) (dragValue * total).toLong() else position), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Text(if (duration > 0) formatTime(duration) else "--:--", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    speedIndex = (speedIndex + 1) % SPEEDS.size
                    player.setPlaybackSpeed(SPEEDS[speedIndex])
                }) {
                    Text("${SPEEDS[speedIndex]}x".replace(".0x", "x"), fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }) {
                    Icon(Icons.Filled.Replay10, "Назад на 10 секунд")
                }
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    if (buffering && error == null) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    } else {
                        IconButton(onClick = {
                            if (error != null) {
                                error = null
                                player.prepare()
                                player.play()
                            } else if (player.isPlaying) {
                                player.pause()
                            } else {
                                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                                player.play()
                            }
                        }) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                if (isPlaying) "Пауза" else "Слушать",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = { player.seekTo(player.currentPosition + 10_000) }) {
                    Icon(Icons.Filled.Forward10, "Вперёд на 10 секунд")
                }
                IconButton(onClick = onNext, enabled = hasNext) {
                    Icon(Icons.Filled.SkipNext, "Следующий урок")
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            if (course.sourceName.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Запись: ${course.sourceName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
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
