package com.lifengqiang.videoeditor.ui.mediaselector.preview

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.lifengqiang.videoeditor.R
import com.lifengqiang.videoeditor.base.FullScreenPopupWindow
import com.lifengqiang.videoeditor.databinding.DialogMediaSelectorPreviewAudioBinding
import com.lifengqiang.videoeditor.model.AudioPlayModel
import com.lifengqiang.videoeditor.model.IAudioPlayModel
import com.lifengqiang.videoeditor.model.IMediaSelectorModel
import com.lifengqiang.videoeditor.utils.timeTranslateMMSS
import java.io.File

class PreviewAudioPopupWindow(activity: Activity) :
    FullScreenPopupWindow<DialogMediaSelectorPreviewAudioBinding>(activity),
    IAudioPlayModel.Callback {

    private var model: IAudioPlayModel? = null
    private val uiHandler: Handler = Handler(Looper.getMainLooper())

    init {
        setOnDismissListener { model?.release() }
        binding.close.setOnClickListener { dismiss() }
    }

    fun setAudioMedia(
        media: IMediaSelectorModel.MediaData,
        confirmCallback: (IMediaSelectorModel.MediaData) -> Unit
    ): PreviewAudioPopupWindow {
        model = AudioPlayModel(media.path)
        model?.setCallback(this)
        if (model?.prepare() == true) {
            model?.resume()
        }
        binding.apply {
            filename.text = File(media.path).name
            playstate.setOnClickListener { model?.toggle() }
            progress.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        progressText.text = timeTranslateMMSS(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    model?.pause()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    model?.seekTo(seekBar.progress)
                    model?.resume()
                }
            })
            confirm.setOnClickListener {
                dismiss()
                confirmCallback(media)
            }
        }
        return this
    }

    override fun onPrepared(durationSecond: Int) {
        runOnUIThread {
            binding.apply {
                progress.max = durationSecond
                durationText.text = timeTranslateMMSS(durationSecond)
            }
        }
    }

    override fun onProgress(durationSecond: Int, progressSecond: Int) {
        runOnUIThread {
            binding.apply {
                progress.progress = progressSecond
                progressText.text = timeTranslateMMSS(progressSecond)
            }
        }
    }

    override fun onPause() {
        runOnUIThread {
            binding.playstate.setImageResource(R.drawable.baseline_play_circle_24)
        }
    }

    override fun onResume() {
        runOnUIThread {
            binding.playstate.setImageResource(R.drawable.baseline_pause_circle_filled_24)
        }
    }

    private fun runOnUIThread(runnable: () -> Unit) {
        if (Looper.getMainLooper().isCurrentThread) {
            runnable()
        } else {
            uiHandler.post(runnable)
        }
    }
}