package com.meikai.giftplayer.mx;

public interface IVideo {

    enum PlayerState {
        NOT_PREPARED, PREPARED, STARTED, PAUSED, STOPPED, RELEASE
    }

    void play();

    void pause();

    void stop();

    void start();

    void release();
}

