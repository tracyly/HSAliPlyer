package com.aliplayer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.alivc.player.AliVcMediaPlayer;

import java.util.List;

public class AudioPlayerManager {
    private static class Instance {
        private static final AudioPlayerManager instance = new AudioPlayerManager();
    }

    public static AudioPlayerManager getInstance() {
        return AudioPlayerManager.Instance.instance;
    }


    private AliVcMediaPlayer mMediaPlayer;
//    private MediaPlayer mMediaPlayer;
    private String mUrl;
    private List<String> mUrlList;
    private ViewHandler mViewHandler = new ViewHandler();


    private final int STATE_ERROR = -2;
    private final int STATE_NONE = -1;
    private final int STATE_READY = 0;
    private final int STATE_PLAY = 1;
    private final int STATE_PAUSE = 2;
    private final int STATE_FINISH = 3;

    private int mState = STATE_NONE;
    private int mPlayIndex = 0;
    private boolean mListPlay = false;
    private Context mContext;
    private boolean mCanPlay = false;

    public void init(Context pContext){
        mContext = pContext;
        AliVcMediaPlayer.init(pContext);
    }

    public void setDataList(List<String> urls){
        if (urls == null || urls.isEmpty()) return;
        setData(mUrlList.get(0), true);
        mPlayIndex = 0;
    }

    public void setData(String url){
        setData(url, false);
    }

    private void setData(String url, boolean list){
        mCanPlay = false;
        if (TextUtils.isEmpty(url)) return;
        this.mListPlay = list;
        this.mUrl = url;
//        this.mUrl = "/sdcard/hsrj/testa.mp3";
//        this.mUrl = "https://m801.music.126.net/20190411172514/c4ed516c5c8ebc92c2be07b8ac8ad0a0/jdyyaac/0f0f/040e/0708/8e7c372ca23dd8e08c980b37bbf68acd.m4a";
        mState = STATE_NONE;
        mCanPlay = true;
        if (mMediaPlayer == null) {
            mMediaPlayer = new AliVcMediaPlayer(mContext);
        }
        mMediaPlayer.prepareToPlay(mUrl);
        mMediaPlayer.setPreparedListener(new com.alivc.player.MediaPlayer.MediaPlayerPreparedListener() {
            @Override
            public void onPrepared() {
                if (mState == STATE_PLAY || (mUrlList != null && !mUrlList.isEmpty())) {
                    //用户已经按了播放键或者是播放列表模式下，缓冲完成之后直接播放
                    mState = STATE_PLAY;

                    mMediaPlayer.play();
                    mViewHandler.sendEmptyMessage(0);
                }else {
                    mState = STATE_READY;
                }
                if (mOnPreparedListener != null) {
                    mOnPreparedListener.onPrepared(mMediaPlayer.getDuration());
                }
            }
        });
        mMediaPlayer.setCompletedListener(new com.alivc.player.MediaPlayer.MediaPlayerCompletedListener() {
            @Override
            public void onCompleted() {
                mState = STATE_FINISH;
                if (mListPlay) {
                    mPlayIndex++;
                    if (mPlayIndex < 0) {
                        mPlayIndex = 0;
                    }

                    if (mPlayIndex >= mUrlList.size()) {
                        mPlayIndex = 0;
                        if (mOnPreparedListener != null) {
                            mOnPreparedListener.onCompletion();
                        }
                    }else {
                        mMediaPlayer.reset();
                        setData(mUrlList.get(mPlayIndex));
                    }
                }else {
                    mPlayIndex = 0;
                    if (mOnPreparedListener != null) {
                        mOnPreparedListener.onCompletion();
                    }
                }
            }
        });
        mMediaPlayer.setErrorListener(new com.alivc.player.MediaPlayer.MediaPlayerErrorListener() {
            @Override
            public void onError(int pI, String pS) {
                if (mOnPreparedListener != null) {
                    mOnPreparedListener.onError();
                }
                mMediaPlayer.reset();
                if (mListPlay) {
                    mPlayIndex--;
                }
                mState = STATE_ERROR;
                mViewHandler.removeMessages(0);
            }
        });
    }

    public void play(){
        if (!mCanPlay) {
            if (mOnPreparedListener != null) {
                mOnPreparedListener.onError();
            }
            return;
        }
        if (mState == STATE_READY || mState == STATE_PAUSE) {
            mMediaPlayer.play();
            mViewHandler.sendEmptyMessage(0);
        }else if (mState == STATE_FINISH){
            mMediaPlayer.prepareToPlay(mUrl);
        }else {
            if (mState == STATE_ERROR) {
                setData(mUrl, mListPlay);
            }
            Toast.makeText(mContext, "正在缓冲...", Toast.LENGTH_SHORT).show();
        }
        mState = STATE_PLAY;
    }

    public void pause(){
        if (mState == STATE_FINISH) return;
        if (mState == STATE_PLAY && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mViewHandler.removeMessages(0);
        }
        mState = STATE_PAUSE;
    }

    public void seekTo(int progress){
        mMediaPlayer.seekTo(progress);
    }

    public void release(){
        mViewHandler.removeCallbacksAndMessages(0);
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.destroy();
            mMediaPlayer = null;
        }
    }

    public void setOnPreparedListener(OnPreparedListener pOnPreparedListener){
        this.mOnPreparedListener = pOnPreparedListener;
    }


    private OnPreparedListener mOnPreparedListener;



    public class ViewHandler extends Handler {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case 0:
                    if (mMediaPlayer != null && mState != STATE_FINISH) {
                        int current = mMediaPlayer.getCurrentPosition();
                        if (mOnPreparedListener != null) {
                            mOnPreparedListener.onSeekChanged(current);
                        }
                        mViewHandler.sendMessage(Message.obtain(mViewHandler, 0));
                    }
                    break;
            }
        }
    }


    public interface OnPreparedListener{
        void onPrepared(int duration);
        void onCompletion();
        void onSeekChanged(int currentPosition);
        void onError();
    }

    private boolean mPerMissionPlay = false;

    public boolean hasPermissionPlay(){
        return mPerMissionPlay;
    }

    public void setPermissionPlay(boolean pPerMissionPlay){
        mPerMissionPlay = pPerMissionPlay;
    }

    public boolean isWifi() {
        NetworkInfo networkInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return false;
    }
}
