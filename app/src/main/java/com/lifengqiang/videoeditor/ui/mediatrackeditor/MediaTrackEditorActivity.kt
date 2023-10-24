package com.lifengqiang.videoeditor.ui.mediatrackeditor

import android.content.Intent
import android.os.Bundle
import com.lifengqiang.videoeditor.base.BaseActivity
import com.lifengqiang.videoeditor.databinding.ActivityMediaTrackEditorBinding
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_AUDIO
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_FLAG
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_PICTURE
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_VIDEO
import com.lifengqiang.videoeditor.ui.mediaselector.MediaSelectorActivity

class MediaTrackEditorActivity :
    BaseActivity<MediaTrackEditorPresenter, ActivityMediaTrackEditorBinding>(),
    IMediaTrackEditorView {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.audio.setOnClickListener {
            startActivity(Intent(this, MediaSelectorActivity::class.java).apply {
                putExtra(MEDIA_SELECT_FLAG, MEDIA_SELECT_AUDIO)
            })
        }
        binding.video.setOnClickListener {
            startActivity(Intent(this, MediaSelectorActivity::class.java).apply {
                putExtra(MEDIA_SELECT_FLAG, MEDIA_SELECT_PICTURE or MEDIA_SELECT_VIDEO)
            })
        }
    }
}