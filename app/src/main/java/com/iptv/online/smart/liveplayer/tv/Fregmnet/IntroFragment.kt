package com.iptv.online.smart.liveplayer.tv.activities.forIntro

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import com.iptv.online.smart.liveplayer.tv.Fregmnet.BaseFragment
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentIntroBinding


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class IntroFragment : BaseFragment<FragmentIntroBinding>() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var number: Int = 1

    override fun setViewBinding() = FragmentIntroBinding.inflate(layoutInflater)
    override fun onCreate(savedInstanceState: Bundle?) {
        setheader()

        super.onCreate(savedInstanceState)

    }
    private fun setheader() {
        activity?.let { act ->
            val window = act.window

            // 1. Edge-to-Edge સેટિંગ
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }

            // 2. StatusBar ને Transparent કરો
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.statusBarColor = Color.TRANSPARENT
            }

            // 3. Status Bar ના આઈકોન્સ સેટ કરો (Light/Dark)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val decorView = window.decorView
                @Suppress("DEPRECATION")
                var flags = decorView.systemUiVisibility
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

                decorView.systemUiVisibility = flags
            }
        }
    }
    private val remoteData: RemoteConfigdata by lazy {
        RemoteConfigdata(requireActivity())
    }

    override fun bindObjects() {

        number = arguments?.getInt(ARG_NUMBER) ?: 1

        when (number) {
            1 -> {
                binding.tvTitle.text = getString(R.string.welcome_to_iptv_streamings)
                binding.btnNext.text = getString(R.string.next)


                binding.tvContent.text =
                    getString(R.string.enjoy_your_favorite_channels_with_smooth_and_easy_streaming);

                binding.imageDot.setImageResource(R.drawable.ic_inro_dot_1)
            }

            2 -> {
                binding.tvTitle.text = getString(R.string.stream_live_channels)
                binding.btnNext.text = getString(R.string.next)

                binding.tvContent.text =
                    getString(R.string.access_live_channels_instantly_with_smooth_streaming)

                binding.imageDot.setImageResource(R.drawable.ic_inro_dot_2)
            }

            3 -> {
                binding.tvTitle.text = getString(R.string.cast_your_tv)
                binding.btnNext.text = getString(R.string.next)

                binding.tvContent.text =
                    getString(R.string.connect_and_stream_your_favorite_content_on_a_larger_screen)
                binding.imageDot.setImageResource(R.drawable.ic_inro_dot_3)
            }

            4 -> {
                binding.tvTitle.text = getString(R.string.smooth_to_use)
                binding.btnNext.text = getString(R.string.get_start)
                binding.tvContent.text =
                    getString(R.string.experience_fluid_controls_and_smooth_performance_every_time)
                binding.imageDot.setImageResource(R.drawable.ic_inro_dot_4)
            }
        }

        binding.btnNext.setOnClickListener {
//            (activity as? IntroActivity)?.onNext()
        }
    }



    override fun bindListener() {
    }

    override fun bindMethod() {
    }

    companion object {
        private const val ARG_NUMBER = "ARG_NUMBER"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment IntroFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(number: Int) = IntroFragment().apply {
            arguments = Bundle().apply { putInt(ARG_NUMBER, number) }
        }
    }
}