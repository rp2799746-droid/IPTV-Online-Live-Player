package com.iptv.online.smart.liveplayer.tv.Activity

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityNoticeBinding

class ActivityNotice : Base__Activity<ActivityNoticeBinding>() {


    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun setViewBinding(): ActivityNoticeBinding {
        return ActivityNoticeBinding.inflate(getLayoutInflater())
    }

    override fun bindObjects() {


    }

    override fun bindListener() {
        binding.btnUnderstand.setOnClickListener {
            val intent = Intent(this@ActivityNotice, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun bindMethod() {
    }
}