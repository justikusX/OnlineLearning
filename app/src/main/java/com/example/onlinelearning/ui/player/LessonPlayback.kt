package com.example.onlinelearning.ui.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.onlinelearning.data.model.Lesson
import com.example.onlinelearning.data.model.LessonKind
import java.util.Locale

/**
 * One lesson being played in the lesson sheet. State is Compose-observable; the sheet calls [tick]
 * twice a second to refresh the position.
 */
interface LessonPlayback {
    val isPlaying: Boolean
    val buffering: Boolean
    val ended: Boolean
    val error: String?
    val positionMs: Long

    /** 0 while unknown. */
    val durationMs: Long

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun tick() {}
    fun release()

    companion object {
        fun create(context: Context, lesson: Lesson): LessonPlayback = when (lesson.kind) {
            LessonKind.AUDIO -> MediaLessonPlayback(context, lesson.audioUrl)
            LessonKind.VIDEO -> MediaLessonPlayback(context, lesson.videoUrl)
            LessonKind.NARRATION -> NarrationPlayback(context, lesson.text)
            LessonKind.NONE -> MediaLessonPlayback(context, "")
        }
    }
}

/** Streams audio or video with Media3 ExoPlayer. Video is drawn by a PlayerView bound to [player]. */
class MediaLessonPlayback(context: Context, url: String) : LessonPlayback {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    override var isPlaying by mutableStateOf(false)
        private set
    override var buffering by mutableStateOf(true)
        private set
    override var ended by mutableStateOf(false)
        private set
    override var error by mutableStateOf<String?>(null)
        private set
    override var positionMs by mutableLongStateOf(0L)
        private set
    override var durationMs by mutableLongStateOf(0L)
        private set

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
        }

        override fun onPlaybackStateChanged(state: Int) {
            buffering = state == Player.STATE_BUFFERING
            ended = state == Player.STATE_ENDED
            tick()
        }

        override fun onPlayerError(e: PlaybackException) {
            buffering = false
            error = "Не удалось загрузить урок. Проверьте подключение к интернету."
        }
    }

    init {
        player.addListener(listener)
        if (url.isBlank()) {
            buffering = false
            error = "У этого урока нет записи"
        } else {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        }
    }

    override fun play() {
        if (error != null) {
            error = null
            player.prepare()
        }
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        player.play()
    }

    override fun pause() = player.pause()

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0))
        this.positionMs = positionMs.coerceAtLeast(0)
    }

    override fun setSpeed(speed: Float) = player.setPlaybackSpeed(speed)

    override fun tick() {
        positionMs = player.currentPosition
        val d = player.duration
        if (d != C.TIME_UNSET && d > 0) durationMs = d
    }

    override fun release() {
        player.removeListener(listener)
        player.release()
    }
}

/**
 * Reads a lesson script aloud with the device's TextToSpeech engine (English voice), sentence by sentence.
 * Duration and position are estimated from the text length, so seeking jumps between sentences.
 */
class NarrationPlayback(context: Context, text: String) : LessonPlayback {

    private val sentences: List<String> = text
        .split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    /** Estimated start of each sentence at 1x, plus the total length as the last element. */
    private val starts: LongArray = LongArray(sentences.size + 1).also { arr ->
        for (i in sentences.indices) arr[i + 1] = arr[i] + (sentences[i].length * MS_PER_CHAR).toLong() + PAUSE_MS
    }

    override var isPlaying by mutableStateOf(false)
        private set
    override var buffering by mutableStateOf(true)
        private set
    override var ended by mutableStateOf(false)
        private set
    override var error by mutableStateOf<String?>(null)
        private set
    override var positionMs by mutableLongStateOf(0L)
        private set
    override val durationMs: Long = starts.last()

    private val main = Handler(Looper.getMainLooper())
    private var index = 0
    private var sentenceStartedAt = 0L
    private var speed = 1f
    private var ready = false
    private var wantPlay = true
    private var released = false

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        main.post { onInit(status) }
    }

    private fun onInit(status: Int) {
        if (released) return
        buffering = false
        if (status != TextToSpeech.SUCCESS) {
            error = "Синтез речи недоступен на этом устройстве"
            return
        }
        val result = tts.setLanguage(Locale.US)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            error = "Нет английского голоса: Настройки → Синтез речи → установите English (US)"
            return
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                main.post {
                    index = utteranceId.toIntOrNull() ?: index
                    sentenceStartedAt = SystemClock.elapsedRealtime()
                    positionMs = starts[index]
                    isPlaying = true
                }
            }

            override fun onDone(utteranceId: String) {
                main.post {
                    if (utteranceId.toIntOrNull() == sentences.lastIndex) {
                        isPlaying = false
                        ended = true
                        wantPlay = false
                        positionMs = durationMs
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                main.post {
                    isPlaying = false
                    error = "Ошибка синтеза речи"
                }
            }
        })
        tts.setSpeechRate(speed)
        ready = true
        if (sentences.isEmpty()) error = "У этого урока нет текста"
        else if (wantPlay) speakFrom(index)
    }

    private fun speakFrom(from: Int) {
        tts.stop()
        for (i in from..sentences.lastIndex) {
            tts.speak(sentences[i], if (i == from) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, i.toString())
        }
    }

    override fun play() {
        wantPlay = true
        if (ended) {
            ended = false
            index = 0
            positionMs = 0
        }
        if (ready && error == null) speakFrom(index)
    }

    override fun pause() {
        wantPlay = false
        if (ready) tts.stop()
        isPlaying = false
    }

    override fun seekTo(positionMs: Long) {
        if (sentences.isEmpty()) return
        index = (starts.indexOfLast { it <= positionMs }).coerceIn(0, sentences.lastIndex)
        ended = false
        this.positionMs = starts[index]
        if (ready && wantPlay) speakFrom(index)
    }

    override fun setSpeed(speed: Float) {
        this.speed = speed
        if (!ready) return
        tts.setSpeechRate(speed)
        if (isPlaying) speakFrom(index) // the new rate applies to newly queued sentences
    }

    override fun tick() {
        if (!isPlaying) return
        val elapsed = ((SystemClock.elapsedRealtime() - sentenceStartedAt) * speed).toLong()
        positionMs = (starts[index] + elapsed).coerceAtMost(starts[index + 1])
    }

    override fun release() {
        released = true
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        /** About 150 words per minute at 1x. */
        const val MS_PER_CHAR = 1000.0 / 14.5
        const val PAUSE_MS = 350L
    }
}
