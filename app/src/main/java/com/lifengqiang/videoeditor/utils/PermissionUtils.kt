package com.lifengqiang.videoeditor.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build

fun tryingGetPermission(activity: Activity, permissions: Array<String>): Boolean {
    for (permissionItem in permissions) {
        if (activity.checkSelfPermission(permissionItem) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(permissions, 0)
            return false
        }
    }
    return true
}

fun tryingGetMediaPermission(activity: Activity): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        tryingGetPermission(
            activity, arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        )
    } else {
        tryingGetPermission(
            activity, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        )
    }
}