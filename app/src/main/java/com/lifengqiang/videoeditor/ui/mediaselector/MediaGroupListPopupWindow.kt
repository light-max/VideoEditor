package com.lifengqiang.videoeditor.ui.mediaselector

import android.app.Activity
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import com.lifengqiang.videoeditor.R
import com.lifengqiang.videoeditor.base.BasePopupWindow
import com.lifengqiang.videoeditor.base.SimpleSingleItemRecyclerAdapter
import com.lifengqiang.videoeditor.databinding.DialogMediaGroupListBinding
import com.lifengqiang.videoeditor.model.IMediaSelectorModel

class MediaGroupListPopupWindow(private val activity: Activity) :
    BasePopupWindow<DialogMediaGroupListBinding>(
        activity,
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
    ) {
    init {
        binding.root.setOnClickListener { dismiss() }
    }

    fun setData(
        data: List<IMediaSelectorModel.MediaGroup>,
        callback: (index: Int) -> Unit
    ): MediaGroupListPopupWindow {
        val list = data.mapIndexed { index, mediaGroup ->
            GroupData(
                index,
                mediaGroup.name,
                mediaGroup.list.size
            )
        }
        val adapter = GroupListAdapter(list)
        adapter.setOnItemClickListener { data, _ ->
            dismiss()
            callback(data.index)
        }
        binding.recycler.addItemDecoration(
            DividerItemDecoration(
                activity,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.recycler.adapter = adapter
        return this
    }

    private data class GroupData(val index: Int, val name: String, val fileCount: Int)

    private class GroupListAdapter(list: List<GroupData>) :
        SimpleSingleItemRecyclerAdapter<GroupData>(list) {
        override fun onBindViewHolder(holder: ViewHolder?, data: GroupData?, position: Int) {
            holder?.setText(R.id.text, "${data?.name} (${data?.fileCount})")
        }

        override fun getItemViewLayout(): Int {
            return R.layout.item_media_selector_group_name
        }
    }
}