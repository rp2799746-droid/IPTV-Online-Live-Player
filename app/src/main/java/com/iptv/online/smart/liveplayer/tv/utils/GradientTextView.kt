package com.iptv.online.smart.liveplayer.tv.utils

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            paint.shader = LinearGradient(
                width.toFloat(), 100f,
                100f, height.toFloat(),
                intArrayOf(
                    Color.parseColor("#51CBFF"),
                    Color.parseColor("#FF45AFFF"),
                    Color.parseColor("#FF2663FF")
                ),
                null, Shader.TileMode.CLAMP
            )
        }
    }
}