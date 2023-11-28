package com.lifengqiang.videoeditor.utils

/**
 * 时间转换为mm:ss样式
 */
fun timeTranslateMMSS(second: Int): String {
    return if (second > 3600) {
        String.format("%d:%02d:%02d", second / 3600, second / 60 % 60, second % 60)
    } else {
        String.format("%02d:%02d", second / 60, second % 60)
    }
}