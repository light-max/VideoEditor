package com.lifengqiang.videoeditor.mulitscreenrender.renderer;

import android.opengl.GLES30;
import android.util.Log;
import android.view.Surface;

import com.lifengqiang.videoeditor.mulitscreenrender.utils.DefaultTextureOutputRenderer;

import javax.microedition.khronos.egl.EGLSurface;

public class GLTextureRendererHolder {
    private static final String TAG = GLTextureRendererHolder.class.getSimpleName();
    private final Surface mSurface;
    private final EGLSurface mEglSurface;
    private int mSurfaceWidth = 1;
    private int mSurfaceHeight = 1;
    private boolean mSurfaceSizeChanged = false;
    private int mInputTextureWidth = 1;
    private int mInputTextureHeight = 1;
    private boolean mInputTextureSizeChanged = false;
    private boolean mRemove = false;
    private Renderer mRenderer;

    public GLTextureRendererHolder(Surface surface, EGLSurface eglSurface) {
        this(surface, eglSurface, new DefaultTextureOutputRenderer());
    }

    public GLTextureRendererHolder(Surface surface, EGLSurface eglSurface, Renderer renderer) {
        this.mSurface = surface;
        this.mEglSurface = eglSurface;
        mRenderer = renderer;
    }

    public void performCreated() {
        mRenderer.onCreated();
    }

    public void performRecycle() {
        mRenderer.onRecycle();
    }

    public void performSurfaeSizeChanged(int width, int height) {
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        this.mSurfaceSizeChanged = true;
        mRenderer.onSurfaceSizeChanged(width, height);
    }

    public void performInputTextureSizeChanged(int width, int height) {
        this.mInputTextureWidth = width;
        this.mInputTextureHeight = height;
        this.mInputTextureSizeChanged = true;
        mRenderer.onInputTextureSizeChanged(width, height);
    }

    public void performGLTextureDraw(int texName, int texWidth, int texHeight) {
        if (isRemove()) return;
        if (!isSurfaceSizeChanged()) {
            Log.w(TAG, "表面大小未设置");
            return;
        }
        if (!isInputTextureSizeChanged()) {
            Log.w(TAG, "输入纹理大小未设置");
            return;
        }
        GLES30.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        mRenderer.onTextureDraw(texName, texWidth, texHeight);
    }

    public void setNewRenderer(Renderer renderer) {
        if (mRenderer != null) {
            mRenderer.onRecycle();
        }
        renderer.onCreated();
        if (isSurfaceSizeChanged()) {
            renderer.onSurfaceSizeChanged(mSurfaceWidth, mSurfaceHeight);
        }
        if (isInputTextureSizeChanged()) {
            renderer.onInputTextureSizeChanged(mInputTextureWidth, mInputTextureHeight);
        }
        mRenderer = renderer;
    }

    public Surface getSurface() {
        return mSurface;
    }

    public EGLSurface getEglSurface() {
        return mEglSurface;
    }

    public synchronized boolean isRemove() {
        return mRemove;
    }

    public synchronized void setRemove(boolean flag) {
        this.mRemove = flag;
    }

    public boolean isSurfaceSizeChanged() {
        return mSurfaceSizeChanged;
    }

    public boolean isInputTextureSizeChanged() {
        return mInputTextureSizeChanged;
    }

    public interface Renderer {
        void onCreated();

        void onRecycle();

        void onSurfaceSizeChanged(int width, int height);

        void onInputTextureSizeChanged(int width, int height);

        void onTextureDraw(int texName, int texWidth, int texHeight);
    }
}
