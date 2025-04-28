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
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.log2
import kotlin.math.round
import kotlin.math.roundToInt


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
                            val closetNote = getClosestNoteString(0, FREQUENCY_LIST.size -1, result.pitch);
                            //getNoteForFrequency(pitchInHz)
                            //given our pitch in hz, which value in our map of notes is closest
                            activity?.runOnUiThread(Runnable {
                                noteTextView.text = getNoteForFrequency(pitchInHz)
 								textView.text = "${pitchInHz}"
                            })
                        }


                    }
                    catch(e: Exception){

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
    var FREQUENCY_LIST  = mutableListOf<Float>()
    val MAX_NOTE_ORDER = 11;
    val STARTING_OCTAVE = -2;
    var arrayIndextoNote = mutableMapOf<Int, String>()
    //C0 is in the "-2" octave in MIDI
    private fun generateFrequencyMap(){
        //C0(-2)
        var noteOrderIndex = 0
        var startingNote = 0
        var currentOctave = STARTING_OCTAVE
        while(startingNote <= HIGHEST_NOTE){
            val arrayIndex = "${NOTE_ORDER[noteOrderIndex]}${currentOctave}"
            var noteValue =  generateFrequencyForNoteNumber(startingNote)
            arrayIndextoNote[startingNote] =arrayIndex
            FREQUENCY_LIST.add(noteValue.toFloat())
            startingNote += 1
            noteOrderIndex +=1
            if(noteOrderIndex == 12){
                noteOrderIndex = 0;
                currentOctave += 1;
            }

        }

    }

    private fun getNoteForFrequency(frequency: Float) : String{

        val semitones = 12 * log2(frequency / BASE_FREQUENCY_HZ)

        val noteIndex = ((semitones % 12) + 12) % 12  // Ensure positive index

        val octave = (semitones / 12).toInt() + 4 // Relative to A4

        return "${NOTE_ORDER[noteIndex.toInt()]} ${octave}"


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

    private fun generateFrequencyForNoteNumber(noteNumber :Int ) : Double {
        var result = 0.0
        val powerToRaise = (noteNumber - BASE_NOTE)
        result = BASE_FREQUENCY_HZ * (TWELTH_ROOT_TWO.pow(powerToRaise))

        return result
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