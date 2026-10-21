package com.metrolist.music.playback.dj

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * DJ-style bass knob. [bassCut] 0 leaves the audio untouched; 1 removes everything below
 * ~250 Hz (kick and bass line). Used to swap the bass between two songs mid-mix so their
 * low ends never stack up. Changes glide over ~60 ms to avoid clicks.
 */
@UnstableApi
class DjFilterAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var bassCut = 0f

    private var current = 0f
    private var sampleRate = 0
    private var channelCount = 0
    private var floatInput = false
    private var glidePerFrame = 0f

    // High-pass biquad coefficients and per-channel state
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var x1 = FloatArray(0)
    private var x2 = FloatArray(0)
    private var y1 = FloatArray(0)
    private var y2 = FloatArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        floatInput = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && !floatInput) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        glidePerFrame = 1f / (GLIDE_SECONDS * sampleRate)
        computeHighPass(CUTOFF_HZ, sampleRate)
        clearState()
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val target = bassCut.coerceIn(0f, 1f)
        if (target == 0f && current == 0f) {
            // Idle: plain copy, and keep the filter ready to start from silence
            clearState()
            replaceOutputBuffer(remaining).put(inputBuffer).flip()
            return
        }

        inputBuffer.order(ByteOrder.nativeOrder())
        val output = replaceOutputBuffer(remaining).order(ByteOrder.nativeOrder())
        val bytesPerSample = if (floatInput) 4 else 2
        val frames = remaining / (bytesPerSample * channelCount)
        for (f in 0 until frames) {
            current += (target - current).coerceIn(-glidePerFrame, glidePerFrame)
            for (c in 0 until channelCount) {
                val x = if (floatInput) inputBuffer.getFloat() else inputBuffer.getShort() / 32768f
                val y = b0 * x + b1 * x1[c] + b2 * x2[c] - a1 * y1[c] - a2 * y2[c]
                x2[c] = x1[c]
                x1[c] = x
                y2[c] = y1[c]
                y1[c] = y
                val mixed = x * (1f - current) + y * current
                if (floatInput) {
                    output.putFloat(mixed)
                } else {
                    output.putShort((mixed * 32768f).coerceIn(-32768f, 32767f).toInt().toShort())
                }
            }
        }
        output.flip()
    }

    // RBJ cookbook 2nd-order Butterworth high-pass
    private fun computeHighPass(cutoffHz: Double, rate: Int) {
        val w0 = 2.0 * PI * cutoffHz / rate
        val alpha = sin(w0) / (2.0 * (1.0 / sqrt(2.0)))
        val cosW0 = cos(w0)
        val a0 = 1.0 + alpha
        b0 = (((1.0 + cosW0) / 2.0) / a0).toFloat()
        b1 = (-(1.0 + cosW0) / a0).toFloat()
        b2 = b0
        a1 = ((-2.0 * cosW0) / a0).toFloat()
        a2 = ((1.0 - alpha) / a0).toFloat()
    }

    private fun clearState() {
        if (x1.size != channelCount) {
            x1 = FloatArray(channelCount)
            x2 = FloatArray(channelCount)
            y1 = FloatArray(channelCount)
            y2 = FloatArray(channelCount)
        } else {
            x1.fill(0f)
            x2.fill(0f)
            y1.fill(0f)
            y2.fill(0f)
        }
    }

    override fun onFlush() {
        clearState()
        current = bassCut.coerceIn(0f, 1f)
    }

    override fun onReset() {
        bassCut = 0f
        current = 0f
        sampleRate = 0
        channelCount = 0
    }

    companion object {
        private const val CUTOFF_HZ = 250.0
        private const val GLIDE_SECONDS = 0.06f
    }
}
