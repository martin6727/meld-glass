package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow

@UnstableApi
class VolumeNormalizationAudioProcessor : BaseAudioProcessor() {
    private var encoding = C.ENCODING_INVALID
    private var bytesPerSample = 0

    @Volatile
    var enabled = false
        set(value) {
            if (field != value) {
                field = value
                Timber.tag(TAG).d("Normalization processor enabled: $value")
            }
        }

    private data class GainState(val targetGainMb: Int, val linearGain: Double)

    @Volatile
    private var currentGain = GainState(0, 1.0)

    @Synchronized
    fun setTargetGain(gainMb: Int) {
        if (currentGain.targetGainMb != gainMb) {
            val linearGain = 10.0.pow(gainMb / 2000.0)
            currentGain = GainState(gainMb, linearGain)
            Timber.tag(TAG).d("Target gain set to $gainMb mB (Linear multiplier: $linearGain)")
        }
    }

    /** Gain currently requested, in millibels (0 = unchanged). */
    val targetGainMb: Int
        get() = currentGain.targetGainMb

    // Gain actually being applied; glides towards the target so level changes never jump
    private var appliedLinear = Double.NaN
    private var glidePerSample = 1.0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        encoding = inputAudioFormat.encoding
        bytesPerSample =
            when (encoding) {
                C.ENCODING_PCM_16BIT -> 2
                C.ENCODING_PCM_24BIT -> 3
                C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
                else -> throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            }
        val samplesPerSecond = (inputAudioFormat.sampleRate * inputAudioFormat.channelCount).coerceAtLeast(1)
        glidePerSample = 1.0 - exp(-1.0 / (GLIDE_SECONDS * samplesPerSecond))

        Timber.tag(TAG).d(
            "Configured: sampleRate=${inputAudioFormat.sampleRate}, channels=${inputAudioFormat.channelCount}, encoding=$encoding",
        )
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val gain = currentGain
        val target = if (enabled && gain.targetGainMb != 0) gain.linearGain else 1.0
        // After a seek or a fresh start, begin at the right level straight away
        if (appliedLinear.isNaN()) appliedLinear = target
        val sampleCount = inputBuffer.remaining() / bytesPerSample
        val output = replaceOutputBuffer(sampleCount * bytesPerSample)

        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        if (appliedLinear == 1.0 && target == 1.0) {
            output.put(inputBuffer)
            output.flip()
            return
        }

        when (encoding) {
            C.ENCODING_PCM_16BIT ->
                repeat(sampleCount) {
                    val sample = inputBuffer.getShort()
                    output.putShort((sample * nextGain(target)).coerceIn(-32768.0, 32767.0).toInt().toShort())
                }

            C.ENCODING_PCM_24BIT ->
                repeat(sampleCount) {
                    val b0 = inputBuffer.get().toInt() and 0xFF
                    val b1 = inputBuffer.get().toInt() and 0xFF
                    val b2 = inputBuffer.get().toInt()
                    val sample = (b2 shl 16) or (b1 shl 8) or b0
                    val processed = (sample * nextGain(target)).coerceIn(-8388608.0, 8388607.0).toInt()
                    output.put((processed and 0xFF).toByte())
                    output.put(((processed shr 8) and 0xFF).toByte())
                    output.put(((processed shr 16) and 0xFF).toByte())
                }

            C.ENCODING_PCM_32BIT ->
                repeat(sampleCount) {
                    val sample = inputBuffer.getInt()
                    output.putInt((sample * nextGain(target)).coerceIn(-2147483648.0, 2147483647.0).toLong().toInt())
                }

            C.ENCODING_PCM_FLOAT ->
                repeat(sampleCount) {
                    val sample = inputBuffer.getFloat()
                    output.putFloat((sample * nextGain(target).toFloat()).coerceIn(-1.0f, 1.0f))
                }
        }

        output.flip()
    }

    private fun nextGain(target: Double): Double {
        val applied = appliedLinear
        if (applied != target) {
            val next = applied + (target - applied) * glidePerSample
            appliedLinear = if (abs(next - target) < 1e-4) target else next
        }
        return appliedLinear
    }

    override fun onFlush() {
        appliedLinear = Double.NaN
    }

    override fun onReset() {
        encoding = C.ENCODING_INVALID
        bytesPerSample = 0
        appliedLinear = Double.NaN
    }

    private companion object {
        const val TAG = "VolumeNormalizationProcessor"

        // Level changes settle over roughly half a second
        const val GLIDE_SECONDS = 0.15
    }
}
