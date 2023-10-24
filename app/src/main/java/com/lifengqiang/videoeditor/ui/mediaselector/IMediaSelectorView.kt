package com.lifengqiang.videoeditor.ui.mediaselector

import androidx.annotation.MainThread
import com.lifengqiang.videoeditor.base.IView
import com.lifengqiang.videoeditor.model.IMediaSelectorModel

interface IMediaSelectorView : IView {
    enum class MediaViewType {
        FILE_PATH,
        MEDIA_COVER,
    }

    @MainThread
    fun showMediaGroups(groups: List<IMediaSelectorModel.MediaGroup>)

    @MainThread
    fun showMediaList(viewType: MediaViewType?, group: IMediaSelectorModel.MediaGroup)
}