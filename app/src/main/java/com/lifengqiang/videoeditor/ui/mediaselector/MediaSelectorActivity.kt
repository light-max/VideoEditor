package com.lifengqiang.videoeditor.ui.mediaselector

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.lifengqiang.videoeditor.R
import com.lifengqiang.videoeditor.base.BaseActivity
import com.lifengqiang.videoeditor.base.SimpleSingleItemRecyclerAdapter
import com.lifengqiang.videoeditor.base.SimpleSingleItemRecyclerAdapter.OnItemClickListener
import com.lifengqiang.videoeditor.databinding.ActivityMediaSelectorBinding
import com.lifengqiang.videoeditor.model.IMediaSelectorModel
import com.lifengqiang.videoeditor.ui.mediaselector.preview.PreviewAudioPopupWindow
import com.lifengqiang.videoeditor.ui.mediaselector.preview.PreviewPicturePopupWindow
import com.lifengqiang.videoeditor.ui.mediaselector.preview.PreviewVideoPopupWindow
import com.lifengqiang.videoeditor.utils.timeTranslateMMSS
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

const val MEDIA_SELECT_FLAG = "select_flag"
const val MEDIA_SELECT_RESULT_KEY = "result_key"

const val MEDIA_SELECT_AUDIO = 0x1
const val MEDIA_SELECT_VIDEO = 0x2
const val MEDIA_SELECT_PICTURE = 0x4

