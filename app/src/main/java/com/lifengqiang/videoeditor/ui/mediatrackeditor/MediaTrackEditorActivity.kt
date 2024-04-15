package com.lifengqiang.videoeditor.ui.mediatrackeditor

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import com.lifengqiang.videoeditor.base.BaseActivity
import com.lifengqiang.videoeditor.data.MediaSelectorResult
import com.lifengqiang.videoeditor.databinding.ActivityMediaTrackEditorBinding
import com.lifengqiang.videoeditor.multiscreenrender.MultiSurfaceRenderer
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_AUDIO
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_FLAG
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_PICTURE
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_RESULT_KEY
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_VIDEO
import com.lifengqiang.videoeditor.ui.mediaselector.MediaSelectorActivity
import com.lifengqiang.videoeditor.ui.mediatrackeditor.video.MultiSectionVideoPlayerControllerView
import com.lifengqiang.videoeditor.ui.mediatrackeditor.video.MultiVideoPlayer
import com.lifengqiang.videoeditor.ui.mediatrackeditor.video.VideoSectionData
import com.lifengqiang.videoeditor.ui.mediatrackeditor.video.videosection.VideoSectionExtractor
import com.lifengqiang.videoeditor.utils.tryingGetMediaPermission

class MediaTrackEditorActivity :
    BaseActivity<MediaTrackEditorPresenter, ActivityMediaTrackEditorBinding>(),
    IMediaTrackEditorView {
    private val player by lazy { MultiVideoPlayer() }
    private val render by lazy { MultiSurfaceRenderer() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.image.setOnClickListener {
            if (tryingGetMediaPermission(this)) {
                startActivityForResult(Intent(this, MediaSelectorActivity::class.java).apply {
                    putExtra(MEDIA_SELECT_FLAG, MEDIA_SELECT_PICTURE)
                }, 0x100)
            }
        }
        binding.audio.setOnClickListener {
            if (tryingGetMediaPermission(this)) {
                startActivityForResult(Intent(this, MediaSelectorActivity::class.java).apply {
                    putExtra(MEDIA_SELECT_FLAG, MEDIA_SELECT_AUDIO)
                }, 0x100)
            }
        }
        binding.video.setOnClickListener {
            if (tryingGetMediaPermission(this)) {
                startActivityForResult(Intent(this, MediaSelectorActivity::class.java).apply {
                    putExtra(MEDIA_SELECT_FLAG, MEDIA_SELECT_VIDEO)
                }, 0x100)
            }
        }
        binding.root.setOnClickListener { player.toggle() }

        binding.controller.setCallback(object : MultiSectionVideoPlayerControllerView.Callback {
            override fun onFrameByFrameStateChanged(flag: Boolean) {
                if (flag) {
                    player.startFrameByFrame()
                } else {
                    player.stopFrameByFrame()
                }
            }

            override fun onFrameByFrameSeek(position: Int, ratio: Float) {
                player.setProgress(position, ratio)
            }
        })

        binding.surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                render.addSurface(holder.surface)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                render.setSurfaceSize(holder.surface, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                render.removeSurface(holder.surface)
            }
        })

        VideoSectionExtractor.setOutputPath(externalCacheDir)

        player.setInputSurface(render.inputSurface)
        player.registerListener(onPlayListener)
        player.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.unregisterListener(onPlayListener)
        player.stop()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x100 && data != null) {
            val list = data.getSerializableExtra(MEDIA_SELECT_RESULT_KEY)
            val mediaList: ArrayList<MediaSelectorResult> = list as ArrayList<MediaSelectorResult>
            for (result in mediaList) {
                if (result.type == MEDIA_SELECT_VIDEO) {
                    VideoSectionExtractor().postVideo(result.path) {
                        player.addVideo(it)
                        binding.controller.addVideoSection(it)
                    }
                }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private val onPlayListener: MultiVideoPlayer.OnPlayListener =
        object : MultiVideoPlayer.OnPlayListener {
            override fun onPrepared(data: VideoSectionData) {
                render.setInputSurfaceSize(data.videoWidth, data.videoHeight)
            }

            override fun onPlay() {
            }

            override fun onPause() {
            }

            override fun onProgress(
                player: MultiVideoPlayer?,
                sumProgressRatio: Float,
                max: Int,
                progress: Int
            ) {
                handler.post { binding.controller.setProgress(sumProgressRatio) }
            }
        }
}