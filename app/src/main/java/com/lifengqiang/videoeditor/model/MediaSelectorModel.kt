package com.lifengqiang.videoeditor.model

import android.content.Context
import android.provider.MediaStore
import java.io.File

interface IMediaSelectorModel {
    enum class MediaType {
        AUDIO, VIDEO, PICTURE
    }

    data class MediaGroup(val name: String, val list: List<MediaData>)

    data class MediaData(
        val path: String,
        val modified: Long, // 创建时间 毫秒
        val duration: Int, // 持续时间 毫秒
        val type: MediaType
    )

    fun queryMedia(context: Context, type: MediaType): List<MediaData>

    fun groupByDir(list: List<MediaData>): MutableList<MediaGroup>

    fun groupByDirOfPictureVideo(list: List<MediaData>): MutableList<MediaGroup>
}

class MediaSelectorModel : IMediaSelectorModel {
    override fun queryMedia(
        context: Context,
        type: IMediaSelectorModel.MediaType
    ): List<IMediaSelectorModel.MediaData> {
        try {
            val cursor = context.contentResolver.query(
                when (type) {
                    IMediaSelectorModel.MediaType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    IMediaSelectorModel.MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    IMediaSelectorModel.MediaType.PICTURE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                },
                arrayOf(
                    MediaStore.MediaColumns.DATA,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.DURATION
                ),
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} desc"
            )
            val list = mutableListOf<IMediaSelectorModel.MediaData>()
            while (cursor?.moveToNext() == true) {
                val filePath = cursor.getString(0)
                val modifiedTime = cursor.getInt(1) * 1000L
                val durationTime = cursor.getInt(2)
                list.add(IMediaSelectorModel.MediaData(filePath, modifiedTime, durationTime, type))
            }
            cursor?.close()
            return list
        } catch (e: Exception) {
            e.printStackTrace()
            return listOf()
        }
    }

    override fun groupByDir(source: List<IMediaSelectorModel.MediaData>): MutableList<IMediaSelectorModel.MediaGroup> {
        val result: MutableList<IMediaSelectorModel.MediaGroup> =
            mutableListOf(IMediaSelectorModel.MediaGroup("all", source))
        val map: HashMap<String, MutableList<IMediaSelectorModel.MediaData>> = HashMap()
        for (media in source) {
            val groupName = File(media.path).parentFile?.name ?: continue
            var list = map[groupName]
            if (list == null) {
                list = mutableListOf()
                map[groupName] = list
            }
            list.add(media)
        }
        for (entry in map.entries) {
            result.add(IMediaSelectorModel.MediaGroup(entry.key, entry.value))
        }
        return result
    }

    override fun groupByDirOfPictureVideo(list: List<IMediaSelectorModel.MediaData>): MutableList<IMediaSelectorModel.MediaGroup> {
        val result = groupByDir(list)
        val videos: MutableList<IMediaSelectorModel.MediaData> = mutableListOf()
        for (media in list) {
            if (media.type == IMediaSelectorModel.MediaType.VIDEO) {
                videos.add(media)
            }
        }
        if (videos.isNotEmpty()) {
            result.add(1, IMediaSelectorModel.MediaGroup("All Local Videos", videos))
        }
        return result
    }
}