package com.lifengqiang.videoeditor.multiscreenrender.renderer;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.lifengqiang.videoeditor.multiscreenrender.utils.DefaultTextureInputSourceRenderer;
import com.lifengqiang.videoeditor.multiscreenrender.utils.TextureCoordinateUtils;

public class GLTextureInputSourceHolder {
    private static final String TAG = GLTextureInputSourceHolder.class.getName();
    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;
    private SurfaceTexture.OnFrameAvailableListener mFrameAvailableListener;
    private final float[] mTextureTransformMatrix = new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1,};
    private final float[] mVertexTransformMatrix = new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1,};
    private int mSurfaceWidth = 0, mSurfaceHeight = 0;
    private boolean mSurfaceSizeChanged = false;
    private Renderer mRenderer;
    private OnOutputTextureSizeChangedListener mOutputSizeChangedListener;

    private int mFrameBufferId = 0;
    private int mOESInputTextureId = 0;
    private int mRGBAOutputTextureId = 0;
    private int mOutputTextureWidth = 0;
    private int mOutputTextureHeight = 0;
    private int mScreenRotation = 0;

    public GLTextureInputSourceHolder(SurfaceTexture.OnFrameAvailableListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mSurfaceTexture = new SurfaceTexture(false);
            mSurface = new Surface(mSurfaceTexture);
            mSurfaceTexture.setOnFrameAvailableListener(listener);
        } else {
            this.mFrameAvailableListener = listener;
        }
        mRenderer = new DefaultTextureInputSourceRenderer();
    }

    public void performSurfaceCreated() {
        generateGLObject();
        mRenderer.onCreated();
    }

    public void performSurfaceSizeChanged(int width, int height) {
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        this.mSurfaceSizeChanged = true;
        mRenderer.onSurfaceSizeChanged(mSurfaceWidth, mSurfaceHeight);
        tryingChangeOutputTextureSize();
    }

    public void performScreenRotationChanged(int screenRotation) {
        this.mScreenRotation = screenRotation;
        Matrix.setRotateM(mVertexTransformMatrix, 0, mScreenRotation, 0, 0, -1);
        tryingChangeOutputTextureSize();
    }

    /**
     * EGL.eglMakeCurrent(display, egl_surface)
     * {@link #bindFrameBuffer()}
     * {@link #performSurfaceDraw()}
     * EGL.eglSwapBuffers(display, egl_surface)
     * {@link #unbindFrameBuffer()}
     */
    public void performSurfaceDraw() {
        if (isSurfaceSizeChanged()) {
            GLES30.glViewport(0, 0, mOutputTextureWidth, mOutputTextureHeight);
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mTextureTransformMatrix);
            tryingChangeOutputTextureSize();
            mRenderer.onDrawFrame(mVertexTransformMatrix, mOESInputTextureId, mTextureTransformMatrix);
        } else {
            Log.w(TAG, "输入大小未设定");
        }
    }

    public void setNewRenderer(Renderer renderer) {
        if (mRenderer != null) {
            mRenderer.onRecycle();
        }
        renderer.onCreated();
        if (isSurfaceSizeChanged()) {
            renderer.onSurfaceSizeChanged(mSurfaceWidth, mSurfaceHeight);
            renderer.onOutputTextureSizeChanged(mOutputTextureWidth, mOutputTextureHeight);
        }
        mRenderer = renderer;
    }

    private void tryingChangeOutputTextureSize() {
        boolean isInvertAspectRatioFromTexture = TextureCoordinateUtils.isInvertAspectRatio(mTextureTransformMatrix);
        boolean isInvertAspectRatioFromScreen = mScreenRotation % 180 != 0;
        if (isInvertAspectRatioFromTexture != isInvertAspectRatioFromScreen) {
            if (!(mOutputTextureWidth == mSurfaceHeight && mOutputTextureHeight == mSurfaceWidth)) {
                setOutputTextureSize(mSurfaceHeight, mSurfaceWidth);
            }
        } else {
            if (!(mOutputTextureWidth == mSurfaceWidth && mOutputTextureHeight == mSurfaceHeight)) {
                setOutputTextureSize(mSurfaceWidth, mSurfaceHeight);
            }
        }
    }

    private void setOutputTextureSize(int width, int height) {
        this.mOutputTextureWidth = width;
        this.mOutputTextureHeight = height;
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferId);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mRGBAOutputTextureId);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                width, height, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_NONE);
        mRenderer.onOutputTextureSizeChanged(width, height);
        mOutputSizeChangedListener.onOutputTextureSizeChanged(width, height);
    }

    private void generateGLObject() {
        int[] ids = new int[3];
        GLES30.glGenTextures(2, ids, 0);
        GLES30.glGenFramebuffers(1, ids, 2);
        // 输入的oes纹理
        mOESInputTextureId = ids[0];
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mOESInputTextureId);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_NONE);
        // 输出的rgba纹理
        mRGBAOutputTextureId = ids[1];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mRGBAOutputTextureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE);
        // 帧缓冲区
        mFrameBufferId = ids[2];
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferId);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, mRGBAOutputTextureId, 0);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                1, 1, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_NONE);
        // 连接纹理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mSurfaceTexture.attachToGLContext(mOESInputTextureId);
        } else {
            synchronized (this) {
                mSurfaceTexture = new SurfaceTexture(mOESInputTextureId, false);
                mSurfaceTexture.setOnFrameAvailableListener(mFrameAvailableListener);
                notifyAll();
            }
        }
    }

    public Surface getSurface() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (mSurface == null) {
                mSurface = new Surface(getSurfaceTexture());
            }
        }
        return mSurface;
    }

    public SurfaceTexture getSurfaceTexture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return mSurfaceTexture;
        } else {
            synchronized (this) {
                while (mSurfaceTexture == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return mSurfaceTexture;
            }
        }
    }

    public boolean isSurfaceSizeChanged() {
        return mSurfaceSizeChanged;
    }

    public void bindFrameBuffer() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferId);
    }

    public void unbindFrameBuffer() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_NONE);
    }

    public int getOutputTextureWidth() {
        return mOutputTextureWidth;
    }

    public int getOutputTextureHeight() {
        return mOutputTextureHeight;
    }

    public int getRGBAOutputTextureId() {
        return mRGBAOutputTextureId;
    }

    public void setOutputTextureSizeChangedListener(OnOutputTextureSizeChangedListener listener) {
        this.mOutputSizeChangedListener = listener;
    }

    public interface OnOutputTextureSizeChangedListener {
        void onOutputTextureSizeChanged(int width, int height);
    }

    public interface Renderer {
        void onCreated();

        void onRecycle();

        void onSurfaceSizeChanged(int width, int height);

        void onOutputTextureSizeChanged(int width, int height);

        void onDrawFrame(float[] vertexTransformMatrix, int texName, float[] textureTransformMatrix);
    }
}
