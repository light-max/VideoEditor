package com.lifengqiang.videoeditor.ui.mediatrackeditor

import android.content.Intent
import android.media.MediaExtractor
import android.os.Bundle
import com.lifengqiang.videoeditor.base.BaseActivity
import com.lifengqiang.videoeditor.databinding.ActivityMediaTrackEditorBinding
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_AUDIO
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_FLAG
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_PICTURE
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_RESULT_KEY
import com.lifengqiang.videoeditor.ui.mediaselector.MEDIA_SELECT_VIDEO
import com.lifengqiang.videoeditor.ui.mediaselector.MediaSelectorActivity

class MediaTrackEditorActivity :
    BaseActivity<MediaTrackEditorPresenter, ActivityMediaTrackEditorBinding>(),
    IMediaTrackEditorView {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.audio.setOnClickListener {
            startActivityForResult(Intent(this, MediaSelectorActivity::class.java).apply {
                putExtra(MEDIA_SELECT_FLAG, MEDIA_SELECT_AUDIO)
            }, 0x100)
        }
        binding.video.setOnClickListener {
            startActivityForResult(Intent(this, MediaSelectorActivity::class.java).apply {
                putExtra(MEDIA_SELECT_FLAG, MEDIA_SELECT_PICTURE or MEDIA_SELECT_VIDEO)
            }, 0x100)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x100 && data != null) {
            val list = data.getStringArrayListExtra(MEDIA_SELECT_RESULT_KEY)
            list?.forEach { println(it) }
            try {
                val me = MediaExtractor()
                me.setDataSource(list!![0])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}