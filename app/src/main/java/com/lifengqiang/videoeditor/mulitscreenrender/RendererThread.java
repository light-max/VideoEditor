package com.lifengqiang.videoeditor.mulitscreenrender;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.lifengqiang.videoeditor.mulitscreenrender.renderer.GLTextureInputSourceHolder;
import com.lifengqiang.videoeditor.mulitscreenrender.renderer.GLTextureRendererHolder;

import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLSurface;

public class RendererThread extends Thread implements Handler.Callback {
    private static final String TAG = RendererThread.class.getSimpleName();
    /*** 输入纹理大小发生改变 */
    public static final int INPUT_SIZE_CHANGED = 0;
    /*** 把目标surface添加到渲染输出列表 */
    public static final int OUTPUT_SURFACE_ATTACH = 1;
    /*** 输出的目标surface即将被销毁或不再使用 */
    public static final int OUTPUT_SURFACE_DETACH = 2;
    /*** 输出目标surface大小发生改变 */
    public static final int OUTPUT_SURFACE_SIZE_CHANGE = 3;
    /*** 输入纹理通知渲染 */
    public static final int NOTIFY_RENDER = 4;
    /*** 设置新的输入纹理的渲染器 */
    public static final int INPUT_RENDERER_CHANGED = 5;
    /*** 设置输出目标surface的渲染器 */
    public static final int OUTPUT_SURFACE_RENDERER_CHANGED = 6;

    private Looper mLooper = null;
    private Handler mHandler = null;

    private final EglHelper mEgl;
    private final GLTextureInputSourceHolder mInput;
    private final Map<Surface, GLTextureRendererHolder> mRenderers;

