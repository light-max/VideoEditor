package com.lifengqiang.videoeditor.data;

import java.io.Serializable;

public class MediaSelectorResult implements Serializable {
    private final String path;
    /*** 0:图片 1:音频 2:视频 */
    private final int type;

    public MediaSelectorResult(String path, int type) {
        this.path = path;
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public int getType() {
        return type;
    }
}
