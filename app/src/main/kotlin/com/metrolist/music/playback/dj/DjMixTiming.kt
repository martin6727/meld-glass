package com.metrolist.music.playback.dj

/** Tuning for DJ Mix transitions. */
object DjMixTiming {
    /** Overlap when the tempos match (song time of the outgoing track). */
    const val MIX_MS = 12_000L

    /** Shorter blend used when the tempos are too far apart to beat-match. */
    const val BLEND_MS = 7_000L

    /** Start loading and analysing the next song this long before the mix. */
    const val PREPARE_LEAD_MS = 25_000L

    /** Songs shorter than this are never mixed. */
    const val MIN_TRACK_MS = 45_000L

    /** The next song's intro is skimmed muted at this speed while measuring its tempo. */
    const val ANALYSIS_SPEED = 2f

    /** Song seconds of the next song's intro to analyse. */
    const val ANALYSIS_SECONDS = 14.0

    /** Give up waiting for the analysis after this long (wall clock). */
    const val ANALYSIS_TIMEOUT_MS = 15_000L

    /** Never skip more than this much of an intro looking for the first beat. */
    const val MAX_INTRO_SKIP_SECONDS = 8.0

    /** Largest tempo change applied to the incoming song (8 %), like a DJ's pitch fader. */
    const val MAX_TEMPO_STRETCH = 0.08

    /**
     * Audio passes the analyser this long before it reaches the speaker (the player's audio
     * buffer). Used to start the next song on a beat you actually hear.
     */
    const val OUTPUT_LEAD_MS = 250L

    /** Automation update interval during a mix. */
    const val TICK_MS = 40L

    /** How often the beat lock nudges the incoming song's speed. */
    const val NUDGE_EVERY_MS = 250L

    /** Speed correction per beat of phase error. */
    const val PHASE_GAIN = 0.35

    /** Largest extra speed nudge used to pull the beats together (3 %). */
    const val MAX_NUDGE = 0.03

    /** After a mix, the new song glides back to its own tempo over this long. */
    const val TEMPO_RESTORE_MS = 20_000L
    const val TEMPO_STEP_MS = 200L
}
