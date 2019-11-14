package com.aliplayer.view.control;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.aliplayer.AliyunScreenMode;
import com.aliplayer.AliyunVodPlayerView;
import com.aliplayer.R;
import com.aliplayer.theme.ITheme;
import com.aliplayer.utils.ScreenUtils;
import com.aliplayer.utils.TimeFormater;
import com.aliplayer.view.interfaces.ViewAction;
import com.aliyun.vodplayer.media.AliyunMediaInfo;

import java.lang.ref.WeakReference;
import java.util.List;


/*
 * Copyright (C) 2010-2018 Alibaba Group Holding Limited.
 */

/**
 * 控制条界面。包括了顶部的标题栏，底部 的控制栏，锁屏按钮等等。是界面的主要组成部分。
 */

public class HsControlView extends  ControlView{

    private int bottomMargin;

    public HsControlView(Context context) {
        super(context);
    }

    public HsControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HsControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void updateControlBarMargin() {
        //调节高度
        if (mAliyunScreenMode == AliyunScreenMode.Full) {
            margin(mControlBar, 0, 0, 0, 0);
        } else {
            margin(mControlBar, 0, 0, 0, getBottomMargin());
        }
    }

    public int getBottomMargin() {
        if (bottomMargin == 0) {
            bottomMargin = (int) (ScreenUtils.getWidth(getContext()) * 1.0f * 128 / 750);
        }
        return bottomMargin;
    }

}