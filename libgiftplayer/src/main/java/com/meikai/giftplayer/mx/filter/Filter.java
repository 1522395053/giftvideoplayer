package com.meikai.giftplayer.mx.filter;

import android.opengl.GLES20;

import java.nio.FloatBuffer;

public abstract class Filter {

    protected static final int FLOAT_SIZE_BYTES = 4;
    protected static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 4 * FLOAT_SIZE_BYTES;
    protected static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    protected static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 2;

    protected FloatBuffer triangleVertices;
    protected String mVertexShader;
    protected String mFragmentShader;
    protected int mGLProgramId;
    protected int mGLAttribPosition;
    protected int mGLAttribTextureCoordinate;
    protected int mGLUniformTextureTransform;
    protected boolean mIsInitialized;
    public int videoWidth;
    public int videoHeight;
    public int viewWidth;
    public int viewHeight;

    protected void onDrawArraysPre() {
    }

    protected abstract void onInitialized();

    public abstract void init();

    protected abstract void onInit();

    public abstract void onDraw(int textureId);

    public void setVideoSize(int videoWidth, int videoHeight) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
    }

    public void setViewSize(int viewWidth, int viewHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
    }

    public int getProgram() {
        return mGLProgramId;
    }

    public void destroy() {
        mIsInitialized = false;
        GLES20.glDeleteProgram(mGLProgramId);
    }
}

