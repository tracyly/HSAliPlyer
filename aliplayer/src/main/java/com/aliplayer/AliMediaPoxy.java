package com.aliplayer;

import android.app.Application;

import com.alivc.player.AliVcMediaPlayer;

public class AliMediaPoxy {
    public static void init(Application pApplication){
        VodAudioPlayerManager.getInstance().init(pApplication);
    }
}
