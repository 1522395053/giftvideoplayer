package com.meikai.giftplayer

import android.util.Log

object AlphaMp4Log {
    const val TAG = "AlphaMp4Log"
    fun e(info:String,thr:Throwable){
        Log.e(TAG,"$info,error:${thr.message}")
    }

    fun e(thr:Throwable){
        Log.e(TAG,"error:${thr.message}")
    }
    fun d(info:String){
        Log.d(TAG,info)
    }
    fun debug(info:String){
        Log.d(TAG,info)
    }
}