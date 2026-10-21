package com.metrolist.music.playback.dj

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class TempoEstimatorTest {
    private val sampleRate = 44_100

    /** Kick on every beat, hi-hat on the off-beats, a bass tone and some noise. */
    private fun drumLoop(
        bpm: Double,
        seconds: Double,
        startDelaySeconds: Double = 0.0,
        noise: Float = 0.02f,
        seed: Int = 1,
    ): FloatArray {
        val random = Random(seed)
        val total = (seconds * sampleRate).toInt()
        val out = FloatArray(total)
        val beat = 60.0 / bpm
        for (i in 0 until total) {
            val t = i.toDouble() / sampleRate - startDelaySeconds
            var s = 0.0
            if (t >= 0) {
                val sinceBeat = t % beat
                val sinceOffBeat = (t + beat / 2) % beat
                // Kick: decaying 55 Hz thump
                s += 0.8 * exp(-sinceBeat * 18) * sin(2 * PI * 55 * sinceBeat)
                // Hi-hat: short noise burst
                s += 0.25 * exp(-sinceOffBeat * 120) * (random.nextFloat() * 2 - 1)
                // Sustained bass so the signal isn't just clicks
                s += 0.15 * sin(2 * PI * 82.4 * t)
            }
            out[i] = (s + noise * (random.nextFloat() * 2 - 1)).toFloat()
        }
        return out
    }

    private fun analyse(samples: FloatArray): Pair<TempoEstimate?, Pair<FloatArray, OnsetDetector>> {
        val detector = OnsetDetector(sampleRate)
        val envelope = ArrayList<Float>()
        val db = ArrayList<Float>()
        for (s in samples) {
            val onset = detector.push(s)
            if (!onset.isNaN()) {
                envelope.add(onset)
                db.add(detector.lastHopDb)
            }
        }
        return TempoEstimator.estimate(envelope.toFloatArray(), detector.hopSeconds) to (db.toFloatArray() to detector)
    }

    @Test
    fun findsCommonDanceTempos() {
        for (bpm in listOf(90.0, 100.0, 120.0, 124.0, 128.0, 140.0, 150.0)) {
            val (estimate, _) = analyse(drumLoop(bpm, 16.0, seed = bpm.toInt()))
            assertNotNull("no estimate for $bpm", estimate)
            assertEquals("tempo for $bpm BPM", bpm, estimate!!.bpm, 1.0)
            assertTrue("confidence for $bpm BPM was ${estimate.confidence}", estimate.confidence >= TempoEstimator.MIN_CONFIDENCE)
        }
    }

    @Test
    fun fastTemposMayFoldToHalfButStillMatch() {
        val (estimate, _) = analyse(drumLoop(174.0, 16.0))
        assertNotNull(estimate)
        // 174 or its half (87) both mix correctly once folded by tempoMatchRatio
        val folded = tempoMatchRatio(estimate!!.bpm, 174.0)
        assertEquals(1.0, folded, 0.01)
    }

    @Test
    fun beatPhaseLandsOnTheKick() {
        val cases = listOf(120.0 to 0.02f, 128.0 to 0.0005f, 100.0 to 0.0005f, 140.0 to 0.01f, 92.0 to 0.002f)
        for ((bpm, noise) in cases) {
            val delay = 0.13 // first kick at 130 ms
            val (estimate, extra) = analyse(drumLoop(bpm, 16.0, startDelaySeconds = delay, noise = noise, seed = bpm.toInt()))
            assertNotNull(estimate)
            val hop = extra.second.hopSeconds
            val period = 60.0 / bpm
            val phaseSeconds = estimate!!.phaseHops * hop
            // Distance from the found phase to the nearest real kick, wrapped into one beat
            val offset = ((phaseSeconds - delay) % period + period) % period
            val error = minOf(offset, period - offset)
            assertTrue("$bpm BPM (noise $noise): phase off by ${error * 1000} ms", error < 0.03)
        }
    }

    @Test
    fun firstBeatSkipsLeadingSilence() {
        val bpm = 128.0
        val delay = 1.5 // 1.5 s of silence before the music starts
        val (estimate, extra) = analyse(drumLoop(bpm, 16.0, startDelaySeconds = delay, noise = 0.0005f))
        assertNotNull(estimate)
        val first = TempoEstimator.firstBeatHop(estimate!!, extra.first)
        assertNotNull(first)
        val firstSeconds = first!! * extra.second.hopSeconds
        assertEquals(delay, firstSeconds, 0.04)
    }

    @Test
    fun noiseHasLowConfidence() {
        val random = Random(7)
        val noise = FloatArray(sampleRate * 16) { (random.nextFloat() * 2 - 1) * 0.3f }
        val (estimate, _) = analyse(noise)
        if (estimate != null) {
            assertTrue("noise confidence ${estimate.confidence}", estimate.confidence < TempoEstimator.MIN_CONFIDENCE)
        }
    }

    @Test
    fun tooShortReturnsNull() {
        val (estimate, _) = analyse(drumLoop(120.0, 3.0))
        assertNull(estimate)
    }

    @Test
    fun tempoMatchRatioFoldsOctaves() {
        assertEquals(1.0, tempoMatchRatio(87.0, 174.0), 1e-9)
        assertEquals(128.0 / 124.0, tempoMatchRatio(124.0, 128.0), 1e-9)
        assertTrue(abs(tempoMatchRatio(140.0, 70.0) - 1.0) < 1e-9)
    }
}
