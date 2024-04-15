package com.lifengqiang.videoeditor.multiscreenrender.utils;

import android.opengl.GLES30;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MyGLUtils {
    public static int createVBO(float[] vertex) {
        Buffer buffer = ByteBuffer.allocateDirect(vertex.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertex)
                .position(0);
        int[] ids = new int[1];
        GLES30.glGenBuffers(1, ids, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, ids[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertex.length * 4, buffer, GLES30.GL_STATIC_DRAW);
        return ids[0];
    }

    public static void setVBO(int index, float[] vertex) {
        Buffer buffer = ByteBuffer.allocateDirect(vertex.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertex)
                .position(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, index);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, vertex.length * 4, buffer);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE);
    }

    public static int createShader(String code, int type) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);
        return shader;
    }
}
