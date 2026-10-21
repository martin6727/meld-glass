package com.metrolist.music.playback.dj

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Turns mono PCM into an onset-strength envelope: one value per ~10 ms hop that jumps
 * whenever a kick, snare or other transient starts. Beats show up as regular peaks.
 */
class OnsetDetector(sampleRate: Int, hopSeconds: Double = DEFAULT_HOP_SECONDS) {
    val hopFrames: Int = max(1, (sampleRate * hopSeconds).roundToInt())
    val hopSeconds: Double = hopFrames.toDouble() / sampleRate

    // Two one-pole low-passes in a row isolate the kick/bass band
    private val lowAlpha = (1.0 - exp(-2.0 * PI * KICK_BAND_HZ / sampleRate)).toFloat()
    private var low = 0f
    private var low2 = 0f
    private var previous = 0f
    private var lowEnergy = 0.0
    private var transientEnergy = 0.0
    private var totalEnergy = 0.0
    private var framesInHop = 0
    private var previousLowLog = 0.0
    private var previousTransientLog = 0.0
    private var primed = false

    /** Loudness of the last completed hop in dBFS. */
    var lastHopDb: Float = -120f
        private set

    /** Feeds one mono sample in -1..1. Returns the onset strength when a hop completes, otherwise NaN. */
    fun push(sample: Float): Float {
        low += lowAlpha * (sample - low)
        low2 += lowAlpha * (low - low2)
        val diff = sample - previous // first difference emphasises clicks and hi-hats
        previous = sample
        lowEnergy += (low2 * low2).toDouble()
        transientEnergy += (diff * diff).toDouble()
        totalEnergy += (sample * sample).toDouble()
        if (++framesInHop < hopFrames) return Float.NaN

        val lowLog = ln(LOW_FLOOR + lowEnergy / hopFrames)
        val transientLog = ln(TRANSIENT_FLOOR + transientEnergy / hopFrames)
        // Kicks carry the beat; hi-hats only help, so they can't pull the grid onto the off-beat
        val onset =
            if (primed) {
                (max(0.0, lowLog - previousLowLog) * 0.8 + max(0.0, transientLog - previousTransientLog) * 0.2).toFloat()
            } else {
                0f
            }
        val meanSquare = totalEnergy / hopFrames
        lastHopDb = if (meanSquare > 0) (10.0 * kotlin.math.log10(meanSquare)).toFloat() else -120f
        primed = true
        previousLowLog = lowLog
        previousTransientLog = transientLog
        lowEnergy = 0.0
        transientEnergy = 0.0
        totalEnergy = 0.0
        framesInHop = 0
        return onset
    }

    fun reset() {
        low = 0f
        low2 = 0f
        previous = 0f
        lowEnergy = 0.0
        transientEnergy = 0.0
        totalEnergy = 0.0
        framesInHop = 0
        previousLowLog = 0.0
        previousTransientLog = 0.0
        primed = false
        lastHopDb = -120f
    }

    companion object {
        const val DEFAULT_HOP_SECONDS = 0.01
        private const val KICK_BAND_HZ = 150.0

        // Energy floors (about -40 / -30 dBFS): quieter detail is ignored so near-silence
        // doesn't turn every tiny hi-hat into a huge jump
        private const val LOW_FLOOR = 1e-4
        private const val TRANSIENT_FLOOR = 1e-3
    }
}

/**
 * Tempo and beat grid found in an onset envelope.
 * Beats fall at [phaseHops] + k * [periodHops] (in hops from the start of the analysed window).
 */
data class TempoEstimate(
    val bpm: Double,
    val confidence: Double,
    val periodHops: Double,
    val phaseHops: Double,
)

object TempoEstimator {
    /** Below this, the beat is too weak or irregular to tempo-match on. */
    const val MIN_CONFIDENCE = 0.12

    fun estimate(
        envelope: FloatArray,
        hopSeconds: Double,
        minBpm: Double = 70.0,
        maxBpm: Double = 180.0,
    ): TempoEstimate? {
        val n = envelope.size
        if (n * hopSeconds < 6.0) return null

        val x = emphasizePeaks(envelope, max(1, (0.1 / hopSeconds).roundToInt()))
        val zeroLag = autocorrelation(x, 0)
        if (zeroLag <= 1e-12) return null

        val lagMin = max(2, floor(60.0 / (maxBpm * hopSeconds)).toInt())
        val lagMax = min(n / 3, ceil(60.0 / (minBpm * hopSeconds)).toInt())
        if (lagMax <= lagMin + 2) return null

        val ac = DoubleArray(lagMax + 2)
        for (lag in (lagMin - 1)..(lagMax + 1)) ac[lag] = autocorrelation(x, lag)

        // Score each period by its own correlation plus the bar-level echo at twice the
        // period, weighted gently towards ~120 BPM to settle half/double-tempo ties.
        var bestLag = -1
        var bestScore = Double.NEGATIVE_INFINITY
        for (lag in lagMin..lagMax) {
            val doubled = if (2 * lag < n / 2) autocorrelation(x, 2 * lag) else 0.0
            val bpm = 60.0 / (lag * hopSeconds)
            val prior = exp(-0.5 * (log2(bpm / 120.0) / 0.9).let { it * it })
            val score = (ac[lag] + 0.5 * doubled) * prior
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }
        if (bestLag < 0) return null

        val coarsePeriod = refine(ac, bestLag, lagMin, lagMax)
        val confidence = (ac[bestLag] / zeroLag).coerceIn(0.0, 1.0)
        // A tiny tempo error drifts the grid over 16 s, so fit tempo and phase together
        val (period, phase) = fitGrid(smooth(x), coarsePeriod)
        return TempoEstimate(
            bpm = 60.0 / (period * hopSeconds),
            confidence = confidence,
            periodHops = period,
            phaseHops = phase,
        )
    }

