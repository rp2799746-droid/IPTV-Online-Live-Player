package com.iptv.online.smart.liveplayer.tv.utils

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

/**
 * NestedScrollView no jaanito Android framework bug fix.
 *
 * Crash: java.lang.IllegalArgumentException "parameter must be a descendant of this view"
 *   ViewGroup.offsetRectBetweenParentAndChild
 *   NestedScrollView.isWithinDeltaOfScreen
 *   NestedScrollView.onSizeChanged
 *
 * Karan: onSizeChanged ma NestedScrollView findFocus() child on-screen chhe ke nahi e
 * ganva offsetDescendantRectToMyCoords() kare chhe. Pan e focused child layout daramiyan
 * detach/replace thai gayu hoy (dr. RecyclerView/ViewPager2 item re-layout) to e child
 * NestedScrollView no descendant nathi rehto -> exception. Aa framework nu bug chhe,
 * etle super call ne guard kariye chhie. Behavior same rahe chhe, fakt aa crash aatakay.
 */
class SafeNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        try {
            super.onSizeChanged(w, h, oldw, oldh)
        } catch (e: IllegalArgumentException) {
            // NestedScrollView framework bug -> ignore, crash aatakavo.
        }
    }
}
