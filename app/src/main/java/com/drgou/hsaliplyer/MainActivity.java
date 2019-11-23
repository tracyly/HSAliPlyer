package com.drgou.hsaliplyer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.aliplayer.AliyunVodPlayerView;

public class MainActivity extends AppCompatActivity {

    private String videoUrl = "https://video.huasheng100.com/9cfe3cb6007442dd87b24f33fe4a3bf8/dae07f2d582e4b3883333cc33d8cf37d-6bfe22c9c1bbedc7064a4e01d7c79590-ld.mp4";
    private AliyunVodPlayerView mAliVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAliVideoView =  findViewById(R.id.ali_video_view);


        //播放非加密视频
        mAliVideoView.setControlBarCanShow(true);
        mAliVideoView.setCirclePlay(true);
        //mAliVideoView.setVideoScalingMode(IAliyunVodPlayer.VideoScalingMode.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        mAliVideoView.setLocalSource(videoUrl, "title");
        mAliVideoView.setAutoPlay(true);
    }
}
