package com.aliplayer;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.aliplayer.constants.PlayParameter;
import com.aliplayer.utils.FixedToastUtils;
import com.aliplayer.view.control.ControlView;
import com.aliplayer.view.control.HsControlView;

import java.util.List;



/*
 * Copyright (C) 2010-2018 Alibaba Group Holding Limited.
 */

/**
 * 控制条添加底部间距
 */
public class HsVodPlayerView extends AliyunVodPlayerView {

    public HsVodPlayerView(Context context) {
        super(context);
    }

    public HsVodPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HsVodPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected ControlView getControlView() {
        return new HsControlView(getContext());
    }
}
