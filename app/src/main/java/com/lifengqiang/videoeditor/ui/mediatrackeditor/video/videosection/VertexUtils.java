package com.lifengqiang.videoeditor.ui.mediatrackeditor.video.videosection;

public class VertexUtils {
    public static final float[] input = new float[]{
            0, 0,/**/ -1, 1,
            1, 0,/**/ 1, 1,
            0, 1,/**/ -1, -1,
            1, 1,/**/ 1, -1,
    };

    public static final float[] default_vertex = new float[]{
            -1, 1,
            1, 1,
            -1, -1,
            1, -1
    };

    public static final float[] default_texture = new float[]{
            0, 0,
            1, 0,
            0, 1,
            1, 1,
    };
}
