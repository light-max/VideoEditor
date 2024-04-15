package com.lifengqiang.videoeditor.ui.mediaselector.preview

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.core.view.isVisible
import com.lifengqiang.videoeditor.R
import com.lifengqiang.videoeditor.base.FullScreenPopupWindow
import com.lifengqiang.videoeditor.databinding.DialogMediaSelectorPreviewVideoBinding
import com.lifengqiang.videoeditor.model.IVideoPlayModel
import com.lifengqiang.videoeditor.model.VideoPlayModel
import com.lifengqiang.videoeditor.multiscreenrender.MultiSurfaceRenderer
import com.lifengqiang.videoeditor.utils.timeTranslateMMSS

class PreviewVideoPopupWindow(activity: Activity, val path: String) :
    FullScreenPopupWindow<DialogMediaSelectorPreviewVideoBinding>(activity),
    IVideoPlayModel.Callback {
    private val handler = Handler(Looper.getMainLooper())
    private val renderer = MultiSurfaceRenderer()
    private val player: IVideoPlayModel = VideoPlayModel(path)

    init {
        binding.apply {
            root.setOnClickListener {
                if (group.isVisible) {
                    group.visibility = View.GONE
                } else {
                    group.visibility = View.VISIBLE
                }
            }
            progress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    player.pause()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    player.seekTo(seekBar.progress)
                    player.resume()
                }
            })
            playstate.setOnClickListener { player.toggle() }
            back.setOnClickListener { dismiss() }
            setOnDismissListener { player.release() }
        }

        player.setCallback(this)
        if (player.prepare(renderer.inputSurface)) {
            player.resume()
        } else {
            Toast.makeText(activity, "无法播放", Toast.LENGTH_SHORT).show()
        }

        val surfaces = listOf(binding.surface0, binding.surface1, binding.surface2)
        for (surface in surfaces) {
            surface.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    renderer.addSurface(holder.surface)
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    renderer.setSurfaceSize(holder.surface, width, height)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    renderer.removeSurface(holder.surface)
                }
            })
        }
    }

    override fun onPrepared(durationSecond: Int, videoWidth: Int, videoHeight: Int, rotation: Int) {
        renderer.setInputSurfaceSize(videoWidth, videoHeight)
        handler.post {
            binding.apply {
                durationText.text = timeTranslateMMSS(durationSecond)
                progress.max = durationSecond
            }
        }
    }

    override fun onProgress(progressSecond: Int) {
        handler.post {
            binding.apply {
                progressText.text = timeTranslateMMSS(progressSecond)
                progress.progress = progressSecond
            }
        }
    }

    override fun onPause() {
        handler.post { binding.playstate.setImageResource(R.drawable.baseline_pause_circle_filled_24) }
    }

    override fun onResume() {
        handler.post { binding.playstate.setImageResource(R.drawable.baseline_play_circle_24) }
    }
}