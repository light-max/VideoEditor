package com.lifengqiang.videoeditor.multiscreenrender;

import android.os.Handler;
import android.os.Message;
import android.view.Surface;

import com.lifengqiang.videoeditor.multiscreenrender.renderer.GLTextureInputSourceHolder;
import com.lifengqiang.videoeditor.multiscreenrender.renderer.GLTextureRendererHolder;

public class MultiSurfaceRenderer {
    private final RendererThread mThread;
    private final Handler mHandler;

    public MultiSurfaceRenderer() {
        this(true);
    }

    protected MultiSurfaceRenderer(boolean isAutoRender) {
        mThread = new RendererThread(isAutoRender);
        mThread.start();
        mHandler = mThread.getHandler();
    }

    protected Handler getHandler() {
        return mHandler;
    }

    /**
     * 获取图像输入的surface, 可以用作于Camera,MediaCodec,ANativeWindow
     */
    public Surface getInputSurface() {
        return mThread.getInputSurface();
    }

    /**
     * 设置输入表面大小
     * MediaCodec视频高宽或Camera原始未旋转的高宽
     */
    public void setInputSurfaceSize(int width, int height) {
        mThread.getInputSurfaceTexture().setDefaultBufferSize(width, height);
        Message message = Message.obtain();
        message.what = RendererThread.INPUT_SIZE_CHANGED;
        message.arg1 = width;
        message.arg2 = height;
        mHandler.sendMessageAtFrontOfQueue(message);
    }

    /**
     * <b>只在Camera中使用时调用此函数</b><br>
     * 设置屏幕旋转的角度 根据屏幕旋转的角度再次旋转相机画面
     *
     * @param screenRotation 屏幕朝着自己面向的方向旋转的角度
     *                       只接受 90的整倍数的数值
     */
    public void setScreenRotation(int screenRotation) {
        if (screenRotation % 90 != 0)
            throw new RuntimeException("角度 " + screenRotation + "不是90的整倍数");
        Message message = Message.obtain();
        message.what = RendererThread.SCREEN_ROTATION_CHANGED;
        message.arg1 = screenRotation;
        mHandler.sendMessage(message);
    }

    /**
     * 设置输入表面渲染的渲染器, 不设置则使用默认渲染器
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
    public void setSurfaceSize(Surface surface, int width, int height) {
        Message message = Message.obtain();
        message.what = RendererThread.OUTPUT_SURFACE_SIZE_CHANGE;
        message.obj = new RendererThread.SurfaceParamsData(surface, width, height);
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
        Message message = Message.obtain();
        message.what = RendererThread.OUTPUT_SURFACE_RENDERER_CHANGED;
        message.obj = new RendererThread.SurfaceParamsData(surface, renderer);
        mHandler.sendMessage(message);
    }

    /**
     * 设置输出纹理尺寸监听器, 当输出的纹理尺寸因<b>矩阵变换</b>或<b>屏幕旋转</b>发生改变时的回调
     */
    public void setOutputSizeChangeListener(GLTextureInputSourceHolder.OnOutputTextureSizeChangedListener listener) {
        mThread.setOutputTextureSizeChangedListener(listener);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        mHandler.getLooper().quitSafely();
    }
}
