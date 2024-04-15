package com.lifengqiang.videoeditor.multiscreenrender.utils;

import android.opengl.GLES30;

public class ReuseRenderProgram {
    private int counter = 0;
    public int id = 0;
    public int texture_vbo = 0;

    private final int[] shader = new int[2];

    public void create() {
        if (counter++ == 0) {
            id = GLES30.glCreateProgram();
            shader[0] = MyGLUtils.createShader(VERTEX_SHADER, GLES30.GL_VERTEX_SHADER);
            shader[1] = MyGLUtils.createShader(FRAGMENT_SHADER, GLES30.GL_FRAGMENT_SHADER);
            GLES30.glAttachShader(id, shader[0]);
            GLES30.glAttachShader(id, shader[1]);
            GLES30.glLinkProgram(id);
            GLES30.glUseProgram(id);
            GLES30.glEnableVertexAttribArray(0);
            GLES30.glEnableVertexAttribArray(1);
            texture_vbo = MyGLUtils.createVBO(new float[]{
                    0, 0,
                    1, 0,
                    0, 1,
                    1, 1,
            });
        }
    }

    public void recycle() {
        if (--counter == 0) {
            GLES30.glDetachShader(id, shader[0]);
            GLES30.glDetachShader(id, shader[1]);
            GLES30.glDeleteShader(shader[0]);
            GLES30.glDeleteShader(shader[1]);
            GLES30.glDeleteProgram(id);
            GLES30.glDeleteBuffers(1, new int[]{texture_vbo}, 0);
        }
    }

    public static String VERTEX_SHADER = "#version 300 es\n" +
            "layout(location = 0) in vec2 vCoord;\n" +
            "layout(location = 1) in vec2 tCoord;\n" +
            "layout(location = 0) uniform mat4 uMatrix;\n" +
            "out vec2 texCoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMatrix * vec4(vCoord, 0., 1.);\n" +
            "    texCoord = tCoord;\n" +
            "}";
    public static String FRAGMENT_SHADER = "#version 300 es\n" +
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
            "precision mediump float;\n" +
            "in vec2 texCoord;\n" +
            "out vec4 fragment;\n" +
            "uniform sampler2D sTexture;\n" +
            "void main(){\n" +
            "    fragment = texture(sTexture, texCoord);\n" +
            "}";

    private final static ThreadLocal<ReuseRenderProgram> threadLocal = new ThreadLocal<>();

    public static ReuseRenderProgram getProgram() {
        if (threadLocal.get() == null) {
            threadLocal.set(new ReuseRenderProgram());
        }
        return threadLocal.get();
    }
}
