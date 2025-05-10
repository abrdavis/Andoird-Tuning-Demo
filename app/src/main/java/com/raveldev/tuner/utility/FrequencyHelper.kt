package com.raveldev.tuner.utility

import android.util.Log
import com.raveldev.tuner.models.TunerResult
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow


class FrequencyHelper {

    val BASE_FREQUENCY_HZ = 440.0;
    val A4_SEMITONE = 69;

    val NOTE_ORDER_FREQ = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")


    val STARTING_OCTAVE = 0;
    val HIGHEST_OCTAVE = 8;
    var mapNoteToFreq = mapOf<String, Double>();
    constructor(){
        mapNoteToFreq = generateNoteFrequencies();
    }

    fun generateNoteFrequencies(): Map<String, Double> {
        val frequencies = mutableMapOf<String, Double>()

        for (octave in STARTING_OCTAVE..HIGHEST_OCTAVE) {
            for ((i, note) in NOTE_ORDER_FREQ.withIndex()) {
                val currentNoteSemitone = i + (12 * (octave+1))
                val semitoneDifference = currentNoteSemitone - A4_SEMITONE
                val powerToRaiseTo = semitoneDifference / 12.0;
                val frequency = BASE_FREQUENCY_HZ * 2.0.pow(powerToRaiseTo)
                val noteKey = "$note$octave"
                frequencies[noteKey] = String.format("%.2f", frequency).toDouble()
            }
        }

        return frequencies
    }
    fun getNoteForFrequency(pitchInHz: Float) : TunerResult{
        var isSharp = false;
        var inTune = false;
        val n = floor(12 * log2(pitchInHz / BASE_FREQUENCY_HZ)).toInt();
        var midiNumber = n + A4_SEMITONE

        val noteName = NOTE_ORDER_FREQ[midiNumber % 12]
        val octave = (midiNumber / 12)

        val fullNoteName = "$noteName}${octave}";
        if(mapNoteToFreq.containsKey(fullNoteName)) {
            val exactPitch = mapNoteToFreq[noteName]!!;
            val freqDiff = abs(pitchInHz - mapNoteToFreq[noteName]!!);

            //if diff greater than 3-5% of a semitone, then it's out of tune
            if(freqDiff > 4){
                isSharp = pitchInHz > exactPitch;
            }
            else{
                inTune = true;
            }
            Log.i("derp", freqDiff.toString());
        }
        return TunerResult(noteDisplayText = noteName, inTune = inTune, isFlat = !isSharp, isSharp = isSharp);
    }



}