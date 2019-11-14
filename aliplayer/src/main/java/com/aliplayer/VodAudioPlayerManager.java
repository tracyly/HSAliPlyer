package com.aliplayer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.alivc.player.AliVcMediaPlayer;
import com.aliyun.vodplayer.media.AliyunVidSts;
import com.aliyun.vodplayer.media.AliyunVodPlayer;
import com.aliyun.vodplayer.media.IAliyunVodPlayer;

import java.util.List;

/**
 * vod播放管理器，适用加密音频的播放
 */
public class VodAudioPlayerManager {
    private static class Instance {
        private static final VodAudioPlayerManager instance = new VodAudioPlayerManager();
    }

    public static VodAudioPlayerManager getInstance() {
        return VodAudioPlayerManager.Instance.instance;
    }

    private AliyunVodPlayer mMediaPlayer;
//    private AliVcMediaPlayer mMediaPlayer;
//    private MediaPlayer mMediaPlayer;
    private List<String> mUrlList;

    private ViewHandler mViewHandler = new ViewHandler();

    //播放状态
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

    private AliyunVidSts mAliyunVidSts;
    private OnPreparedListener mOnPreparedListener;

    public void init(Context pContext){
        mContext = pContext;
        AliVcMediaPlayer.init(pContext);
    }

    /**
     * 设置需要播放的音频内容，用于预加载音频信息
     * @param audioID 音频id
     * @param tempAccessKeySecret sts中的aks
     * @param tempToken sts中的token
     * @param tempAccessKeyId sts中的aki
     */
    public void setData(String audioID, String tempAccessKeySecret, String tempToken, String tempAccessKeyId){
        mAliyunVidSts = new AliyunVidSts();
        mAliyunVidSts.setAcId(tempAccessKeyId);
        mAliyunVidSts.setAkSceret(tempAccessKeySecret);
        mAliyunVidSts.setSecurityToken(tempToken);
        mAliyunVidSts.setVid(audioID);
        setData();
    }

    private void setData(){
        mCanPlay = false;
        if (mAliyunVidSts == null) return;
//        this.mUrl = "/sdcard/hsrj/testa.mp3";
//        this.mUrl = "https://m801.music.126.net/20190411172514/c4ed516c5c8ebc92c2be07b8ac8ad0a0/jdyyaac/0f0f/040e/0708/8e7c372ca23dd8e08c980b37bbf68acd.m4a";
        mState = STATE_NONE;
        mCanPlay = true;
        if (mMediaPlayer == null) {
            mMediaPlayer = new AliyunVodPlayer(mContext);
        }
        mMediaPlayer.prepareAsync(mAliyunVidSts);
        mMediaPlayer.setOnPreparedListener(new IAliyunVodPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {
                if (mState == STATE_PLAY || (mUrlList != null && !mUrlList.isEmpty())) {
                    //用户已经按了播放键或者是播放列表模式下，缓冲完成之后直接播放
                    mState = STATE_PLAY;

                    mMediaPlayer.start();
                    mViewHandler.sendEmptyMessage(0);
                }else {
                    mState = STATE_READY;
                }
                if (mOnPreparedListener != null) {
                    mOnPreparedListener.onPrepared(mMediaPlayer.getDuration());
                }
            }
        });
        mMediaPlayer.setOnCompletionListener(new IAliyunVodPlayer.OnCompletionListener() {
            @Override
            public void onCompletion() {
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
                        mAliyunVidSts.setVid(mUrlList.get(mPlayIndex));
                        setData();
                    }
                }else {
                    mPlayIndex = 0;
                    if (mOnPreparedListener != null) {
                        mOnPreparedListener.onCompletion();
                    }
                }
            }
        });
        mMediaPlayer.setOnErrorListener(new IAliyunVodPlayer.OnErrorListener() {
            @Override
            public void onError(int pI, int pI1, String pS) {
                if (mOnPreparedListener != null) {
                    mOnPreparedListener.onError(pS);
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

    /**
     * 播放操作
     */
    public void play(){
        if (!mCanPlay) {
            if (mOnPreparedListener != null) {
                mOnPreparedListener.onError(null);
            }
            return;
        }
        if (mState == STATE_READY || mState == STATE_PAUSE) {
            mMediaPlayer.start();
            mViewHandler.sendEmptyMessage(0);
        }else if (mState == STATE_FINISH){
            mMediaPlayer.prepareAsync(mAliyunVidSts);
        }else {
            if (mState == STATE_ERROR) {
                setData();
            }
            Toast.makeText(mContext, "正在缓冲...", Toast.LENGTH_SHORT).show();
        }
        mState = STATE_PLAY;
    }

    /**
     * 暂停操作
     */
    public void pause(){
        if (mState == STATE_FINISH) return;
        if (mState == STATE_PLAY && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mViewHandler.removeMessages(0);
        }
        mState = STATE_PAUSE;
    }

    /**
     * 快进或倒退，拖动进度条
     * @param progress 进度
     */
    public void seekTo(int progress){
        mMediaPlayer.seekTo(progress);
    }

    /**
     * 释放对应的MediaPlayer，页面销毁时调用
     */
    public void release(){
        mViewHandler.removeCallbacksAndMessages(0);
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * 设置播放中一些状态监听事件
     * @param pOnPreparedListener
     */
    public void setOnPreparedListener(OnPreparedListener pOnPreparedListener){
        this.mOnPreparedListener = pOnPreparedListener;
    }

    /**
     * 用于回归主线程的ui操作，或延时事件的处理
     */
    public class ViewHandler extends Handler {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case 0:
                    if (mMediaPlayer != null && mState != STATE_FINISH) {
                        long current = mMediaPlayer.getCurrentPosition();
                        if (mOnPreparedListener != null) {
                            mOnPreparedListener.onSeekChanged(current);
                        }
                        mViewHandler.sendMessage(Message.obtain(mViewHandler, 0));
                    }
                    break;
            }
        }
    }

    /**
     * 整个播放中状态的监听
     */
    public interface OnPreparedListener{
        /**
         * 预加载回调
         * @param duration 音频总时长
         */
        void onPrepared(long duration);
        /** 音频播放完成回调 */
        void onCompletion();

        /**
         * 监听播放进度的改变，适用于进度条更新
         * @param currentPosition 当前进度条
         */
        void onSeekChanged(long currentPosition);
        /** 获取音频信息失败 */
        void onError(String info);
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
    public long getDuration(){
        long currentPosition = mMediaPlayer.getDuration();
        return currentPosition;

    }
}
