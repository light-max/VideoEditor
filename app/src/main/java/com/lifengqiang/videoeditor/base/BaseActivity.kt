package com.lifengqiang.videoeditor.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AndroidException
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

open class BaseActivity<P : IPresenter<*, *>, VB : ViewBinding> : ViewBindingActivity<VB>(), IView {
    protected val presenter: P = generatePresenter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.onAttachView(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDetachView()
    }

    private fun generatePresenter(): P {
        val type = javaClass.genericSuperclass
        if (type is ParameterizedType) {
            val vbType = type.actualTypeArguments[0]
            val vbClass = vbType as Class<*>
            val constructor = vbClass.getConstructor()
            return constructor.newInstance() as P
        } else {
            throw AndroidException("$type 找不到泛型参数")
        }
    }

    override fun context(): Context {
        return this
    }

    override fun activity(): Activity? {
        return this
    }
}