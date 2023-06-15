package com.meikai.giftplayer.mx.filter;


import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.meikai.giftplayer.mx.GlUtils;
import com.meikai.giftplayer.mx.TextureCropUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 着色器--》混合alpha和rgb
 * MP4是特殊的形式，左半边取alpha，右半边取rgb
 */
public class AlphaMp4Filter extends Filter {
    private static final String vertexShader = "attribute vec2 a_position;\n"
            + "attribute vec2 a_texCoord;\n"
            + "varying vec2 v_texCoord;\n"
            + "uniform mat4 transform;\n"
            + "void main(void) {\n"
            + "gl_Position = vec4(a_position, 0.0, 1.0);\n"
            + "v_texCoord = a_texCoord;\n"
            + "}\n";

    private static final String alphaShader = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 v_texCoord;\n"
            + "uniform mat4 transform;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "void main() {\n"
            + "float midX = (transform * vec4(0.5,0,0,0)).x;\n"
            + "vec2 RTex = v_texCoord;\n"
            + "vec2 LTex = vec2(RTex.x-midX,RTex.y);\n"
            +" vec4 color = texture2D(sTexture,RTex);\n"
            +" vec4 alpha = texture2D(sTexture,LTex);\n"
            + "gl_FragColor = vec4(color.rgb,alpha.r);\n"
            + "}\n";


    private float[] triangleVerticesData;

    private float[] textureTransform;


    public AlphaMp4Filter() {
        this(vertexShader, alphaShader);
    }

    public AlphaMp4Filter(final String vertexShader, final String fragmentShader) {
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
    }

    @Override
    public final void init() {
        onInit();
        mIsInitialized = true;
        onInitialized();
    }

    public void measure(){
        if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) {
            return;
        }
        try {
            triangleVerticesData = TextureCropUtil.calculateCropCenter(
                    viewWidth, viewHeight, videoWidth, videoHeight);
            triangleVertices = ByteBuffer.allocateDirect(triangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            triangleVertices.put(triangleVerticesData).position(0);
        } catch (Exception e) {
            float[] triangleVerticesData = {
                    // x , y, s , t
                    -1.0f, 1.0f, 0.5f, 0.0f,
                    1.0f, 1.0f, 1.0f, 0.0f,
                    -1.0f, -1.0f, 0.5f, 1.0f,
                    1.0f, -1.0f, 1.0f, 1.0f,
            };
            triangleVertices = ByteBuffer.allocateDirect(
                            triangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            triangleVertices.put(triangleVerticesData).position(0);
        }

        float wW = (int) (Math.ceil(videoWidth / 16f) * 16);
        float sx = videoWidth / wW;
        textureTransform = new float[]{ sx, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1};
    }

    @Override
    protected void onInit() {
        mGLProgramId = GlUtils.loadProgram(mVertexShader, mFragmentShader);
        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgramId, "a_position");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgramId, "a_texCoord");
        mGLUniformTextureTransform = GLES20.glGetUniformLocation(mGLProgramId, "transform");
        mIsInitialized = true;
    }


    @Override
    protected void onInitialized() {

    }

    @Override
    protected void onDrawArraysPre() {
        super.onDrawArraysPre();
    }

    @Override
    public void onDraw(final int textureId) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mGLProgramId);
        if (!mIsInitialized) {
            return;
        }

        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        GLES20.glUniformMatrix4fv(mGLUniformTextureTransform, 1, false, textureTransform, 0);

        if (textureId != GlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }
}

