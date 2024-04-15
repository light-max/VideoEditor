package com.lifengqiang.videoeditor.utils

import androidx.exifinterface.media.ExifInterface
import java.io.IOException


fun getExifOrientation(filepath: String): Int {
    return try {
        val exif = ExifInterface(filepath)
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: IOException) {
        e.printStackTrace()
        0
    }
}