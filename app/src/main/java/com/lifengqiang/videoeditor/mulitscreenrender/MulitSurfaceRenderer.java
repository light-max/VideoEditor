package com.lifengqiang.videoeditor.mulitscreenrender;

import android.os.Handler;
import android.os.Message;
import android.view.Surface;

import com.lifengqiang.videoeditor.mulitscreenrender.renderer.GLTextureInputSourceHolder;
import com.lifengqiang.videoeditor.mulitscreenrender.renderer.GLTextureRendererHolder;
import com.lifengqiang.videoeditor.mulitscreenrender.utils.DefaultTextureOutputRenderer;

public class MulitSurfaceRenderer {
    private final RendererThread mThread;
    private final Handler mHandler;

    public MulitSurfaceRenderer() {
        mThread = new RendererThread();
        mThread.start();
        mHandler = mThread.getHandler();
    }

    /**
     * 获取图像输入的surface, 可用作Camera,MediaCodec,ANativeWindow
     */
    public Surface getInputSurface() {
        return mThread.getInputSurface();
    }

    /**
     * 设置输入表面大小
     * MediaCodec视频尺寸或Camera原始未旋转的尺寸
     *
     * @param isInvertAspectRatio camera需要旋转90度或270度时可设置未true
     */
    public void setInputSurfaceSize(int width, int height, boolean isInvertAspectRatio) {
        mThread.getInputSurfaceTexture().setDefaultBufferSize(width, height);
        Message message = Message.obtain();
        message.what = RendererThread.INPUT_SIZE_CHANGED;
        message.obj = new RendererThread.InputSurfaceParamsData(width, height, isInvertAspectRatio);
        mHandler.sendMessageAtFrontOfQueue(message);
    }

    public void setInputSurfaceSize(int widht, int height) {
        setInputSurfaceSize(widht, height, false);
    }

    /**
     * 设置输入表面的渲染器, 不设置则使用默认渲染器
     */
    public void setInputRenderer(GLTextureInputSourceHolder.Renderer renderer) {
        Message message = Message.obtain();
        message.what = RendererThread.INPUT_RENDERER_CHANGED;
        message.obj = renderer;
        mHandler.sendMessage(message);
    }

    /**
     * 添加要输出到的表面
     *
     * @param renderer 同时加入自定义渲染器, 为null则使用默认渲染器
     */
    public void addSurface(Surface surface, GLTextureRendererHolder.Renderer renderer) {
        Message message = Message.obtain();
        message.what = RendererThread.OUTPUT_SURFACE_ATTACH;
        message.obj = new RendererThread.SurfaceParamsData(surface, renderer);
        mHandler.sendMessage(message);
    }

    public void addSurface(Surface surface) {
        addSurface(surface, null);
    }

    /**
     * 设置输出表面的大小
     * SurfaceView的高度和宽度,MediaCodec编码时视频的高度和宽度
     */
    public void setSurfaceSize(Surface surface, int width, int hegith) {
        Message message = Message.obtain();
        message.what = RendererThread.OUTPUT_SURFACE_SIZE_CHANGE;
        message.obj = new RendererThread.SurfaceParamsData(surface, width, hegith);
        mHandler.sendMessage(message);
    }

    /**
     * 移除要渲染的表面
     * SurfaceView.Callback.onSurfaceDestroy时或MediaCodec编码完成时
     */
    public void removeSurface(Surface surface) {
        GLTextureRendererHolder renderer = mThread.getRenderer(surface);
        if (renderer != null) {
            renderer.setRemove(true);
        }
        Message message = Message.obtain();
        message.what = RendererThread.OUTPUT_SURFACE_DETACH;
        message.obj = surface;
        mHandler.sendMessageAtFrontOfQueue(message);
    }

    /**
     * 设置要渲染的表面的渲染器, 设置null则使用默认渲染器
     */
    public void setSurfaceRenderer(Surface surface, GLTextureRendererHolder.Renderer renderer) {
        renderer = renderer != null ? renderer : new DefaultTextureOutputRenderer();
        Message message = Message.obtain();
        message.what = RendererThread.OUTPUT_SURFACE_RENDERER_CHANGED;
        message.obj = new RendererThread.SurfaceParamsData(surface, renderer);
        mHandler.sendMessage(message);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        mHandler.getLooper().quitSafely();
    }
}
