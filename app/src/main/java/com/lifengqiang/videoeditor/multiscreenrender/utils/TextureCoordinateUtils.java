package com.lifengqiang.videoeditor.multiscreenrender.utils;

public class TextureCoordinateUtils {
    public static boolean isInvertAspectRatio(float[] matrix) {
        return (matrix[13] < 0.5) == (matrix[5] + matrix[13] < 0.5);
    }
}
