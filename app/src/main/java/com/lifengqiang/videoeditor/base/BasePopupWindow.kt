package com.lifengqiang.videoeditor.base

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

open class BasePopupWindow<VB : ViewBinding> : PopupWindow {
    protected var binding: VB

    constructor(activity: Activity, popupWidth: Int, popupHeight: Int) : super(activity) {
        binding = generateBinding(activity.layoutInflater)
        contentView = binding.root
        isOutsideTouchable = true
        isFocusable = true
        width = popupWidth
        height = popupHeight
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    constructor(activity: Activity) : this(
        activity,
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun generateBinding(layoutInflater: LayoutInflater): VB {
        val type = javaClass.genericSuperclass
        if (type is ParameterizedType) {
            for (vbType in type.actualTypeArguments) {
                val vbClass = vbType as Class<*>
                if (ViewBinding::class.java.isAssignableFrom(vbClass)) {
                    val inflate = vbClass.getMethod("inflate", LayoutInflater::class.java)
                    return inflate.invoke(null, layoutInflater) as VB
                }
            }
            throw RuntimeException("\n找不到ViewBinding泛型参数 $type")
        } else {
            throw RuntimeException("\n找不到ViewBinding泛型参数 $type")
        }
    }
}