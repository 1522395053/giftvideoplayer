package com.meikai.giftplayer.mx;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Surface;

import com.meikai.giftplayer.AlphaMp4Log;

import java.io.File;
import java.io.FileDescriptor;
import java.util.HashMap;



/**
 *
 * 1.setVideoFromFile url assets...设置MP4资源路径(目前仅支持礼物特定的MP4)
 * 2.play 开始播放
 */
@SuppressLint("ViewConstructor")
public class MxVideoView extends GLTextureView implements IVideo {

    private boolean isPreparing;

    private static final int GL_CONTEXT_VERSION = 2;

    private MxRenderer renderer;
    private Surface surface;
    private MediaPlayer mediaPlayer;

    private OnVideoStartedListener onVideoStartedListener;
    private OnVideoEndedListener onVideoEndedListener;

    private boolean isSurfaceCreated;
    private boolean isDataSourceSet;
    private boolean isStartPrepare;

    private PlayerState state = PlayerState.NOT_PREPARED;


    public MxVideoView(Context context) {
        super(context);
        init();
    }

    public MxVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setVolume(float leftVolume, float rightVolume){
        if(mediaPlayer!=null){
            mediaPlayer.setVolume(leftVolume,rightVolume);
        }
    }

    @SuppressWarnings("unused")
    public void setVideoFromAssets(String assetsFileName) {
        reset();

        try {
            AssetFileDescriptor assetFileDescriptor = getContext().getAssets().openFd(assetsFileName);
            mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
            mediaPlayer.setLooping(false);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());

            onDataSourceSet(retriever);

        } catch (Exception e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxVideoView setVideoFromAssets: "+assetsFileName,e);
        }
    }

    public void setVideoFromFile(File file) {
        reset();

        try {
            if (file.exists()) {
                mediaPlayer.setDataSource(file.getAbsolutePath());

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(file.getAbsolutePath());
                onDataSourceSet(retriever);
            }
        } catch (Exception e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxVideoView setVideoFromFile: " + file.getAbsolutePath(), e);
        }
    }

    public void setVideoFromSD(String fileName) {
        reset();

        try {
            File file = new File(fileName);
            if (file.exists()) {
                mediaPlayer.setDataSource(fileName);

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(fileName);
                onDataSourceSet(retriever);
            }
        } catch (Exception e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxVideoView setVideoFromSD: " + fileName, e);
        }
    }

    @SuppressWarnings("unused")
    public void setVideoByUrl(String url) {
        reset();

        try {
            mediaPlayer.setDataSource(url);
            if (mediaPlayer == null) {
                return;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(url, new HashMap<>());

            onDataSourceSet(retriever);

        } catch (Exception e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxVideoView setVideoByUrl: " + url, e);
        }
    }

    @SuppressWarnings("unused")
    public void setVideoFromFile(FileDescriptor fileDescriptor) {
        reset();

        try {
            mediaPlayer.setDataSource(fileDescriptor);
            if (mediaPlayer == null) {
                return;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(fileDescriptor);

            onDataSourceSet(retriever);

        } catch (Exception e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxVideoView setVideoFromFile: ", e);
        }
    }

    @SuppressWarnings("unused")
    public void setVideoFromFile(FileDescriptor fileDescriptor, int startOffset, int endOffset) {
        reset();

        try {
            mediaPlayer.setDataSource(fileDescriptor, startOffset, endOffset);
            if (mediaPlayer == null) {
                return;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(fileDescriptor, startOffset, endOffset);

            onDataSourceSet(retriever);

        } catch (Exception e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxVideoView setVideoFromFile startOffset: "+startOffset+" endOffset: "+endOffset, e);
        }
    }

    @SuppressWarnings("unused")
    @TargetApi(23)
    public void setVideoFromMediaDataSource(MediaDataSource mediaDataSource) {
        reset();

        mediaPlayer.setDataSource(mediaDataSource);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mediaDataSource);

        onDataSourceSet(retriever);
    }

    @SuppressWarnings("unused")
    public void setVideoFromUri(Context context, Uri uri) {
        reset();

        try {
            mediaPlayer.setDataSource(context, uri);
            if (mediaPlayer == null) {
                return;
            }
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(context, uri);

            onDataSourceSet(retriever);
        } catch (Exception e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxVideoView setVideoFromUri: " + uri, e);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    /**
     *
     * 播放
     */
    @Override
    public void play() {
        isStartPrepare = true;
        if (isSurfaceCreated && !isPreparing) {
            prepareAndStartMediaPlayer();
            AlphaMp4Log.INSTANCE.d("libx.android.alphamp4.MxVideoView play");
        }
    }

    /**
     *  prepare--->start
     *  paused--->start
     *  stopped--->start
     *  若没调用过play 调用start无效
     */
    @Override
    public void start() {
        if (mediaPlayer != null) {
            switch (state) {
                case PREPARED: {
                    mediaPlayer.start();
                    state = PlayerState.STARTED;
                    if (onVideoStartedListener != null) {
                        onVideoStartedListener.onVideoStarted();
                    }

                    break;
                }
                case PAUSED: {
                    onResume();
                    mediaPlayer.start();
                    state = PlayerState.STARTED;
                    break;
                }
                case STOPPED: {
                    onResume();
                    prepareAsync(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaPlayer.start();
                            state = PlayerState.STARTED;
                            if (onVideoStartedListener != null) {
                                onVideoStartedListener.onVideoStarted();
                            }
                        }
                    });

                    break;
                }
                default: {
                    if (onVideoEndedListener != null) {
                        onVideoEndedListener.onVideoEnded();
                    }
                    break;
                }
            }
        }
        AlphaMp4Log.INSTANCE.d("libx.android.alphamp4.MxVideoView start");
    }

    @Override
    public void pause() {
        if (mediaPlayer != null && state == PlayerState.STARTED) {
            mediaPlayer.pause();
            state = PlayerState.PAUSED;
            AlphaMp4Log.INSTANCE.d("libx.android.alphamp4.MxVideoView pause");
        }
        onPause();
    }

    @Override
    public void stop() {
        if (mediaPlayer != null && (state == PlayerState.STARTED || state == PlayerState.PAUSED)) {
            mediaPlayer.stop();
            state = PlayerState.STOPPED;
            AlphaMp4Log.INSTANCE.d("libx.android.alphamp4.MxVideoView stop");
        }
        onPause();
    }

    public void reset() {
        if (mediaPlayer != null && (state == PlayerState.STARTED || state == PlayerState.PAUSED || state == PlayerState.STOPPED)) {
            mediaPlayer.reset();
            state = PlayerState.NOT_PREPARED;
        }
    }

    @Override
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            state = PlayerState.RELEASE;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
        AlphaMp4Log.INSTANCE.d("libx.android.alphamp4.MxVideoView release");
    }

    public PlayerState getState() {
        return state;
    }

    public boolean isPlaying() {
        return state == PlayerState.STARTED;
    }

    public boolean isPaused() {
        return state == PlayerState.PAUSED;
    }

    @SuppressWarnings("unused")
    public boolean isStopped() {
        return state == PlayerState.STOPPED;
    }

    @SuppressWarnings("unused")
    public boolean isReleased() {
        return state == PlayerState.RELEASE;
    }

    @SuppressWarnings("unused")
    public void seekTo(int msec) {
        mediaPlayer.seekTo(msec);
    }

    @SuppressWarnings("unused")
    public void setLooping(boolean looping) {
        mediaPlayer.setLooping(looping);
    }

    @SuppressWarnings("unused")
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void setScreenOnWhilePlaying(boolean screenOn) {
        mediaPlayer.setScreenOnWhilePlaying(screenOn);
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener) {
        mediaPlayer.setOnErrorListener(onErrorListener);
    }

    public void setOnVideoStartedListener(OnVideoStartedListener onVideoStartedListener) {
        this.onVideoStartedListener = onVideoStartedListener;
    }

    public void setOnVideoEndedListener(OnVideoEndedListener onVideoEndedListener) {
        this.onVideoEndedListener = onVideoEndedListener;
    }

    @SuppressWarnings("unused")
    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener onSeekCompleteListener) {
        mediaPlayer.setOnSeekCompleteListener(onSeekCompleteListener);
    }

    @SuppressWarnings("unused")
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public interface OnVideoStartedListener {
        void onVideoStarted();
    }

    public interface OnVideoEndedListener {
        void onVideoEnded();
    }

    private void init() {
        try {
            AlphaMp4Log.INSTANCE.d("libx.android.alphamp4.MxVideoView init");

            setEGLContextClientVersion(GL_CONTEXT_VERSION);
            setEGLConfigChooser(8, 8, 8, 8, 16, 0);

            initMediaPlayer();

            renderer = new MxRenderer();

            renderer.setOnSurfacePrepareListener(new MxRenderer.OnSurfacePrepareListener() {
                @Override
                public void surfacePrepared(Surface surface) {
                    isSurfaceCreated = true;
                    mediaPlayer.setSurface(surface);
                    MxVideoView.this.surface = surface;
                    if (isDataSourceSet && !isPreparing && isStartPrepare) {
                        prepareAndStartMediaPlayer();
                    }
                }
            });

            setRenderer(renderer);

            setPreserveEGLContextOnPause(true);
            setOpaque(false);
        } catch (Throwable e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxVideoView init ", e);
        }
    }

    private void initMediaPlayer() {
        state = PlayerState.NOT_PREPARED;
        mediaPlayer = new MediaPlayer();
        setScreenOnWhilePlaying(true);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                state = PlayerState.PAUSED;
                if (onVideoEndedListener != null) {
                    onVideoEndedListener.onVideoEnded();
                }
            }
        });
    }

    private void prepareAndStartMediaPlayer() {
        AlphaMp4Log.INSTANCE.d("libx.android.alphamp4.MxVideoView prepareAndStartMediaPlayer");
        prepareAsync(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                start();
            }
        });
    }


    private void onDataSourceSet(MediaMetadataRetriever retriever) {
        int videoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int videoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        AlphaMp4Log.INSTANCE.d("libx.android.alphamp4.MxVideoView onDataSourceSet w " + videoWidth + "  h " + videoHeight);
        isDataSourceSet = true;
        renderer.setVideoSize(videoWidth,videoHeight);
//        if (isSurfaceCreated && !isPreparing) {
//            prepareAndStartMediaPlayer();
//        }
    }

    private void prepareAsync(final MediaPlayer.OnPreparedListener onPreparedListener) {
        try {
            if (mediaPlayer != null && state == PlayerState.NOT_PREPARED) {
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        AlphaMp4Log.INSTANCE.d("libx.android.alphamp4.MxVideoView onPrepared");

                        state = PlayerState.PREPARED;
                        isPreparing = false;
                        onPreparedListener.onPrepared(mp);
                    }
                });
                AlphaMp4Log.INSTANCE.d("libx.android.alphamp4.MxVideoView prepareAsync");

                mediaPlayer.prepareAsync();
                isPreparing = true;
            }
        } catch (Throwable e) {
            AlphaMp4Log.INSTANCE.e("libx.android.alphamp4.MxVideoView prepareAsync ", e);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        renderer.setViewSize(getMeasuredWidth(), getMeasuredHeight());
    }
}


