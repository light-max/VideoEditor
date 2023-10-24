package com.lifengqiang.videoeditor.base

import android.app.Activity
import android.view.View
import android.widget.LinearLayout.LayoutParams
import androidx.viewbinding.ViewBinding

open class FullScreenPopupWindow<VB : ViewBinding>(activity: Activity) :
    BasePopupWindow<VB>(
        activity,
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT
    ) {
    fun showAsView(parentView: View) {
        showAsDropDown(parentView)
    }
}