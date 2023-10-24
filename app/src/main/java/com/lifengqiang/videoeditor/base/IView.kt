package com.lifengqiang.videoeditor.base

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment

interface IView {
    fun context(): Context
    fun activity(): Activity? = null
    fun fragment(): Fragment? = null
}