    /**
     * First beat of [estimate]'s grid where the music has actually started (louder than
     * [soundThresholdDb]), in hops from the start of [envelope]. Null if none is found.
     */
    fun firstBeatHop(
        estimate: TempoEstimate,
        hopDb: FloatArray,
        soundThresholdDb: Float = -45f,
    ): Double? {
        val firstSound = hopDb.indexOfFirst { it > soundThresholdDb }
        if (firstSound < 0) return null
        var beat = estimate.phaseHops
        // Step the grid back to the start of the window, then forward to the first beat with sound
        while (beat - estimate.periodHops >= 0) beat -= estimate.periodHops
        val tolerance = estimate.periodHops * 0.25
        while (beat < hopDb.size) {
            if (beat + tolerance >= firstSound) return beat
            beat += estimate.periodHops
        }
        return null
    }

    // Keeps only the parts of the envelope that stick out above their neighbourhood
    private fun emphasizePeaks(envelope: FloatArray, halfWindow: Int): DoubleArray {
        val n = envelope.size
        val prefix = DoubleArray(n + 1)
        for (i in 0 until n) prefix[i + 1] = prefix[i] + envelope[i]
        val out = DoubleArray(n)
        var sum = 0.0
        for (i in 0 until n) {
            val from = max(0, i - halfWindow)
            val to = min(n, i + halfWindow + 1)
            val localMean = (prefix[to] - prefix[from]) / (to - from)
            out[i] = max(0.0, envelope[i] - localMean)
            sum += out[i]
        }
        // Normalise so loud and quiet tracks score alike
        val mean = sum / n
        var variance = 0.0
        for (v in out) variance += (v - mean) * (v - mean)
        val std = sqrt(variance / n)
        if (std > 1e-12) for (i in 0 until n) out[i] = (out[i] - mean) / std
        return out
    }

    private fun autocorrelation(x: DoubleArray, lag: Int): Double {
        val count = x.size - lag
        if (count <= 0) return 0.0
        var sum = 0.0
        for (i in 0 until count) sum += x[i] * x[i + lag]
        return sum / count
    }

    // Parabolic interpolation around the best lag for sub-hop tempo precision
    private fun refine(ac: DoubleArray, lag: Int, lagMin: Int, lagMax: Int): Double {
        if (lag <= lagMin || lag >= lagMax) return lag.toDouble()
        val y0 = ac[lag - 1]
        val y1 = ac[lag]
        val y2 = ac[lag + 1]
        val denominator = y0 - 2 * y1 + y2
        if (abs(denominator) < 1e-12) return lag.toDouble()
        val delta = (0.5 * (y0 - y2) / denominator).coerceIn(-0.5, 0.5)
        return lag + delta
    }

    /**
     * Slides a comb across the envelope, trying periods within ±1.5 % of [coarsePeriod] and every
     * phase; returns the (period, phase) whose teeth land on the most onset energy.
     */
    private fun fitGrid(x: DoubleArray, coarsePeriod: Double): Pair<Double, Double> {
        var bestPeriod = coarsePeriod
        var bestPhase = 0.0
        var bestSum = Double.NEGATIVE_INFINITY
        var period = coarsePeriod * (1 - GRID_PERIOD_RANGE)
        val maxPeriod = coarsePeriod * (1 + GRID_PERIOD_RANGE)
        while (period <= maxPeriod) {
            var offset = 0.0
            while (offset < period) {
                val sum = combSum(x, period, offset)
                if (sum > bestSum) {
                    bestSum = sum
                    bestPeriod = period
                    bestPhase = offset
                }
                offset += 0.5
            }
            period += 0.05
        }
        return bestPeriod to bestPhase
    }

    private fun combSum(
        x: DoubleArray,
        period: Double,
        offset: Double,
    ): Double {
        var sum = 0.0
        var position = offset
        while (position < x.size - 1) {
            val i = position.toInt()
            val frac = position - i
            // Recent beats count a little more: the grid is extrapolated forward from here
            val weight = 1.0 + 0.5 * position / x.size
            sum += weight * (x[i] * (1 - frac) + x[i + 1] * frac)
            position += period
        }
        return sum
    }

    // Light 3-tap blur so the comb still scores a beat that lands half a hop away
    private fun smooth(x: DoubleArray): DoubleArray {
        val out = DoubleArray(x.size)
        for (i in x.indices) {
            val left = if (i > 0) x[i - 1] else x[i]
            val right = if (i < x.size - 1) x[i + 1] else x[i]
            out[i] = 0.25 * left + 0.5 * x[i] + 0.25 * right
        }
        return out
    }

    private const val GRID_PERIOD_RANGE = 0.015
}

/** Speed ratio that brings [fromBpm] to [toBpm], allowing half/double-tempo matches. */
fun tempoMatchRatio(fromBpm: Double, toBpm: Double): Double {
    var ratio = toBpm / fromBpm
    while (ratio >= 1.5) ratio /= 2
    while (ratio < 0.75) ratio *= 2
    return ratio
}
