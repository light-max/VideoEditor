package com.lifengqiang.videoeditor.multiscreenrender.utils;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;

import com.lifengqiang.videoeditor.multiscreenrender.renderer.GLTextureRendererHolder;

public class DefaultTextureOutputRenderer implements GLTextureRendererHolder.Renderer {
    private final ReuseRenderProgram program = ReuseRenderProgram.getProgram();

    private int vertex_vbo = 0;
    private int viewport_width, viewport_height, texture_width, texture_height;
    private float[] matrix = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1,
    };

    @Override
    public void onCreated() {
        program.create();
        vertex_vbo = MyGLUtils.createVBO(new float[]{
                -1, 1,
                1, 1,
                -1, -1,
                1, -1
        });
    }

    @Override
    public void onRecycle() {
        program.recycle();
        GLES30.glDeleteBuffers(1, new int[]{vertex_vbo}, 0);
    }

    @Override
    public void onSurfaceSizeChanged(int width, int height) {
        viewport_width = width;
        viewport_height = height;
        resetVertexVBO();
    }

    @Override
    public void onInputTextureSizeChanged(int width, int height) {
        texture_width = width;
        texture_height = height;
        resetVertexVBO();
    }

    private void resetVertexVBO() {
        float[] vertex;
        float[] projection = new float[16];
        float[] view = new float[16];
        float ratio;
        // 窗口高宽比大于画面高宽比 宽度设置成1 高度设置成height/width 上下留黑边
        if (((float) viewport_height / viewport_width) > ((float) texture_height / texture_width)) {
            ratio = (float) viewport_height / viewport_width;
            Matrix.frustumM(projection, 0,
                    -1, 1,
                    -ratio, ratio,
                    2.f, 10.f);
            float input_ratio = (float) texture_height / texture_width;
            vertex = new float[]{
                    -1, input_ratio,
                    1, input_ratio,
                    -1, -input_ratio,
                    1, -input_ratio
            };
        } else {
            ratio = (float) viewport_width / viewport_height;
            Matrix.frustumM(projection, 0,
                    -ratio, ratio,
                    -1, 1,
                    2.f, 10.f);
            float input_ratio = (float) texture_width / texture_height;
            vertex = new float[]{
                    -input_ratio, 1,
                    input_ratio, 1,
                    -input_ratio, -1,
                    input_ratio, -1
            };
        }
        matrix = new float[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1,
        };
        Matrix.setLookAtM(view, 0,
                0, 0, 2,
                0, 0, 0,
                0, 1, 0);
        Matrix.multiplyMM(matrix, 0, projection, 0, view, 0);

        MyGLUtils.setVBO(vertex_vbo, vertex);
    }

    @Override
    public void onTextureDraw(int texName, int texWidth, int texHeight) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(program.id);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texName);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertex_vbo);
        GLES30.glUniformMatrix4fv(0, 1, false, matrix, 0);
        GLES30.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, program.texture_vbo);
        GLES30.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE);
    }
}