class MediaSelectorActivity : BaseActivity<MediaSelectorPresenter, ActivityMediaSelectorBinding>(),
    IMediaSelectorView, OnItemClickListener<IMediaSelectorModel.MediaData> {
    private val mediaType: Int by lazy { intent.getIntExtra(MEDIA_SELECT_FLAG, 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            close.setOnClickListener { finish() }
            groupName.setOnClickListener { presenter.showMediaGroup() }
            if (mediaType and MEDIA_SELECT_AUDIO != 0) {
                recycler.addItemDecoration(
                    DividerItemDecoration(
                        this@MediaSelectorActivity,
                        DividerItemDecoration.VERTICAL
                    )
                )
                recycler.layoutManager = LinearLayoutManager(this@MediaSelectorActivity)
                confirm.visibility = View.GONE
            } else {
                recycler.layoutManager = GridLayoutManager(this@MediaSelectorActivity, 4)
                confirm.visibility = View.VISIBLE
                confirm.setOnClickListener {
                    val adapter = recycler.adapter as ImageListAdapter
                    val fileList = adapter.selected.map { it.media.path }
                    presenter.returnSelectResult(fileList)
                }
            }
        }
        presenter.queryMedia(mediaType)
    }

    override fun showMediaGroups(groups: List<IMediaSelectorModel.MediaGroup>) {
        MediaGroupListPopupWindow(this)
            .setData(groups, presenter::selectMediaGroup)
            .showAsDropDown(binding.root)
    }

    @SuppressLint("SetTextI18n")
    override fun showMediaList(
        viewType: IMediaSelectorView.MediaViewType?,
        group: IMediaSelectorModel.MediaGroup
    ) {
        binding.groupName.text = "${group.name} (${group.list.size})"
        if (viewType == IMediaSelectorView.MediaViewType.FILE_PATH) {
            binding.recycler.adapter = FileListAdapter(group.list)
                .apply { setOnItemClickListener(this@MediaSelectorActivity) }
        } else if (viewType == IMediaSelectorView.MediaViewType.MEDIA_COVER) {
            val list = group.list.map { ImageListAdapter.MediaDataWrapper(it) }
            binding.recycler.adapter = ImageListAdapter(list)
                .apply {
                    setOnItemClickListener { data, position ->
                        onClick(
                            data.media,
                            position
                        )
                    }
                    onItemChangedListener = { number ->
                        binding.confirm.isEnabled = number > 0
                    }
                }
        }
    }

    override fun onClick(data: IMediaSelectorModel.MediaData, position: Int) {
        when (data.type) {
            IMediaSelectorModel.MediaType.AUDIO ->
                PreviewAudioPopupWindow(this)
                    .setAudioMedia(data) { media ->
                        presenter.returnSelectResult(listOf(media.path))
                    }.showAsView(binding.root)

            IMediaSelectorModel.MediaType.PICTURE ->
                PreviewPicturePopupWindow(this, data.path)
                    .showAsView(binding.root)

            IMediaSelectorModel.MediaType.VIDEO ->
                PreviewVideoPopupWindow(this, data.path)
                    .showAsView(binding.root)
        }
    }

    /**
     * 音频型文件列表适配器
     */
    private class FileListAdapter(list: List<IMediaSelectorModel.MediaData>) :
        SimpleSingleItemRecyclerAdapter<IMediaSelectorModel.MediaData>(list) {
        @SuppressLint("SimpleDateFormat")
        private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        override fun onBindViewHolder(
            holder: ViewHolder,
            data: IMediaSelectorModel.MediaData,
            position: Int
        ) {
            val file = File(data.path)
            holder.setText(R.id.name, file.name)
                .setText(R.id.duration, timeTranslateMMSS(data.duration / 1000))
                .setText(R.id.time, "创建日期: ${dateFormat.format(Date(data.modified))}")
                .setText(R.id.path, data.path)
        }

        override fun getItemViewLayout(): Int {
            return R.layout.item_media_selector_audio_file
        }
    }

    /**
     * 视频或图片的列表适配器
     */
    private class ImageListAdapter(list: List<MediaDataWrapper>) :
        SimpleSingleItemRecyclerAdapter<ImageListAdapter.MediaDataWrapper>(list) {
        data class MediaDataWrapper(
            val media: IMediaSelectorModel.MediaData,
            var check: Boolean = false
        )

        val selected = mutableListOf<MediaDataWrapper>()
        var onItemChangedListener: (Int) -> Unit = {}

        override fun onBindViewHolder(
            holder: ViewHolder,
            wrapper: MediaDataWrapper,
            position: Int
        ) {
            holder.getImage(R.id.image).apply {
                Glide.with(holder.itemView)
                    .asBitmap()
                    .load(File(wrapper.media.path))
                    .override(128)
                    .centerCrop()
                    .into(this)
            }
            holder.getText(R.id.duration).apply {
                if (wrapper.media.type == IMediaSelectorModel.MediaType.VIDEO) {
                    visibility = View.VISIBLE
                    text = timeTranslateMMSS(wrapper.media.duration / 1000)
                } else {
                    visibility = View.GONE
                }
            }
            holder.getImage(R.id.checkbox).setOnClickListener {
                wrapper.check = !wrapper.check
                if (wrapper.check) {
                    selected.add(wrapper)
                } else {
                    val begin = selected.indexOf(wrapper)
                    selected.removeAt(begin)
                    for (i in begin until selected.size) {
                        val selectedWrapper = selected[i]
                        val hasChangedIndex = data.indexOf(selectedWrapper)
                        if (hasChangedIndex in 0..<dataCount) {
                            notifyItemChanged(hasChangedIndex)
                        }
                    }
                }
                onItemChangedListener(selected.size)
                setSelectView(holder, wrapper)
            }
            setSelectView(holder, wrapper)
        }

        private fun setSelectView(holder: ViewHolder, wrapper: MediaDataWrapper) {
            holder.getImage(R.id.checkbox).isSelected = wrapper.check
            val text = holder.getText(R.id.index)
            if (wrapper.check) {
                text.visibility = View.VISIBLE
                val indexOf = selected.indexOf(wrapper) + 1
                text.text = "$indexOf"
            } else {
                text.visibility = View.GONE
            }
        }

        override fun getItemViewLayout(): Int {
            return R.layout.item_media_selector_image_file
        }
    }
}