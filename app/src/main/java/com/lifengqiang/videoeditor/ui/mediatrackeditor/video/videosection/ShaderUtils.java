package com.lifengqiang.videoeditor.ui.mediatrackeditor.video.videosection;

import android.opengl.GLES30;

public class ShaderUtils {
    public static final String[] input = {"#version 300 es\n" +
            "layout(location = 0) in vec4 vertex;\n" +// 前两位是纹理坐标,后两位是顶点坐标
            "layout(location = 0) uniform mat4 tMatrix;\n" +// 纹理变换矩阵
            "out vec2 texCoord;\n" +
            "void main() {\n" +
            "    gl_Position = vec4(vertex[2], vertex[3], 0., 1.);\n" +
            "    vec4 tCoord = vec4(vertex.x, vertex.y, 0., 1.);\n" +
            "    texCoord = (tMatrix * tCoord).xy;\n" +
            "}", "#version 300 es\n" +
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
            "precision mediump float;\n" +
            "in vec2 texCoord;\n" +
            "out vec4 fragment;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    fragment = texture(sTexture, texCoord);\n" +
//            "    fragment = vec4(1.,1.,0.,1.);\n" +
            "}"
    };

    public static final String[] output = {"#version 300 es\n" +
            "layout(location = 0) in vec2 vCoord;\n" +
            "layout(location = 1) in vec2 tCoord;\n" +
            "layout(location = 0) uniform mat4 uMatrix;\n" +
            "out vec2 texCoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMatrix * vec4(vCoord, 0., 1.);\n" +
            "    texCoord = tCoord;\n" +
            "}", "#version 300 es\n" +
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
            "precision mediump float;\n" +
            "in vec2 texCoord;\n" +
            "out vec4 fragment;\n" +
            "uniform sampler2D sTexture;\n" +
            "void main(){\n" +
            "    fragment = texture(sTexture, texCoord);\n" +
//            "   vec4 c = texture(sTexture, texCoord);\n" +
//            "   int x = int(gl_FragCoord.x);\n" +
//            "   int y = int(gl_FragCoord.y);\n" +
//            "   if (x%2==0 && y%2==0)\n" +
//            "       fragment = vec4(texCoord.x,texCoord.y,texCoord.x*texCoord.y,1.);\n" +
//            "   else\n" +
//            "       fragment = c;\n" +
            "}"
    };

    public static int createProgram(String vertexCode, String fragmentCode) {
        int vertex = createShader(vertexCode, GLES30.GL_VERTEX_SHADER);
        int fragment = createShader(fragmentCode, GLES30.GL_FRAGMENT_SHADER);
        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertex);
        GLES30.glAttachShader(program, fragment);
        GLES30.glLinkProgram(program);
        GLES30.glUseProgram(program);
        return program;
    }

    public static int createShader(String code, int type) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);
        return shader;
    }
}
