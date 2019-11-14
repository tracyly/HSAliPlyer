package com.aliplayer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;

import com.aliplayer.utils.NetWatchdog;
import com.alivc.player.AliyunErrorCode;
import com.alivc.player.VcPlayerLog;
import com.aliyun.vodplayer.media.AliyunLocalSource;
import com.aliyun.vodplayer.media.AliyunMediaInfo;
import com.aliyun.vodplayer.media.AliyunVodPlayer;
import com.aliyun.vodplayer.media.IAliyunVodPlayer;
import com.aliyun.vodplayer.media.IAliyunVodPlayer.PlayerState;

import java.util.concurrent.ExecutorService;


public class AliyunPlayerView extends RelativeLayout {

    private static final String TAG = AliyunPlayerView.class.getSimpleName();

    //视频画面
    private SurfaceView mSurfaceView;
    //播放器
    private AliyunVodPlayer mAliyunVodPlayer;
    //播放是否完成
    private boolean isCompleted = false;
    //用来记录前后台切换时的状态，以供恢复。
    private PlayerState mPlayerState;
    //整体缓冲进度
    private int mCurrentBufferPercentage = 0;

    //对外的各种事件监听
    private IAliyunVodPlayer.OnInfoListener mOutInfoListener = null;
    private IAliyunVodPlayer.OnErrorListener mOutErrorListener = null;
    private IAliyunVodPlayer.OnPcmDataListener mOutPcmDataListener = null;
    private IAliyunVodPlayer.OnAutoPlayListener mOutAutoPlayListener = null;
    private IAliyunVodPlayer.OnPreparedListener mOutPreparedListener = null;
    private IAliyunVodPlayer.OnCompletionListener mOutCompletionListener = null;
    private IAliyunVodPlayer.OnUrlTimeExpiredListener mOutUrlTimeExpiredListener = null;

    public AliyunPlayerView(Context context) {
        super(context);
        initVideoView();
    }

