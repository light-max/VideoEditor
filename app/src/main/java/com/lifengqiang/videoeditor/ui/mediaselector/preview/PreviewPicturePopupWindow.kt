package com.lifengqiang.videoeditor.ui.mediaselector.preview

import android.annotation.SuppressLint
import android.app.Activity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import com.bumptech.glide.Glide
import com.lifengqiang.videoeditor.base.FullScreenPopupWindow
import com.lifengqiang.videoeditor.databinding.DialogMediaSelectorPreviewPictureBinding
import java.io.File
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class PreviewPicturePopupWindow(activity: Activity, path: String) :
    FullScreenPopupWindow<DialogMediaSelectorPreviewPictureBinding>(activity) {
    private val sgd: ScaleGestureDetector

    private var previousX: Float = 0.0f
    private var previousY: Float = 0.0f

    init {
        Glide.with(binding.image)
            .load(File(path))
            .into(binding.image)
        binding.exit.setOnClickListener { dismiss() }
        sgd = ScaleGestureDetector(activity, object : OnScaleGestureListener {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                val matrix = binding.image.imageMatrix
                matrix.postScale(factor, factor)
                binding.image.invalidate()
                return true
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
            }
        })


        binding.image.setOnTouchListener { v, event ->
            if (event.pointerCount == 1) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        previousX = event.x
                        previousY = event.y
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val mx = event.x - previousX
                        val my = event.y - previousY
                        previousX = event.x
                        previousY = event.y
                        if (abs(mx) > 100 || abs(my) > 100) {
                        } else {
                            binding.image.apply {
                                imageMatrix.postTranslate(mx, my)
                                invalidate()
                            }
                        }
                    }
                }
                true
            } else {
                sgd.onTouchEvent(event)
            }
        }
    }
}