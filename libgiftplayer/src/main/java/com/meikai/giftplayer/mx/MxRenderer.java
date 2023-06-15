package com.meikai.giftplayer.mx;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.view.Surface;

import com.meikai.giftplayer.AlphaMp4Log;
import com.meikai.giftplayer.GLTextureView;
import com.meikai.giftplayer.mx.filter.AlphaMp4Filter;
import com.meikai.giftplayer.mx.filter.Filter;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class MxRenderer implements GLTextureView.Renderer, SurfaceTexture.OnFrameAvailableListener {


    private SurfaceTexture surface;
    private int textureID;
    private Filter alphaMp4Filter;

    private boolean updateSurface = false;

    private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    private OnSurfacePrepareListener onSurfacePrepareListener;

    MxRenderer() {
        alphaMp4Filter = new AlphaMp4Filter();
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        try {
            synchronized (this) {
                if (updateSurface) {
                    surface.updateTexImage();
//                surface.getTransformMatrix(sTMatrix);
                    updateSurface = false;
                }
            }
            alphaMp4Filter.onDraw(textureID);
        } catch (Throwable e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxRenderer onDrawFrame",e);
        }
    }

    @Override
    public void onSurfaceDestroyed(GL10 gl) {
        try {
            alphaMp4Filter.destroy();
        } catch (Throwable e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxRenderer onSurfaceDestroyed", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        try {
            GLES20.glViewport(0, 0, width, height);
        } catch (Throwable e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxRenderer onSurfaceChanged",e);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        try {
            alphaMp4Filter.init();
            prepareSurface();
        } catch (Throwable e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxRenderer onSurfaceCreated",e);
        }
    }

    private void prepareSurface() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        textureID = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureID);

        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        surface = new SurfaceTexture(textureID);
        surface.setOnFrameAvailableListener(this);

        Surface surface = new Surface(this.surface);
        onSurfacePrepareListener.surfacePrepared(surface);

        synchronized (this) {
            updateSurface = false;
        }
    }

    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        updateSurface = true;
    }


    void setOnSurfacePrepareListener(OnSurfacePrepareListener onSurfacePrepareListener) {
        this.onSurfacePrepareListener = onSurfacePrepareListener;
    }

    void setVideoSize(int width, int height) {
        alphaMp4Filter.setVideoSize(width,height);
        ((AlphaMp4Filter) alphaMp4Filter).measure();
    }

    void setViewSize(int viewWidth,int viewHeight){
        alphaMp4Filter.setViewSize(viewWidth,viewHeight);
        ((AlphaMp4Filter) alphaMp4Filter).measure();
    }

    interface OnSurfacePrepareListener {
        void surfacePrepared(Surface surface);
    }

}
