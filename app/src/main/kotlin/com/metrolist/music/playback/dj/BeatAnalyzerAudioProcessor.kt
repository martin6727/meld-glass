package com.metrolist.music.playback.dj

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * What the analyser currently knows about a player's music.
 *
 * Times are on the processing clock: when the audio passed through this processor, on
 * [System.nanoTime]. Every player's audio chain runs the same distance ahead of the speaker,
 * so two players' beats line up on this clock when they line up in your ears.
 */
data class BeatSnapshot(
    val bpm: Double,
    val confidence: Double,
    /** A recent beat on the processing clock. */
    val beatNanos: Long,
    /** Beat length on the processing clock at the current playback speed. */
    val periodNanos: Double,
    /** Beat length in the song's own time. */
    val periodMediaSeconds: Double,
    /** Song time analysed since the last seek. */
    val analyzedSeconds: Double,
    /** First beat after leading silence, in song time since the last seek. Only set while the window still covers that point. */
    val firstBeatSeconds: Double?,
)

/**
 * Pass-through processor that listens to a player's audio and keeps a live tempo and beat grid.
 * Costs almost nothing while [enabled] is false.
 */
@UnstableApi
class BeatAnalyzerAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var enabled = false

    /** Playback speed applied after this processor; set by the service whenever it changes. */
    @Volatile
    var speed = 1f

    @Volatile
    var snapshot: BeatSnapshot? = null
        private set

    private var sampleRate = 0
    private var channelCount = 0
    private var floatInput = false
    private var detector: OnsetDetector? = null

    private val envelope = FloatArray(CAPACITY)
    private val hopDb = FloatArray(CAPACITY)
    private val hopScaledSeconds = DoubleArray(CAPACITY)
    private var hopCount = 0L
    private var hopsSinceEstimate = 0

    // Song seconds processed since the last flush, and the same divided by speed ("clock seconds")
    private var mediaSeconds = 0.0
    private var scaledSeconds = 0.0

    // Smoothed processing-clock time of scaledSeconds == 0
    private var anchorNanos = Double.NaN

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        floatInput = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && !floatInput) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        detector = OnsetDetector(sampleRate)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        if (enabled && channelCount > 0) analyze(inputBuffer)
        replaceOutputBuffer(remaining).put(inputBuffer).flip()
    }

    private fun analyze(input: ByteBuffer) {
        val detector = detector ?: return
        val now = System.nanoTime()
        val currentSpeed = speed.coerceIn(0.25f, 4f).toDouble()

        // The first frame of this buffer is processed "now"; average that over many buffers
        val candidate = now - scaledSeconds * 1e9
        anchorNanos = if (anchorNanos.isNaN()) candidate else anchorNanos + ANCHOR_SMOOTHING * (candidate - anchorNanos)

        input.order(ByteOrder.nativeOrder())
        val bytesPerSample = if (floatInput) 4 else 2
        val frameBytes = bytesPerSample * channelCount
        val frames = input.remaining() / frameBytes
        val start = input.position()
        val frameMedia = 1.0 / sampleRate
        val frameScaled = frameMedia / currentSpeed

        for (f in 0 until frames) {
            val base = start + f * frameBytes
            var sum = 0f
            for (c in 0 until channelCount) {
                val index = base + c * bytesPerSample
                sum += if (floatInput) input.getFloat(index) else input.getShort(index) / 32768f
            }
            val onset = detector.push(sum / channelCount)
            mediaSeconds += frameMedia
            scaledSeconds += frameScaled
            if (!onset.isNaN()) {
                val slot = (hopCount % CAPACITY).toInt()
                envelope[slot] = onset
                hopDb[slot] = detector.lastHopDb
                hopScaledSeconds[slot] = scaledSeconds
                hopCount++
                if (++hopsSinceEstimate >= ESTIMATE_EVERY_HOPS) {
                    hopsSinceEstimate = 0
                    publishEstimate(detector.hopSeconds, currentSpeed)
                }
            }
        }
        clock = Clock(anchorNanos, mediaSeconds, scaledSeconds)
    }

    private fun publishEstimate(hopSeconds: Double, currentSpeed: Double) {
        val count = minOf(hopCount, WINDOW_HOPS.toLong()).toInt()
        if (count * hopSeconds < MIN_WINDOW_SECONDS) return
        val firstHop = hopCount - count
        val window = FloatArray(count)
        val windowDb = FloatArray(count)
        for (i in 0 until count) {
            val slot = ((firstHop + i) % CAPACITY).toInt()
            window[i] = envelope[slot]
            windowDb[i] = hopDb[slot]
        }
        val estimate = TempoEstimator.estimate(window, hopSeconds) ?: return

        // Latest beat of the grid inside the window, placed on the processing clock
        var beatHop = estimate.phaseHops
        while (beatHop + estimate.periodHops <= count - 1) beatHop += estimate.periodHops
        val i = beatHop.toInt().coerceIn(0, count - 1)
        val frac = beatHop - i
        val slotA = ((firstHop + i) % CAPACITY).toInt()
        val slotB = ((firstHop + minOf(i + 1, count - 1)) % CAPACITY).toInt()
        val beatScaled = hopScaledSeconds[slotA] * (1 - frac) + hopScaledSeconds[slotB] * frac

        val periodMedia = estimate.periodHops * hopSeconds
        val firstBeat =
            if (firstHop == 0L) {
                TempoEstimator.firstBeatHop(estimate, windowDb)?.let { it * hopSeconds }
            } else {
                null
            }
        snapshot =
            BeatSnapshot(
                bpm = estimate.bpm,
                confidence = estimate.confidence,
                beatNanos = (anchorNanos + beatScaled * 1e9).toLong(),
                periodNanos = periodMedia / currentSpeed * 1e9,
                periodMediaSeconds = periodMedia,
                analyzedSeconds = mediaSeconds,
                firstBeatSeconds = firstBeat,
            )
    }

    /** Where the processing head is, published once per buffer for other threads. */
    class Clock(val anchorNanos: Double, val mediaSeconds: Double, val scaledSeconds: Double)

    @Volatile
    var clock: Clock? = null
        private set

    /**
     * Processing-clock time at which song time [songSeconds] (since the last seek) passes through
     * this processor, extrapolated at the current speed.
     */
    fun nanosForSongTime(songSeconds: Double): Long? {
        val c = clock ?: return null
        val speedNow = speed.coerceIn(0.25f, 4f).toDouble()
        val scaled = c.scaledSeconds + (songSeconds - c.mediaSeconds) / speedNow
        return (c.anchorNanos + scaled * 1e9).toLong()
    }

    override fun onFlush() {
        clearAnalysis()
    }

    override fun onReset() {
        clearAnalysis()
        detector = null
        sampleRate = 0
        channelCount = 0
    }

    private fun clearAnalysis() {
        detector?.reset()
        hopCount = 0
        hopsSinceEstimate = 0
        mediaSeconds = 0.0
        scaledSeconds = 0.0
        anchorNanos = Double.NaN
        snapshot = null
        clock = null
    }

    companion object {
        private const val CAPACITY = 2048
        private const val WINDOW_HOPS = 1600 // 16 s of analysis
        private const val MIN_WINDOW_SECONDS = 6.0
        private const val ESTIMATE_EVERY_HOPS = 50 // re-estimate twice a second
        private const val ANCHOR_SMOOTHING = 0.05
    }
}
