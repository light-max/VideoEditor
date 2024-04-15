package com.lifengqiang.videoeditor.ui.mediatrackeditor.video;

import java.util.List;

public class VideoSectionData {
    private String path;
    private int durationMs;
    private int sliceWidth, sliceHeight;
    private int videoWidth, videoHeight;
    private List<String> slices;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    public int getSliceWidth() {
        return sliceWidth;
    }

    public void setSliceWidth(int sliceWidth) {
        this.sliceWidth = sliceWidth;
    }

    public int getSliceHeight() {
        return sliceHeight;
    }

    public void setSliceHeight(int sliceHeight) {
        this.sliceHeight = sliceHeight;
    }

    public List<String> getSlices() {
        return slices;
    }

    public void setSlices(List<String> slices) {
        this.slices = slices;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    @Override
    public String toString() {
        return "VideoSectionData{" +
                "path='" + path + '\'' +
                "\n, durationMs=" + durationMs +
                "\n, sliceWidth=" + sliceWidth +
                "\n, sliceHeight=" + sliceHeight +
                "\n, rawWidth=" + videoWidth +
                "\n, rawHeight=" + videoHeight +
                "\n, slices=" + slices +
                '}';
    }
}
