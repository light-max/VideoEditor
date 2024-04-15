package com.lifengqiang.videoeditor.multiscreenrender;

import android.os.Handler;

/**
 * 这个类和父类的区别在于父类是在有新图像输入时自动渲染，这个类则是开启线程手动渲染
 */
public class ForceMultiSurfaceRenderer extends MultiSurfaceRenderer implements Runnable {
    private Thread thread;
    private long frameShowTime;
    private long nextFrameTime;

    public ForceMultiSurfaceRenderer(int fps) {
//        super(false);
        frameShowTime = 1000000000 / fps;
        nextFrameTime = System.nanoTime() + frameShowTime;
    }

    public void startThread() {
        synchronized (this) {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }
    }

    public void stopThread() {
        synchronized (this) {
            if (thread != null) {
                thread.interrupt();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setFps(int fps) {
        frameShowTime = 1000000000 / fps;
        nextFrameTime = System.nanoTime() + frameShowTime;
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        int frame = 0;

        Handler handler = getHandler();
        while (!thread.isInterrupted()) {
            handler.sendEmptyMessage(RendererThread.NOTIFY_RENDER);

            long sleepTime = nextFrameTime - System.nanoTime();
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime / 1000000, (int) (sleepTime % 1000000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            nextFrameTime += frameShowTime;

            ++frame;
            if (System.currentTimeMillis() - time >= 1000) {
                System.out.println("fps: " + frame);
                frame = 0;
                time = System.currentTimeMillis();
            }
        }
    }
}
