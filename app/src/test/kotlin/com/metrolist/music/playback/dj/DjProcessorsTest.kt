package com.metrolist.music.playback.dj

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

class DjProcessorsTest {
    private val sampleRate = 44_100
    private val format = AudioProcessor.AudioFormat(sampleRate, 2, C.ENCODING_PCM_16BIT)

    private fun pcm(samples: FloatArray): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(samples.size * 2 * 2).order(ByteOrder.nativeOrder())
        for (s in samples) {
            val v = (s * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            buffer.putShort(v) // left
            buffer.putShort(v) // right
        }
        buffer.flip()
        return buffer
    }

    private fun leftChannel(buffer: ByteBuffer): FloatArray {
        val out = FloatArray(buffer.remaining() / 4)
        for (i in out.indices) out[i] = buffer.getShort(buffer.position() + i * 4) / 32768f
        return out
    }

    // Amplitude of one frequency in a signal (single-bin DFT)
    private fun amplitudeAt(signal: FloatArray, hz: Double): Double {
        var re = 0.0
        var im = 0.0
        for (i in signal.indices) {
            val angle = 2 * PI * hz * i / sampleRate
            re += signal[i] * kotlin.math.cos(angle)
            im += signal[i] * sin(angle)
        }
        return 2 * sqrt(re * re + im * im) / signal.size
    }

    private fun runFilter(bassCut: Float, input: FloatArray): FloatArray {
        val filter = DjFilterAudioProcessor()
        filter.configure(format)
        filter.flush()
        filter.bassCut = bassCut
        val output = ArrayList<Float>()
        // Feed in small chunks, like the player does
        var start = 0
        while (start < input.size) {
            val end = minOf(start + 1024, input.size)
            filter.queueInput(pcm(input.copyOfRange(start, end)))
            output.addAll(leftChannel(filter.output).toList())
            start = end
        }
        return output.toFloatArray()
    }

    @Test
    fun bassCutRemovesBassAndKeepsTheRest() {
        val n = sampleRate
        val input = FloatArray(n) { i -> (0.4 * sin(2 * PI * 60 * i / sampleRate) + 0.4 * sin(2 * PI * 2000 * i / sampleRate)).toFloat() }
        val cut = runFilter(1f, input)
        // Skip the first 0.2 s while the filter glides in
        val settled = cut.copyOfRange(sampleRate / 5, cut.size)
        val reference = input.copyOfRange(sampleRate / 5, input.size)
        val bassBefore = amplitudeAt(reference, 60.0)
        val bassAfter = amplitudeAt(settled, 60.0)
        val highBefore = amplitudeAt(reference, 2000.0)
        val highAfter = amplitudeAt(settled, 2000.0)
        assertTrue("60 Hz only fell from $bassBefore to $bassAfter", bassAfter < bassBefore * 0.1)
        assertEquals("2 kHz should be kept", highBefore, highAfter, highBefore * 0.1)
    }

    @Test
    fun idleFilterIsBitExact() {
        val input = FloatArray(4096) { i -> (0.3 * sin(i * 0.05)).toFloat() }
        val out = runFilter(0f, input)
        assertEquals(input.size, out.size)
        for (i in input.indices) {
            val expected = (input[i] * 32767f).toInt().toShort() / 32768f
            assertEquals(expected, out[i], 1e-6f)
        }
    }

    @Test
    fun analyzerFindsTempoAndFirstBeatFromPcm() {
        val bpm = 124.0
        val delay = 0.8
        val seconds = 16.0
        val beat = 60.0 / bpm
        val samples =
            FloatArray((seconds * sampleRate).toInt()) { i ->
                val t = i.toDouble() / sampleRate - delay
                if (t < 0) {
                    0f
                } else {
                    val sinceBeat = t % beat
                    (0.8 * exp(-sinceBeat * 18) * sin(2 * PI * 55 * sinceBeat) + 0.15 * sin(2 * PI * 82.4 * t)).toFloat()
                }
            }

        val analyzer = BeatAnalyzerAudioProcessor()
        analyzer.configure(format)
        analyzer.flush()
        analyzer.enabled = true
        var start = 0
        while (start < samples.size) {
            val end = minOf(start + 2048, samples.size)
            analyzer.queueInput(pcm(samples.copyOfRange(start, end)))
            analyzer.output // drain
            start = end
        }

        val snapshot = analyzer.snapshot
        assertNotNull(snapshot)
        assertEquals(bpm, snapshot!!.bpm, 1.0)
        assertTrue(snapshot.confidence >= TempoEstimator.MIN_CONFIDENCE)
        assertNotNull(snapshot.firstBeatSeconds)
        assertEquals(delay, snapshot.firstBeatSeconds!!, 0.04)
        assertEquals(beat, snapshot.periodMediaSeconds, 0.01)
    }
}
