package com.lifengqiang.videoeditor.ui.mediatrackeditor.video.videosection;

import android.opengl.EGLExt;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

class EGLHelper {
    private static final String TAG = "EGLHelper";

    private EGL10 egl;
    private EGLDisplay display = null;
    private EGLContext context = null;
    private EGLConfig config = null;
    private EGLSurface surface = null;

    public boolean createEGL() {
        egl = (EGL10) EGLContext.getEGL();
        display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (display == EGL10.EGL_NO_DISPLAY) {
            Log.e(TAG, "egl no display");
            return false;
        }
        int[] version = new int[2];
        if (!egl.eglInitialize(display, version)) {
            Log.e(TAG, "egl init fail");
            return false;
        }
        Log.i(TAG, "egl version: " + version[0] + "." + version[1]);
        int[] confAttr = {
                EGL10.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_NONE
        };
        EGLConfig[] eglConfigs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!egl.eglChooseConfig(display, confAttr,
                eglConfigs, 1,
                numConfigs)) {
            Log.e(TAG, "egl choose config fail");
            return false;
        }
        config = eglConfigs[0];
        int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        int[] ctxAttr = {EGL_CONTEXT_CLIENT_VERSION, 3, EGL10.EGL_NONE};
        context = egl.eglCreateContext(display, eglConfigs[0], EGL10.EGL_NO_CONTEXT, ctxAttr);
        if (context == null) {
            Log.e(TAG, "egl context create fail error: 0x%x" + Integer.toHexString(egl.eglGetError()));
            return false;
        }
        Log.i(TAG, "egl initial finish");
        int[] attrib = new int[]{
                EGL10.EGL_WIDTH, 1,
                EGL10.EGL_HEIGHT, 1,
                EGL10.EGL_NONE
        };
        surface = egl.eglCreatePbufferSurface(display, config, attrib);
        if (!egl.eglMakeCurrent(display, surface, surface, context)) {
            Log.e(TAG, "makeCurrent: fail error 0x" + Integer.toHexString(egl.eglGetError()));
            return false;
        }
        return true;
    }

    void swipeBuffers() {
        if (!egl.eglSwapBuffers(display, surface)) {
            Log.e(TAG, "swipeBuffers: fail");
        }
    }

    void destroyEGL() {
        egl.eglDestroySurface(display, surface);
        egl.eglDestroyContext(display, context);
        egl.eglTerminate(display);
    }
}
