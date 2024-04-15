package com.lifengqiang.videoeditor.ui.mediaselector

import android.app.Activity
import android.content.Intent
import com.lifengqiang.videoeditor.base.BasePresenter
import com.lifengqiang.videoeditor.data.MediaSelectorResult
import com.lifengqiang.videoeditor.model.IMediaSelectorModel
import com.lifengqiang.videoeditor.model.MediaSelectorModel

class MediaSelectorPresenter : BasePresenter<MediaSelectorModel, IMediaSelectorView>() {
    private var mediaGroups: List<IMediaSelectorModel.MediaGroup> = mutableListOf()
    private var viewType: IMediaSelectorView.MediaViewType? = null

    fun queryMedia(mediaType: Int) {
        var isVideoAndPictureMix = false
        val mediaList = view()?.context()?.run {
            if (mediaType and MEDIA_SELECT_AUDIO != 0) {
                viewType = IMediaSelectorView.MediaViewType.FILE_PATH
                model().queryMedia(this, IMediaSelectorModel.MediaType.AUDIO)
            } else if (mediaType and MEDIA_SELECT_VIDEO != 0 && mediaType and MEDIA_SELECT_PICTURE != 0) {
                isVideoAndPictureMix = true
                viewType = IMediaSelectorView.MediaViewType.MEDIA_COVER
                val picture = model().queryMedia(this, IMediaSelectorModel.MediaType.PICTURE)
                val video = model().queryMedia(this, IMediaSelectorModel.MediaType.VIDEO)
                val list: MutableList<IMediaSelectorModel.MediaData> =
                    mutableListOf<IMediaSelectorModel.MediaData>()
                        .apply {
                            addAll(picture)
                            addAll(video)
                            sortByDescending { it.modified }
                        }
                list
            } else if (mediaType and MEDIA_SELECT_PICTURE != 0) {
                viewType = IMediaSelectorView.MediaViewType.MEDIA_COVER
                model().queryMedia(this, IMediaSelectorModel.MediaType.PICTURE)
            } else if (mediaType and MEDIA_SELECT_VIDEO != 0) {
                viewType = IMediaSelectorView.MediaViewType.MEDIA_COVER
                model().queryMedia(this, IMediaSelectorModel.MediaType.VIDEO)
            } else {
                mutableListOf()
            }
        }
        if (mediaList != null) {
            mediaGroups = if (isVideoAndPictureMix) {
                model().groupByDirOfPictureVideo(mediaList)
            } else {
                model().groupByDir(mediaList)
            }
            selectMediaGroup(0)
        }
    }

    fun selectMediaGroup(position: Int) {
        if (mediaGroups.isEmpty()) return
        if (position >= 0 && position < mediaGroups.size) {
            runOnUIThread { view()?.showMediaList(viewType, mediaGroups[position]) }
        } else {
            throw IndexOutOfBoundsException("下表范围超出 position:$position arraySize:${mediaGroups.size}")
        }
    }

    fun showMediaGroup() {
        view()?.showMediaGroups(mediaGroups)
    }

    fun returnSelectResult(list: List<MediaSelectorResult>) {
        view()?.activity()?.apply {
            val intent = Intent().apply {
                putExtra(MEDIA_SELECT_RESULT_KEY, ArrayList(list))
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }
}