package com.iptv.online.smart.liveplayer.tv.utils

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView


class SafeNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        try {
            super.onSizeChanged(w, h, oldw, oldh)
        } catch (e: IllegalArgumentException) {
        }
    }
}