    public RendererThread() {
        super(TAG);
        mEgl = new EglHelper();
        mRenderers = new HashMap<>();
        mInput = new GLTextureInputSourceHolder(st -> {
            if (mHandler != null) {
                mHandler.sendEmptyMessage(NOTIFY_RENDER);
            }
        });
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (this) {
            mLooper = Looper.myLooper();
            notifyAll();
        }
        if (!mEgl.createEGL()) {
            throw new AndroidRuntimeException("EGL环境创建失败");
        }
        if (!mEgl.makeCurrent(mEgl.getOESSurface())) {
            throw new AndroidRuntimeException("EGL上下文切换失败");
        }
        mInput.performSurfaceCreated();
        Looper.loop();
        mEgl.destroyEGL();
        mLooper = null;
        mHandler = null;
        Log.i(TAG, "exit");
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case INPUT_SIZE_CHANGED: {
                InputSurfaceParamsData data = (InputSurfaceParamsData) msg.obj;
                mInput.performSurfaceSizeChanged(data.width, data.height, data.invertAspectRatio);
                for (GLTextureRendererHolder renderer : mRenderers.values()) {
                    renderer.performSurfaeSizeChanged(
                            mInput.getOutputTextureWidth(),
                            mInput.getOutputTextureHeight()
                    );
                }
                Log.i(TAG, "New InputSize: " + data.width + "," + data.height);
                break;
            }
            case INPUT_RENDERER_CHANGED:
                mInput.setNewRenderer((GLTextureInputSourceHolder.Renderer) msg.obj);
                Log.i(TAG, "New InputRenderer: " + msg.obj);
                break;
            case OUTPUT_SURFACE_ATTACH: {
                SurfaceParamsData data = (SurfaceParamsData) msg.obj;
                Surface surface = data.surface;
                if (mRenderers.containsKey(surface)) {
                    Log.w(TAG, "表面: " + surface + "连接了两次");
                } else {
                    EGLSurface eglSurface = mEgl.attatchSurface(surface);
                    if (eglSurface != null) {
                        GLTextureRendererHolder renderer;
                        if (data.renderer != null) {
                            renderer = new GLTextureRendererHolder(surface, eglSurface, data.renderer);
                        } else {
                            renderer = new GLTextureRendererHolder(surface, eglSurface);
                        }
                        mRenderers.put(surface, renderer);
                        renderer.performCreated();
                    } else {
                        Log.w(TAG, "表面: " + surface + "连接失败");
                    }
                }
                break;
            }
            case OUTPUT_SURFACE_DETACH: {
                Surface surface = (Surface) msg.obj;
                if (mRenderers.containsKey(surface)) {
                    GLTextureRendererHolder renderer = mRenderers.get(surface);
                    if (renderer != null) {
                        renderer.performRecycle();
                        mEgl.detachSurface(renderer.getEglSurface());
                    }
                }
                mRenderers.remove(surface);
                break;
            }
            case OUTPUT_SURFACE_SIZE_CHANGE: {
                SurfaceParamsData data = (SurfaceParamsData) msg.obj;
                GLTextureRendererHolder renderer = mRenderers.get(data.surface);
                if (renderer != null) {
                    renderer.performSurfaeSizeChanged(data.width, data.height);
                    if (mInput.isSurfaceSizeChanged()) {
                        renderer.performInputTextureSizeChanged(
                                mInput.getOutputTextureWidth(),
                                mInput.getOutputTextureHeight()
                        );
                    }
                }
                Log.i(TAG, "New OutputSurfaceSize: " + data.width + "," + data.height);
                break;
            }
            case OUTPUT_SURFACE_RENDERER_CHANGED: {
                SurfaceParamsData data = (SurfaceParamsData) msg.obj;
                GLTextureRendererHolder renderer = mRenderers.get(data.surface);
                if (renderer != null) {
                    renderer.setNewRenderer(data.renderer);
                    Log.i(TAG, "NewOutputRenderer-success: " + data.surface + "," + data.renderer.getClass().getName());
                } else {
                    Log.i(TAG, "NewOutputRenderer-fail: " + data.surface + "," + data.renderer.getClass().getName());
                }
                break;
            }
            case NOTIFY_RENDER:
                if (mEgl.makeCurrent(mEgl.getOESSurface())) {
                    mInput.bindFrameBuffer();
                    mInput.performSurfaceDraw();
                    mEgl.swipeBuffers(mEgl.getOESSurface());
                    mInput.unbindFrameBuffer();
                } else {
                    break;
                }
                for (GLTextureRendererHolder renderer : mRenderers.values()) {
                    if (!renderer.isRemove() && mEgl.makeCurrent(renderer.getEglSurface())) {
                        renderer.performGLTextureDraw(
                                mInput.getRGBAOutputTextureId(),
                                mInput.getOutputTextureWidth(),
                                mInput.getOutputTextureHeight()
                        );
                        mEgl.swipeBuffers(renderer.getEglSurface());
                    }
                }
                break;
        }
        return true;
    }

    public Looper getLooper() {
        if (!isAlive()) return null;
        boolean wasInterrupted = false;
        synchronized (this) {
            while (isAlive() && mLooper == null) {
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
        return mLooper;
    }

    public Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(getLooper(), this);
        }
        return mHandler;
    }

    public Surface getInputSurface() {
        return mInput.getSurface();
    }

    public SurfaceTexture getInputSurfaceTexture() {
        return mInput.getSurfaceTexture();
    }

    public GLTextureRendererHolder getRenderer(Surface surface) {
        return mRenderers.get(surface);
    }

    public static class SurfaceParamsData {
        public Surface surface;
        public GLTextureRendererHolder.Renderer renderer = null;
        public int width, height;

        public SurfaceParamsData(Surface surface) {
            this.surface = surface;
        }

        public SurfaceParamsData(Surface surface, GLTextureRendererHolder.Renderer renderer) {
            this.surface = surface;
            this.renderer = renderer;
        }

        public SurfaceParamsData(Surface surface, int width, int height) {
            this.surface = surface;
            this.width = width;
            this.height = height;
        }
    }

    public static class InputSurfaceParamsData {
        public int width, height;
        public boolean invertAspectRatio;

        public InputSurfaceParamsData(int width, int height, boolean invertAspectRatio) {
            this.width = width;
            this.height = height;
            this.invertAspectRatio = invertAspectRatio;
        }
    }
}
