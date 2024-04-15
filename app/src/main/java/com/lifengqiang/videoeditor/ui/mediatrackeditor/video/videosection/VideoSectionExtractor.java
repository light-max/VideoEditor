package com.lifengqiang.videoeditor.ui.mediatrackeditor.video.videosection;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.lifengqiang.videoeditor.ui.mediatrackeditor.video.VideoSectionData;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VideoSectionExtractor {
    private static ExtractorThread thread;
    private static File outputPath = null;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public VideoSectionExtractor() {
        synchronized (VideoSectionExtractor.class) {
            if (thread == null) {
                thread = new ExtractorThread();
                thread.start();
            }
        }
    }

    public static void setOutputPath(File rootPath) {
        File file = new File(rootPath, "videosection");
        file.mkdirs();
        outputPath = file;
    }

    public void postVideo(String path, OnCapturePictureCallback callback) {
        ExtractorThread.VideoParams params = new ExtractorThread.VideoParams(path, new ExtractorThread.OnCaptureCallback() {
            private final List<String> outputs = new ArrayList<>();

            @Override
            public void onCapture(Bitmap bitmap) {
                try {
                    File saveFile = new File(outputPath, UUID.randomUUID().toString() + ".jpg");
                    FileOutputStream outputStream = new FileOutputStream(saveFile);
                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)) {
                        outputs.add(saveFile.getPath());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFinish(ExtractorThread.VideoFormat format) {
                handler.post(() -> {
                    VideoSectionData data = new VideoSectionData();
                    data.setPath(format.path);
                    data.setSliceWidth(format.sliceWidth);
                    data.setSliceHeight(format.sliceHeight);
                    data.setVideoWidth(format.videoWidth);
                    data.setVideoHeight(format.videoHeight);
                    data.setDurationMs(format.durationMs);
                    data.setSlices(outputs);
                    callback.onFinish(data);
                });
            }
        });
        Message message = new Message();
        message.obj = params;
        thread.getHandler().sendMessage(message);
    }

    public interface OnCapturePictureCallback {
        void onFinish(VideoSectionData data);
    }
}
