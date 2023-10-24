package com.lifengqiang.videoeditor.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

open class ViewBindingActivity<VB : ViewBinding> : AppCompatActivity() {
    protected val binding: VB by lazy { generateBinding() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    private fun generateBinding(): VB {
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