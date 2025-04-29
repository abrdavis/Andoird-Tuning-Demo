package com.raveldev.timer

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import com.raveldev.timer.databinding.FragmentTunerBinding
import com.raveldev.tuner.models.TunerResult
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.log2



/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class TunerFragment : Fragment() {
    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentTunerBinding? = null


    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentTunerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        val noteTextView : TextView = binding.textNote

        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        try{
            generateFrequencyMap()
            var recordAudioGranted = ContextCompat.checkSelfPermission(this.requireContext(), "android.permission.RECORD_AUDIO")
            if(recordAudioGranted == PackageManager.PERMISSION_GRANTED){
                val dispatcher: AudioDispatcher =
                    AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)

                val pdh = PitchDetectionHandler { result, e ->
                    try{
                        val pitchInHz = result.pitch
                        if(pitchInHz > 0) {
                            //given our pitch in hz, which value in our map of notes is closest
                            activity?.runOnUiThread(Runnable {
                                val tunerResult = getNoteForFrequency(pitchInHz);

                                noteTextView.text = tunerResult.GetDisplayText();
 								textView.text = "${pitchInHz}";
                            })
                        }


                    }
                    catch(e: Exception){
                        Log.e("debug", e.toString());
                    }

                }

                val p: AudioProcessor = PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050f, 1024, pdh)
                dispatcher.addAudioProcessor(p)
                Thread(dispatcher, "Audio Dispatcher").start()
            }
            else{

            }
        }
        catch(e: Exception){
            Log.e("tuner", e.message.toString())
        }
        return root

    }
    val BASE_FREQUENCY_HZ = 440.0;
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
    //C0 is in the "-2" octave in MIDI
    private fun generateFrequencyMap(){
        //C0(-2)
        mapNoteToFreq = generateNoteFrequencies();

    }
    fun generateNoteFrequencies(): Map<String, Double> {
        val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val baseFrequency = 440.0 // A4
        val baseIndex = 9 + 12 * 4 // A4 is the 9th note in the 4th octave

        val frequencies = mutableMapOf<String, Double>()

        for (octave in 0..8) {
            for ((i, note) in noteNames.withIndex()) {
                val noteIndex = i + 12 * octave
                val semitoneDifference = noteIndex - baseIndex
                val frequency = baseFrequency * Math.pow(2.0, semitoneDifference / 12.0)
                val noteKey = "$note$octave"
                frequencies[noteKey] = String.format("%.2f", frequency).toDouble()
            }
        }

        return frequencies
    }
    private fun getNoteForFrequency(pitchInHz: Float) : TunerResult{

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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}