package com.lifengqiang.videoeditor.mulitscreenrender.utils;

import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import com.lifengqiang.videoeditor.mulitscreenrender.renderer.GLTextureInputSourceHolder;
import com.lifengqiang.videoeditor.mulitscreenrender.renderer.GLTextureRendererHolder;

public class DefaultTextureInputSourceRenderer implements GLTextureInputSourceHolder.Renderer {
    private static final String VERTEX_SHADER = "#version 300 es\n" +
            "layout(location = 0) in vec4 vertex;\n" +
            "layout(location = 0) uniform mat4 tMatrix;\n" +
            "out vec2 texCoord;\n" +
            "void main(){\n" +
            "   gl_Position = vec4(vertex[2], vertex[3], 0., 1.);\n" +
            "   vec4 tCoord = vec4(vertex.x, vertex.y, 0., 1.);\n" +
            "   texCoord = (tMatrix * tCoord).xy;\n" +
            "}";
    private static final String FRAGMENT_SHADER = "#version 300 es\n" +
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
            "precision mediump float;\n" +
            "in vec2 texCoord;\n" +
            "out vec4 fragment;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main(){\n" +
            "   fragment = texture(sTexture, texCoord);\n" +
            "}";
    private static final float[] VERTEX = new float[]{
            0, 0,/**/-1, 1,
            1, 0,/**/1, 1,
            0, 1,/**/-1, -1,
            1, 1,/**/1, -1,
    };
    private int program = 0;
    private int vbo = 0;
    private int[] shader = new int[2];

    @Override
    public void onCreated() {
        program = GLES30.glCreateProgram();
        shader = new int[]{
                MyGLUtils.createShader(VERTEX_SHADER, GLES30.GL_VERTEX_SHADER),
                MyGLUtils.createShader(FRAGMENT_SHADER, GLES30.GL_FRAGMENT_SHADER)
        };
        GLES30.glAttachShader(program, shader[0]);
        GLES30.glAttachShader(program, shader[1]);
        GLES30.glLinkProgram(program);
        GLES30.glUseProgram(program);
        GLES30.glEnableVertexAttribArray(0);
        vbo = MyGLUtils.createVBO(VERTEX);
    }

    @Override
    public void onSurfaceSizeChanged(int width, int height) {
    }

    @Override
    public void onDrawFrame(int texName, float[] transformMatrix) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(program);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texName);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glVertexAttribPointer(0, 4, GLES30.GL_FLOAT, false, 16, 0);
        GLES30.glUniformMatrix4fv(0, 1, false, transformMatrix, 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void onRecycle() {
        GLES30.glDetachShader(program, shader[0]);
        GLES30.glDetachShader(program, shader[1]);
        GLES30.glDeleteProgram(program);
        GLES30.glDeleteShader(shader[0]);
        GLES30.glDeleteShader(shader[1]);
        GLES30.glDeleteBuffers(1, new int[]{vbo}, 0);
    }
}
