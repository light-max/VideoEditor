package com.lifengqiang.videoeditor.ui.mediatrackeditor.video.videosection;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.AndroidRuntimeException;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ExtractorThread extends Thread implements Handler.Callback {
    private Looper looper;
    private Handler handler;

    private final EGLHelper egl;
    private final SurfaceTexture input;
    private final Surface surface;

    private MediaExtractor me = null;
    private MediaCodec codec = null;
    private MediaFormat format = null;
    private MediaCodec.BufferInfo bufferInfo = null;
    private int trackIndex = -1;
    public int width = 0, height = 0;
    private long durationSecond = 0;
    private boolean isReady = false;

    private long progressStepSize = 1000000;
    private long currentProgress = 0;

    private final float[] temp_matrix = new float[16];
    private int framebuffer_texture = 0;
    private int oes_texture = 0;
    private int vbo = 0;

    private static final int DEFAULT_OUTPUT_SIZE = 160;
    private int output_width = 0;
    private int output_height = 0;

    public ExtractorThread() {
        super("VideoExtractor");
        egl = new EGLHelper();
        input = new SurfaceTexture(false);
        surface = new Surface(input);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        VideoParams params = (VideoParams) msg.obj;
        isReady = prepareMedia(params.path);
        if (!isReady) return true;
        if (width > height) {
            output_height = DEFAULT_OUTPUT_SIZE;
            float ratio = (float) width / (float) height;
            output_width = (int) (ratio * output_height);
        } else {
            output_width = DEFAULT_OUTPUT_SIZE;
            float ratio = (float) height / (float) width;
            output_height = (int) (ratio * output_width);
        }

        if (format.containsKey(MediaFormat.KEY_ROTATION)) {
            if (format.getInteger(MediaFormat.KEY_ROTATION) % 180 != 0) {
                int t = output_width;
                output_width = output_height;
                output_height = t;
            }
        }

        params.onPrepared((int) (format.getLong(MediaFormat.KEY_DURATION) / 1000),
                width, height,
                output_width, output_height
        );
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, framebuffer_texture);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                output_width, output_height, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glViewport(0, 0, output_width, output_height);
        decodeVideo(params);
        params.onFinish();
        releaseMediaCodec();
        return true;
    }

    private boolean prepareMedia(String fd) {
        me = null;
        codec = null;
        format = null;
        bufferInfo = null;
        isReady = false;
        trackIndex = -1;
        width = height = 0;
        durationSecond = 0;
        currentProgress = 0;
        progressStepSize = 1000000;
        try {
            me = new MediaExtractor();
            me.setDataSource(fd);
            for (int i = 0; i < me.getTrackCount(); i++) {
                MediaFormat f = me.getTrackFormat(i);
                String mine = f.getString(MediaFormat.KEY_MIME);
                if (mine.startsWith("video")) {
                    trackIndex = i;
                    format = f;
                    break;
                }
            }
            if (trackIndex == -1) return false;
            me.selectTrack(trackIndex);
            width = format.getInteger(MediaFormat.KEY_WIDTH);
            height = format.getInteger(MediaFormat.KEY_HEIGHT);
            durationSecond = format.getLong(MediaFormat.KEY_DURATION) / 1000000;
            // 15秒分3片
            if (durationSecond <= 15) {
                progressStepSize *= 5;
                // 30秒分5片
            } else if (durationSecond <= 30) {
                progressStepSize *= (30.0 / 5);
                // 45秒分7片
            } else if (durationSecond <= 45) {
                progressStepSize *= (45.0 / 7);
                // 1分钟分8片
            } else if (durationSecond <= 60) {
                progressStepSize *= (60.0 / 8);
                // 2分钟分9片
            } else if (durationSecond <= 120) {
                progressStepSize *= (120.0 / 9);
                // 5分钟分10片
            } else if (durationSecond <= 300) {
                progressStepSize *= (300.0 / 10);
                // 12分钟分12片
            } else if (durationSecond <= 720) {
                progressStepSize *= (720.0 / 12);
                // 20分钟14片
            } else if (durationSecond <= 1200) {
                progressStepSize *= (1200.0 / 14);
                // 30分钟分16片
            } else if (durationSecond <= 1800) {
                progressStepSize *= (1800.0 / 16);
                // 30分钟以上分20片
            } else {
                progressStepSize *= (float) durationSecond / 20;
            }
            // 尽量去掉片头全黑图片
            me.seekTo(progressStepSize / 10, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            codec.configure(format, surface, null, 0);
            codec.start();
            bufferInfo = new MediaCodec.BufferInfo();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void releaseMediaCodec() {
        if (codec != null) {
            codec.stop();
            codec.release();
            codec = null;
        }
        if (me != null) {
            me.release();
            me = null;
        }
    }

    private void decodeVideo(VideoParams params) {
        while (true) {
            int input = codec.dequeueInputBuffer(2000);
            if (input >= 0) {
                ByteBuffer buffer = codec.getInputBuffer(input);
                int sampleSize = me.readSampleData(buffer, 0);
                long sampleTime = me.getSampleTime();
                if (me.advance() && sampleSize > 0) {
                    codec.queueInputBuffer(input, 0, sampleSize, sampleTime, 0);
                } else {
                    codec.queueInputBuffer(input, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
            }
            int output = codec.dequeueOutputBuffer(bufferInfo, 2000);
            if (output >= 0) {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                } else {
                    codec.releaseOutputBuffer(output, true);

                    SystemClock.sleep(10);
                    drawTexture();
                    params.onDraw(output_width, output_height);
                    egl.swipeBuffers();

                    codec.flush();
                    currentProgress += progressStepSize;
                    me.seekTo(currentProgress, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    if (me.getSampleTime() < currentProgress) {
                        break;
                    }
                }
            }
        }
    }

    private void createGL() {
        // 创建输入渲染画面缓存纹理
        int[] texture = new int[1];
        GLES30.glGenTextures(1, texture, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE);
        // 创建输入渲染画面缓冲帧
        int[] framebuffer = new int[1];
        GLES30.glGenFramebuffers(1, framebuffer, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer[0]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, texture[0], 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_NONE);
        // 创建输入帧
        int[] oes_texture = new int[1];
        GLES30.glGenTextures(1, oes_texture, 0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oes_texture[0]);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_NONE);
        // 连接纹理到surface
        input.attachToGLContext(oes_texture[0]);
        // 启用帧缓冲区
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer[0]);
        // 创建着色器
        ShaderUtils.createProgram(ShaderUtils.input[0], ShaderUtils.input[1]);
        GLES30.glEnableVertexAttribArray(0);
        // 创建缓冲对象
        float[] vertex = VertexUtils.input;
        Buffer buffer = ByteBuffer.allocateDirect(vertex.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertex)
                .position(0);
        // 创建顶点缓冲区
        int[] vbo = new int[1];
        GLES30.glGenBuffers(1, vbo, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertex.length * 4, null, GLES30.GL_STATIC_DRAW);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, vertex.length * 4, buffer);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE);
        this.vbo = vbo[0];
        this.framebuffer_texture = texture[0];
        this.oes_texture = oes_texture[0];
    }

    private void drawTexture() {
        input.updateTexImage();
        input.getTransformMatrix(temp_matrix);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oes_texture);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glVertexAttribPointer(0, 4, GLES30.GL_FLOAT, false, 16, 0);
        GLES30.glUniformMatrix4fv(0, 1, false, temp_matrix, 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
//        egl.swipeBuffers();
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (this) {
            looper = Looper.myLooper();
            notifyAll();
        }
        if (egl.createEGL()) {
            createGL();
        } else {
            throw new AndroidRuntimeException("EGL创建失败");
        }
        Looper.loop();
        egl.destroyEGL();
        releaseMediaCodec();
    }

    public Looper getLooper() {
        if (!isAlive()) return null;
        boolean wasInterrupted = false;
        synchronized (this) {
            while (isAlive() && looper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                }
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return looper;
    }

    public Handler getHandler() {
        if (handler == null) {
            handler = new Handler(getLooper(), this);
        }
        return handler;
    }

    public void quitSafety() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quitSafely();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        quitSafety();
    }

    public interface OnCaptureCallback {
        void onCapture(Bitmap bitmap);

        void onFinish(VideoFormat format);
    }

    public static class VideoFormat {
        public String path;
        public int durationMs;
        public int sliceWidth, sliceHeight;
        public int videoWidth, videoHeight;
    }

    public static class VideoParams {
        private Bitmap bitmap = null;
        private ByteBuffer buffer = null;
        private final String path;
        private final OnCaptureCallback callback;
        private VideoFormat format;

        public VideoParams(String path, OnCaptureCallback callback) {
            this.path = path;
            this.callback = callback;
        }

        public void onPrepared(int durationMs, int videoWidth, int videoHeight, int sliceWidth, int sliceHeight) {
            bitmap = Bitmap.createBitmap(sliceWidth, sliceHeight, Bitmap.Config.ARGB_8888);
            buffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
            format = new VideoFormat();
            format.durationMs = durationMs;
            format.videoWidth = videoWidth;
            format.videoHeight = videoHeight;
            format.sliceWidth = sliceWidth;
            format.sliceHeight = sliceHeight;
            format.path = path;
        }

        public void onDraw(int width, int height) {
            buffer.position(0);
            GLES30.glReadPixels(0, 0, width, height,
                    GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);
            buffer.position(0);
            bitmap.copyPixelsFromBuffer(buffer);
            callback.onCapture(bitmap);
        }

        public void onFinish() {
            callback.onFinish(format);
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            bitmap.recycle();
        }
    }
}
