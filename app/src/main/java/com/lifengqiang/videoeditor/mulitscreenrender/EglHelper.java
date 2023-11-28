package com.lifengqiang.videoeditor.mulitscreenrender;

import android.opengl.EGLExt;
import android.util.Log;
import android.view.Surface;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class EglHelper {
    private static final String TAG = "MSREGLHelper";
    private EGL10 mEgl;
    private EGLDisplay mDisplay = null;
    private EGLContext mContext = null;
    private EGLConfig mConfig = null;
    private EGLSurface mOESSurface = null;

    public boolean createEGL() {
        mEgl = (EGL10) EGLContext.getEGL();
        if ((mDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)) == EGL10.EGL_NO_DISPLAY) {
            Log.e(TAG, "egl no display");
            return false;
        }

        int[] version = new int[2];
        if (!mEgl.eglInitialize(mDisplay, version)) {
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
        if (!mEgl.eglChooseConfig(mDisplay, confAttr,
                eglConfigs, 1,
                numConfigs)) {
            Log.e(TAG, "egl choose config fail");
            return false;
        }
        mConfig = eglConfigs[0];

        int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        int[] ctxAttr = {EGL_CONTEXT_CLIENT_VERSION, 3, EGL10.EGL_NONE};
        mContext = mEgl.eglCreateContext(mDisplay, eglConfigs[0], EGL10.EGL_NO_CONTEXT, ctxAttr);
        if (mContext == null) {
            Log.e(TAG, "egl context create fail error: 0x" + Integer.toHexString(mEgl.eglGetError()));
            return false;
        }

        mEgl.eglMakeCurrent(mDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, mContext);
        Log.i(TAG, "egl initial finish");
        int[] attrib = new int[]{
                EGL10.EGL_WIDTH, 1,
                EGL10.EGL_HEIGHT, 1,
                EGL10.EGL_NONE
        };
        mOESSurface = mEgl.eglCreatePbufferSurface(mDisplay, mConfig, attrib);
        return true;
    }

    public EGLSurface getOESSurface() {
        return mOESSurface;
    }

    public EGLSurface attatchSurface(Surface surface) {
        EGLSurface eglSurface = mEgl.eglCreateWindowSurface(mDisplay, mConfig, surface, null);
        if (eglSurface != null) {
            return eglSurface;
        } else {
            Log.e(TAG, "surface attach fail " + surface);
            return null;
        }
    }

    public void detachSurface(EGLSurface surface) {
        mEgl.eglDestroySurface(mDisplay, surface);
    }

    public boolean makeCurrent(EGLSurface surface) {
        if (!mEgl.eglMakeCurrent(mDisplay, surface, surface, mContext)) {
            Log.e(TAG, "makeCurrent: fail");
            return false;
        }
        return true;
    }

    public void swipeBuffers(EGLSurface surface) {
        if (!mEgl.eglSwapBuffers(mDisplay, surface)) {
            Log.e(TAG, "swipeBuffers: fail");
        }
    }

    public void destroyEGL() {
        mEgl.eglDestroySurface(mDisplay, mOESSurface);
        mEgl.eglDestroyContext(mDisplay, mContext);
        mEgl.eglTerminate(mDisplay);
    }
}
