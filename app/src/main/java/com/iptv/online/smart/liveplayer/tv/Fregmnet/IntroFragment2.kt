package com.iptv.online.smart.liveplayer.tv.activities.forIntro

import android.os.Bundle
import androidx.fragment.app.Fragment

import com.iptv.online.smart.liveplayer.tv.Fregmnet.BaseFragment
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentIntro2Binding
import com.iptv.online.smart.liveplayer.tv.utils.triggerClick
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [IntroFragment2.newInstance] factory method to
 * create an instance of this fragment.
 */
class IntroFragment2 : BaseFragment<FragmentIntro2Binding>() {


    override fun setViewBinding() = FragmentIntro2Binding.inflate(layoutInflater)

    override fun bindObjects() {
    }

    override fun bindListener() {
        binding.btnNext.triggerClick {
            (activity as? IntroActivity)?.getNextFragment()
        }
    }

    override fun bindMethod() {
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment IntroFragment2.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            IntroFragment2().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}