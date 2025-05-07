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
import com.raveldev.tuner.utility.FrequencyHelper


class TunerFragment : Fragment() {
    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentTunerBinding? = null
    private val frequencyHelper : FrequencyHelper =  FrequencyHelper()

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
                                val tunerResult = frequencyHelper.getNoteForFrequency(pitchInHz);

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