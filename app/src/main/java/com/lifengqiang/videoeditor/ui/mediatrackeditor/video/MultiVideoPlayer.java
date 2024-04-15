package com.lifengqiang.videoeditor.ui.mediatrackeditor.video;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.view.Surface;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MultiVideoPlayer implements Runnable {
    private final List<VideoSectionData> videos = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isNewVideo = false;
    private Surface inputSurface = null;
    private Thread thread = null;
    private MediaCodec codec = null;
    private MediaExtractor extractor = null;
    private MediaFormat format = null;
    private MediaCodec.BufferInfo info = null;
    private boolean isPause = false;
    private boolean isPrepared = false;

    private int durationMS = 0;
    private int progressMS = 0;

    private final Object sliceParamsLock = new Object();
    private int maxSliceCount = 0;
    private int previousSliceCount = 0;
    private int currentSliceCount = 0;
    private float sumProgressRatio = 0;

    private final Object frameByFrameLock = new Object();
    private boolean isFrameByFrame = false;
    private boolean isNewProgress = false;
    private boolean isFrameByFrameLast = false;
    private float newProgressRatio = 0;

    private final List<WeakReference<OnPlayListener>> listeners = new ArrayList<>();

    public void registerListener(OnPlayListener listener) {
        listeners.add(new WeakReference<>(listener));
        if (isPrepared) {
            listener.onPrepared(videos.get(currentIndex));
            listener.onProgress(this, sumProgressRatio, durationMS, progressMS);
            if (isPause) listener.onPause();
            else listener.onPlay();
        } else {
            listener.onPause();
        }
    }

    public void unregisterListener(OnPlayListener listener) {
        listeners.removeIf(onPlayListenerWeakReference -> {
            if (onPlayListenerWeakReference != null) {
                return onPlayListenerWeakReference.get() == listener;
            } else {
                return false;
            }
        });
    }

    public void setInputSurface(Surface inputSurface) {
        this.inputSurface = inputSurface;
    }

    public void addVideo(VideoSectionData data) {
        synchronized (this) {
            videos.add(data);
            if (videos.size() == 1) {
                isNewVideo = true;
                notify();
            } else {
                resetSliceParams();
            }
        }
    }

    public void selectVideo(int position) {
        if (position >= 0 && position < videos.size()) {
            synchronized (this) {
                currentIndex = position;
                isNewVideo = true;
                notify();
            }
        }
    }

    private void resetSliceParams() {
        synchronized (sliceParamsLock) {
            List<VideoSectionData> videos = getVideos();
            maxSliceCount = 0;
            previousSliceCount = 0;
            for (int i = 0; i < videos.size(); i++) {
                VideoSectionData v = videos.get(i);
                int sliceCount = v.getSlices().size();
                maxSliceCount += sliceCount;
                if (i < currentIndex) {
                    previousSliceCount += sliceCount;
                }
            }
            currentSliceCount = getCurrentVideo().getSlices().size();
        }
    }

    public void setProgress(int sumSlicePosition, float sliceRatio) {
        int sliceCount = 0;
        int index = 0;
        for (VideoSectionData video : videos) {
            if (sliceCount + video.getSlices().size() > sumSlicePosition) {
                break;
            }
            sliceCount += video.getSlices().size();
            ++index;
        }
        synchronized (this) {
            if (index != currentIndex) {
                currentIndex = index;
                isNewVideo = true;
                isPrepared = false;
                notify();
            }
        }
        synchronized (frameByFrameLock) {
            float currentSliceCount = sumSlicePosition - sliceCount + sliceRatio;
            float sumRatio = currentSliceCount / videos.get(index).getSlices().size();
            newProgressRatio = sumRatio;
            isNewProgress = true;
            frameByFrameLock.notify();
        }
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, "multivideoplayer");
            thread.start();
        } else {
            throw new RuntimeException("重复开启");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void resume() {
        synchronized (this) {
            isPause = false;
            notify();
        }
    }

    public void pause() {
        synchronized (this) {
            isPause = true;
            notify();
        }
    }

    public void startFrameByFrame() {
        synchronized (frameByFrameLock) {
            isFrameByFrame = true;
            isFrameByFrameLast = false;
        }
        synchronized (this) {
            if (isPause) {
                notify();
            }
        }
    }

    public void stopFrameByFrame() {
        synchronized (frameByFrameLock) {
            isFrameByFrame = false;
            isFrameByFrameLast = true;
            frameByFrameLock.notify();
        }
    }

    public void toggle() {
        synchronized (this) {
            isPause = !isPause;
            notify();
        }
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted()) {
            // 切换视频文件
            synchronized (this) {
                if (isNewVideo) {
                    releaseMediaCodec();
                    isPrepared = prepareMediaCodec();
                    if (isPrepared) {
                        isNewVideo = false;
                        VideoSectionData currentVideo = getCurrentVideo();
                        durationMS = currentVideo.getDurationMs();
                        progressMS = 0;
                        for (WeakReference<OnPlayListener> reference : getListenersNotNull()) {
                            if (reference.get() != null) {
                                reference.get().onPrepared(currentVideo);
                            }
                        }
                        resetSliceParams();
                        startTime = System.currentTimeMillis();
                    }
                    // 视频资源准备失败，暂停
                    else try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
            }
            // 判断资源是否准备好了
            synchronized (this) {
                if (!isPrepared) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
            }
            // 逐帧模式
            boolean isFBF;
            boolean isNP;
            int seekProgressMS;
            synchronized (frameByFrameLock) {
                isFBF = isFrameByFrame;
                isNP = isNewProgress;
                seekProgressMS = (int) (newProgressRatio * durationMS);
                if (!isNP && isFrameByFrameLast) {
                    isNP = true;
                    isFrameByFrameLast = false;
                }
            }
            if (isFBF || isNP) {
                // 设置新进度时
                if (isNP) {
                    codec.flush();
                    extractor.seekTo((long) seekProgressMS * 1000L,
                            MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    while (true) {
                        int input = codec.dequeueInputBuffer(2000);
                        if (input >= 0) {
                            ByteBuffer buffer = codec.getInputBuffer(input);
                            int sampleSize = extractor.readSampleData(buffer, 0);
                            long sampleTime = extractor.getSampleTime();
                            if (extractor.advance() && sampleSize > 0) {
                                codec.queueInputBuffer(input, 0, sampleSize, sampleTime, 0);
                            } else {
                                codec.queueInputBuffer(input, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            }
                        }
                        int output = codec.dequeueOutputBuffer(info, 2000);
                        if (output >= 0) {
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break;
                            } else {
                                progressMS = (int) (info.presentationTimeUs / 1000);
                                // 如果播放进度小于目标进度则放弃这一帧，否则渲染
                                if (progressMS < seekProgressMS) {
                                    codec.releaseOutputBuffer(output, false);
                                } else {
                                    codec.releaseOutputBuffer(output, true);
                                    break;
                                }
                            }
                        }
                    }
                    synchronized (frameByFrameLock) {
                        isNewProgress = false;
                        float currentVideoRatio = (float) progressMS / (float) durationMS;
                        float sumProgress = previousSliceCount + currentSliceCount * currentVideoRatio;
                        sumProgressRatio = sumProgress / maxSliceCount;
                    }
                }

                // 逐帧模式下暂停线程并不往下执行
                synchronized (frameByFrameLock) {
                    if (isFrameByFrame) {
                        try {
                            frameByFrameLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (isNP) {
                    startTime = System.currentTimeMillis() - progressMS;
                }
                continue;
            }

            // 如果暂停 并且不在逐帧模式
            if (isPause) {
                long pauseStartTime = System.currentTimeMillis();
                synchronized (this) {
                    try {
                        for (WeakReference<OnPlayListener> reference : getListenersNotNull()) {
                            reference.get().onPause();
                        }
                        wait();
                        for (WeakReference<OnPlayListener> reference : getListenersNotNull()) {
                            reference.get().onPlay();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                long sleepTime = System.currentTimeMillis() - pauseStartTime;
                startTime += sleepTime;
                synchronized (frameByFrameLock) {
                    if (isFrameByFrame) {
                        continue;
                    }
                }
            }
            // 开始解码
            int input = codec.dequeueInputBuffer(2000);
            if (input >= 0) {
                ByteBuffer buffer = codec.getInputBuffer(input);
                int sampleSize = extractor.readSampleData(buffer, 0);
                long sampleTime = extractor.getSampleTime();
                if (extractor.advance() && sampleSize > 0) {
                    codec.queueInputBuffer(input, 0, sampleSize, sampleTime, 0);
                } else {
                    codec.queueInputBuffer(input, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
            }
            int output = codec.dequeueOutputBuffer(info, 2000);
            // 视频结束，下一个循环
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                synchronized (this) {
                    if (currentIndex + 1 < videos.size()) {
                        currentIndex += 1;
                    } else {
                        currentIndex = 0;
                    }
                    progressMS = 0;
                    durationMS = videos.get(currentIndex).getDurationMs();
                    isNewVideo = true;
                }
            }
            // 正常渲染
            else if (output >= 0) {
                synchronized (sliceParamsLock) {
                    progressMS = (int) (info.presentationTimeUs / 1000);
                    float currentVideoRatio = (float) progressMS / (float) durationMS;
                    float sumProgress = previousSliceCount + currentSliceCount * currentVideoRatio;
                    sumProgressRatio = sumProgress / maxSliceCount;
                }
                for (WeakReference<OnPlayListener> reference : getListenersNotNull()) {
                    if (reference.get() != null) {
                        reference.get().onProgress(this, sumProgressRatio, durationMS, progressMS);
                    }
                }
                codec.releaseOutputBuffer(output, true);
                long clock = System.currentTimeMillis() - startTime;
                long diff = info.presentationTimeUs / 1000 - clock;
                if (diff > 0) {
                    SystemClock.sleep(diff);
                }
            }
        }
    }

    private void releaseMediaCodec() {
        try {
            if (codec != null) {
                codec.stop();
                codec.release();
            }
            if (extractor != null) {
                extractor.release();
            }
            codec = null;
            extractor = null;
            format = null;
            info = null;
            isPrepared = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean prepareMediaCodec() {
        if (currentIndex == -1) currentIndex = 0;
        try {
            int trackIndex = -1;
            VideoSectionData data = videos.get(currentIndex);
            extractor = new MediaExtractor();
            extractor.setDataSource(data.getPath());
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                if (f.getString(MediaFormat.KEY_MIME).startsWith("video")) {
                    trackIndex = i;
                    break;
                }
            }
            if (trackIndex == -1) return false;
            format = extractor.getTrackFormat(trackIndex);
            extractor.selectTrack(trackIndex);
//            extractor.seekTo(5000000, MediaExtractor.SEEK_TO_NEXT_SYNC);
            progressMS = (int) (extractor.getSampleTime() / 1000);
//            System.out.println(progressMS);
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            codec.configure(format, inputSurface, null, 0);
            codec.start();
            info = new MediaCodec.BufferInfo();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private List<WeakReference<OnPlayListener>> getListenersNotNull() {
        listeners.removeIf(reference -> reference.get() == null);
        return listeners;
    }

    public VideoSectionData getCurrentVideo() {
        return videos.get(currentIndex);
    }

    public List<VideoSectionData> getVideos() {
        return videos;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    @SuppressLint("DefaultLocale")
    private String msToString(int ms) {
        ms /= 1000;
        return String.format("%02d:%02d", ms / 60, ms % 60);
    }

    public interface OnPlayListener {
        void onPrepared(VideoSectionData data);

        void onPlay();

        void onPause();

        void onProgress(MultiVideoPlayer player, float sumProgressRatio, int max, int progress);
    }
}