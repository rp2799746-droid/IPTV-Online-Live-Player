package com.iptv.online.smart.liveplayer.tv.Fregmnet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<actBinding : ViewBinding> : Fragment() {

    lateinit var binding: actBinding
    lateinit var mActivity: FragmentActivity
    lateinit var TAG: String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as FragmentActivity
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = setViewBinding()

        TAG = "_${this::class.simpleName}"

        if (activity != null)
            mActivity = activity as FragmentActivity

        bindObjects()
        bindListener()
        bindMethod()
        bindObserver()
        bindAds()

        return binding.root
    }

    abstract fun setViewBinding(): actBinding
    abstract fun bindObjects()
    abstract fun bindListener()
    abstract fun bindMethod()

    open fun bindObserver() {
    }

    open fun bindAds() {
    }
}