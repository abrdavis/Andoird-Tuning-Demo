package com.raveldev.tuner.utility

import android.util.Log
import com.raveldev.tuner.models.TunerResult
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.text.indexOf
import kotlin.times

class FrequencyHelper {

    val BASE_FREQUENCY_HZ = 440.0;
    val A4_SEMITONE = 69;
    val TWELTH_ROOT_TWO = 1.059463094359
    val BASE_NOTE_STR = "A4"
    //A4
    val BASE_NOTE = 81
    val STARTING_NOTE = 0;
    val HIGHEST_NOTE = 127
    val NOTE_ORDER = listOf("A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#")
    val NOTE_ORDER_FREQ = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    var FREQUENCY_LIST  = mutableListOf<Float>()
    val MAX_NOTE_ORDER = 11;
    val STARTING_OCTAVE = 0;
    val HIGHEST_OCTAVE = 8;
    var arrayIndextoNote = mutableMapOf<Int, String>()
    var mapNoteToFreq = mapOf<String, Double>();
    constructor(){
        mapNoteToFreq = generateNoteFrequencies();
    }
    //C0 is in the "-2" octave in MIDI

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

        val semitones = 12 * log2(pitchInHz / BASE_FREQUENCY_HZ)

        val noteIndex = ((semitones % 12) + 12) % 12  // Ensure positive index

        val octave = (semitones / 12).toInt() + 4 // Relative to A4
        var isSharp : Boolean = false;
        var inTune: Boolean = false;
        val noteName = "${NOTE_ORDER[noteIndex.toInt()]}${octave}";
        if(mapNoteToFreq.containsKey(noteName)) {
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
    private fun getClosestNoteString(
        start: Int,
        end: Int,
        myNumber: Float
    ): String {
        val mid = (start + end) / 2
        if (FREQUENCY_LIST[mid] == myNumber) return arrayIndextoNote[mid]!!
        if (start == end - 1) return if (abs(FREQUENCY_LIST[end] - myNumber) >= abs(
                FREQUENCY_LIST[start] - myNumber
            )
        ) arrayIndextoNote[start]!! else arrayIndextoNote[end]!!
        return if (FREQUENCY_LIST[mid] > myNumber) getClosestNoteString(
            start,
            mid,
            myNumber
        ) else getClosestNoteString(mid, end, myNumber)
    }

    private fun generateFrequencyForNoteNumber(note :String ) : Double {
        val halfSteps = NOTE_ORDER_FREQ.indexOf(note) + 12 * (note.substring(0, 1).uppercase().indexOf('4')) // Find the note's position in the chromatic scale
        return (BASE_FREQUENCY_HZ * 2.0) .pow(halfSteps / 12.0)
    }

}