    public AliyunPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView();
    }

    public AliyunPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView();
    }

    /**
     * 初始化view
     */
    private void initVideoView() {
        //初始化播放用的surfaceView
        initSurfaceView();
        //初始化播放器
        initAliVcPlayer();
    }

    /**
     * 重置。包括一些状态值，view的状态等
     */
    private void reset() {
        isCompleted = false;
        stop();
    }

    /**
     * 切换播放状态。点播播放按钮之后的操作
     */
    private void switchPlayerState() {
        PlayerState playerState = mAliyunVodPlayer.getPlayerState();
        if (playerState == PlayerState.Started) {
            pause();
        } else if (playerState == PlayerState.Paused || playerState == PlayerState.Prepared) {
            start();
        }
    }

    /**
     * 初始化播放器显示view
     */
    private void initSurfaceView() {
        mSurfaceView = new SurfaceView(getContext().getApplicationContext());
        addSubView(mSurfaceView);

        SurfaceHolder holder = mSurfaceView.getHolder();
        //增加surfaceView的监听
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                VcPlayerLog.d(TAG, " surfaceCreated = surfaceHolder = " + surfaceHolder);
                mAliyunVodPlayer.setDisplay(surfaceHolder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width,
                                       int height) {
                VcPlayerLog.d(TAG,
                        " surfaceChanged surfaceHolder = " + surfaceHolder + " ,  width = " + width + " , height = "
                                + height);
                mAliyunVodPlayer.surfaceChanged();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                VcPlayerLog.d(TAG, " surfaceDestroyed = surfaceHolder = " + surfaceHolder);
            }
        });
    }

    /**
     * 初始化播放器
     */
    private void initAliVcPlayer() {
        mAliyunVodPlayer = new AliyunVodPlayer(getContext());
        mAliyunVodPlayer.enableNativeLog();
        //设置准备回调
        mAliyunVodPlayer.setOnPreparedListener(new IAliyunVodPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {
                if (mAliyunVodPlayer == null) {
                    return;
                }
                //准备成功之后可以调用start方法开始播放
                if (mOutPreparedListener != null) {
                    mOutPreparedListener.onPrepared();
                }
            }
        });
        //播放器出错监听
        mAliyunVodPlayer.setOnErrorListener(new IAliyunVodPlayer.OnErrorListener() {
            @Override
            public void onError(int errorCode, int errorEvent, String errorMsg) {
                if (errorCode == AliyunErrorCode.ALIVC_ERR_INVALID_INPUTFILE.getCode()) {
                    //当播放本地报错4003的时候，可能是文件地址不对，也有可能是没有权限。
                    //如果是没有权限导致的，就做一个权限的错误提示。其他还是正常提示：
                    int storagePermissionRet = ContextCompat.checkSelfPermission(
                            AliyunPlayerView.this.getContext().getApplicationContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (storagePermissionRet != PackageManager.PERMISSION_GRANTED) {
                        errorMsg = AliyunErrorCode.ALIVC_ERR_NO_STORAGE_PERMISSION.getDescription(getContext());
                    } else if (!NetWatchdog.hasNet(getContext())) {
                        //也可能是网络不行
                        errorCode = AliyunErrorCode.ALIVC_ERR_NO_NETWORK.getCode();
                        errorMsg = AliyunErrorCode.ALIVC_ERR_NO_NETWORK.getDescription(getContext());
                    }
                }

                if (mOutErrorListener != null) {
                    mOutErrorListener.onError(errorCode, errorEvent, errorMsg);
                }

            }
        });

        //播放器加载回调
        mAliyunVodPlayer.setOnLoadingListener(new IAliyunVodPlayer.OnLoadingListener() {
            @Override
            public void onLoadStart() {
            }

            @Override
            public void onLoadEnd() {
            }

            @Override
            public void onLoadProgress(int percent) {
            }
        });
        //播放结束
        mAliyunVodPlayer.setOnCompletionListener(new IAliyunVodPlayer.OnCompletionListener() {
            @Override
            public void onCompletion() {
                if (mOutCompletionListener != null) {
                    mOutCompletionListener.onCompletion();
                }
            }
        });
        mAliyunVodPlayer.setOnBufferingUpdateListener(new IAliyunVodPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(int percent) {
                mCurrentBufferPercentage = percent;
            }
        });
        //播放信息监听
        mAliyunVodPlayer.setOnInfoListener(new IAliyunVodPlayer.OnInfoListener() {
            @Override
            public void onInfo(int arg0, int arg1) {
                if (mOutInfoListener != null) {
                    mOutInfoListener.onInfo(arg0, arg1);
                }
            }
        });

        //自动播放
        mAliyunVodPlayer.setOnAutoPlayListener(new IAliyunVodPlayer.OnAutoPlayListener() {
            @Override
            public void onAutoPlayStarted() {
                //自动播放开始,需要设置播放状态
                if (mOutAutoPlayListener != null) {
                    mOutAutoPlayListener.onAutoPlayStarted();
                }
            }
        });
        //PCM原始数据监听
        mAliyunVodPlayer.setOnPcmDataListener(new IAliyunVodPlayer.OnPcmDataListener() {
            @Override
            public void onPcmData(byte[] data, int size) {
                if (mOutPcmDataListener != null) {
                    mOutPcmDataListener.onPcmData(data, size);
                }
            }
        });
        //url过期监听
        mAliyunVodPlayer.setOnUrlTimeExpiredListener(new IAliyunVodPlayer.OnUrlTimeExpiredListener() {
            @Override
            public void onUrlTimeExpired(String vid, String quality) {
                System.out.println("abc : onUrlTimeExpired");
                if (mOutUrlTimeExpiredListener != null) {
                    mOutUrlTimeExpiredListener.onUrlTimeExpired(vid, quality);
                }
            }
        });

        //请求源过期信息
        mAliyunVodPlayer.setOnTimeExpiredErrorListener(new IAliyunVodPlayer.OnTimeExpiredErrorListener() {
            @Override
            public void onTimeExpiredError() {
                System.out.println("abc : onTimeExpiredError");
            }
        });

        mAliyunVodPlayer.setDisplay(mSurfaceView.getHolder());
    }

    /**
     * 获取整体缓冲进度
     *
     * @return 整体缓冲进度
     */
    public int getBufferPercentage() {
        if (mAliyunVodPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }



    /**
     * 获取视频时长
     *
     * @return 视频时长
     */
    public int getDuration() {
        if (mAliyunVodPlayer != null && mAliyunVodPlayer.isPlaying()) {
            return (int) mAliyunVodPlayer.getDuration();
        }

        return 0;
    }

    /**
     * 获取当前位置
     *
     * @return 当前位置
     */
    public int getCurrentPosition() {
        if (mAliyunVodPlayer != null && mAliyunVodPlayer.isPlaying()) {
            return (int) mAliyunVodPlayer.getCurrentPosition();
        }

        return 0;
    }

    /**
     * addSubView 添加子view到布局中
     *
     * @param view 子view
     */
    private void addSubView(View view) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(view, params);//添加到布局中
    }

    /**
     * 设置准备事件监听
     *
     * @param onPreparedListener 准备事件
     */
    public void setOnPreparedListener(IAliyunVodPlayer.OnPreparedListener onPreparedListener) {
        mOutPreparedListener = onPreparedListener;
    }

    /**
     * 设置错误事件监听
     *
     * @param onErrorListener 错误事件监听
     */
    public void setOnErrorListener(IAliyunVodPlayer.OnErrorListener onErrorListener) {
        mOutErrorListener = onErrorListener;
    }

    /**
     * 设置信息事件监听
     *
     * @param onInfoListener 信息事件监听
     */
    public void setOnInfoListener(IAliyunVodPlayer.OnInfoListener onInfoListener) {
        mOutInfoListener = onInfoListener;
    }

    /**
     * 设置播放完成事件监听
     *
     * @param onCompletionListener 播放完成事件监听
     */
    public void setOnCompletionListener(IAliyunVodPlayer.OnCompletionListener onCompletionListener) {
        mOutCompletionListener = onCompletionListener;
    }


    /**
     * 设置自动播放事件监听
     *
     * @param l 自动播放事件监听
     */
    public void setOnAutoPlayListener(IAliyunVodPlayer.OnAutoPlayListener l) {
        mOutAutoPlayListener = l;
    }

    /**
     * 设置PCM数据监听
     *
     * @param l PCM数据监听
     */
    public void setOnPcmDataListener(IAliyunVodPlayer.OnPcmDataListener l) {
        mOutPcmDataListener = l;
    }


    /**
     * 设置鉴权过期监听，在鉴权过期前一分钟回调
     *
     * @param listener
     */
    public void setOnUrlTimeExpiredListener(IAliyunVodPlayer.OnUrlTimeExpiredListener listener) {
        this.mOutUrlTimeExpiredListener = listener;
    }


    /**
     * 设置停止播放监听
     *
     * @param onStoppedListener 停止播放监听
     */
    public void setOnStoppedListener(IAliyunVodPlayer.OnStoppedListener onStoppedListener) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setOnStoppedListner(onStoppedListener);
        }
    }

    /**
     * 设置加载状态监听
     *
     * @param onLoadingListener 加载状态监听
     */
    public void setOnLoadingListener(IAliyunVodPlayer.OnLoadingListener onLoadingListener) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setOnLoadingListener(onLoadingListener);
        }
    }

    /**
     * 设置缓冲监听
     *
     * @param onBufferingUpdateListener 缓冲监听
     */
    public void setOnBufferingUpdateListener(IAliyunVodPlayer.OnBufferingUpdateListener onBufferingUpdateListener) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setOnBufferingUpdateListener(onBufferingUpdateListener);
        }
    }

    /**
     * 设置视频宽高变化监听
     *
     * @param onVideoSizeChangedListener 视频宽高变化监听
     */
    public void setOnVideoSizeChangedListener(IAliyunVodPlayer.OnVideoSizeChangedListener onVideoSizeChangedListener) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setOnVideoSizeChangedListener(onVideoSizeChangedListener);
        }
    }

    /**
     * 设置循环播放开始监听
     *
     * @param onCircleStartListener 循环播放开始监听
     */
    public void setOnCircleStartListener(IAliyunVodPlayer.OnCircleStartListener onCircleStartListener) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setOnCircleStartListener(onCircleStartListener);
        }
    }

    /**
     * 设置本地播放源
     */
    public void setLocalSource(String url, String title) {
        if (mAliyunVodPlayer == null) {
            return;
        }
        AliyunLocalSource.AliyunLocalSourceBuilder alsb = new AliyunLocalSource.AliyunLocalSourceBuilder();
        alsb.setSource(url);
        alsb.setTitle(title);
        reset();
        prepareLocalSource(alsb.build());
    }

    /**
     * prepare本地播放源
     *
     * @param aliyunLocalSource 本地播放源
     */
    private void prepareLocalSource(AliyunLocalSource aliyunLocalSource) {
        System.out.println("abc : prepared = " + aliyunLocalSource.getSource() + " --- " + mAliyunVodPlayer.getPlayerState());
        mAliyunVodPlayer.prepareAsync(aliyunLocalSource);
        System.out.println("abc : preparedAfter = " + mAliyunVodPlayer.getPlayerState());
    }

    /**
     * 设置边播边存
     *
     * @param enable      是否开启。开启之后会根据maxDuration和maxSize决定有无缓存。
     * @param saveDir     保存目录
     * @param maxDuration 单个文件最大时长 秒
     * @param maxSize     所有文件最大大小 MB
     */
    public void setPlayingCache(boolean enable, String saveDir, int maxDuration, long maxSize) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setPlayingCache(enable, saveDir, maxDuration, maxSize);
        }
    }

    /**
     * 设置缩放模式
     *
     * @param scallingMode 缩放模式
     */
    public void setVideoScalingMode(IAliyunVodPlayer.VideoScalingMode scallingMode) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setVideoScalingMode(scallingMode);
        }
    }

    /**
     * 获取媒体信息
     *
     * @return 媒体信息
     */
    public AliyunMediaInfo getMediaInfo() {
        if (mAliyunVodPlayer != null) {
            return mAliyunVodPlayer.getMediaInfo();
        }

        return null;
    }

    /**
     * 活动销毁，释放
     */
    public void onDestroy() {
        stop();
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.release();
        }

        mSurfaceView = null;
        mAliyunVodPlayer = null;
    }

    /**
     * 是否处于播放状态：start或者pause了
     *
     * @return 是否处于播放状态
     */
    public boolean isPlaying() {
        if (mAliyunVodPlayer != null) {
            return mAliyunVodPlayer.isPlaying();
        }
        return false;
    }

    /**
     * 获取播放器状态
     *
     * @return 播放器状态
     */
    public PlayerState getPlayerState() {
        if (mAliyunVodPlayer != null) {
            return mAliyunVodPlayer.getPlayerState();
        }
        return null;
    }

    /**
     * 开始播放
     */
    public void start() {
        if (mAliyunVodPlayer == null) {
            return;
        }

        PlayerState playerState = mAliyunVodPlayer.getPlayerState();
        if (playerState == PlayerState.Paused || playerState == PlayerState.Prepared || mAliyunVodPlayer.isPlaying()) {
            mAliyunVodPlayer.start();
            if (mOnVideoStartPlayListener != null) {
                mOnVideoStartPlayListener.startPlay();
            }
        }

    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (mAliyunVodPlayer == null) {
            return;
        }

        PlayerState playerState = mAliyunVodPlayer.getPlayerState();
        if (playerState == PlayerState.Started || mAliyunVodPlayer.isPlaying()) {
            mAliyunVodPlayer.pause();
        }
    }

    /**
     * 停止播放
     */
    private void stop() {
        if (mAliyunVodPlayer != null ) {
            mAliyunVodPlayer.stop();
        }
    }

    /**
     * 开启底层日志
     */
    public void enableNativeLog() {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.enableNativeLog();
        }
    }

    /**
     * 关闭底层日志
     */
    public void disableNativeLog() {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.disableNativeLog();
        }
    }

    /**
     * 设置线程池
     *
     * @param executorService 线程池
     */
    public void setThreadExecutorService(ExecutorService executorService) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setThreadExecutorService(executorService);
        }
    }

    /**
     * 获取SDK版本号
     *
     * @return SDK版本号
     */
    public String getSDKVersion() {
        return AliyunVodPlayer.getSDKVersion();
    }

    /**
     * 获取播放surfaceView
     *
     * @return 播放surfaceView
     */
    public SurfaceView getPlayerView() {
        return mSurfaceView;
    }

    /**
     * 设置自动播放
     *
     * @param auto true 自动播放
     */
    public void setAutoPlay(boolean auto) {
        if (mAliyunVodPlayer != null) {
            mAliyunVodPlayer.setAutoPlay(auto);
        }
    }


    private OnVideoStartPlayListener mOnVideoStartPlayListener;

    public void setOnVideoStartPlayListener(OnVideoStartPlayListener listener) {
        this.mOnVideoStartPlayListener = listener;
    }

    /**
     * 开始播放监听
     */
    public interface OnVideoStartPlayListener {
        void startPlay();
    }

}
