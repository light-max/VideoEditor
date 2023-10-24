package com.lifengqiang.videoeditor.base

import android.os.Handler
import android.os.Looper
import android.util.AndroidException
import java.lang.ref.WeakReference
import java.lang.reflect.ParameterizedType

open class BasePresenter<M, V : IView> : IPresenter<M, V> {
    private var _view: WeakReference<V>? = null
    private val _model: M by lazy { generateModel() }
    protected val uiHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

    override fun model(): M = _model
    override fun view(): V? = _view?.get()

    override fun onAttachView(view: V) {
        _view = WeakReference(view)
    }

    override fun onDetachView() {
        _view = null
    }

    private fun generateModel(): M {
        val type = javaClass.genericSuperclass
        if (type is ParameterizedType) {
            val vbType = type.actualTypeArguments[0]
            val vbClass = vbType as Class<*>
            val constructor = vbClass.getConstructor()
            return constructor.newInstance() as M
        } else {
            throw AndroidException("$type 找不到泛型参数")
        }
    }

    fun runOnUIThread(runnable: Runnable) {
        if (Looper.getMainLooper().isCurrentThread) {
            runnable.run()
        } else {
            uiHandler.post(runnable)
        }
    }
}