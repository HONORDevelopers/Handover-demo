/*
 * Copyright (c) Honor Device Co., Ltd. 2022-2023. All rights reserved.
 */
package com.example.handoverdemo;

import static com.example.handoverdemo.HandoverManager.NODE_ID;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * 接续数据中转Activity
 *
 * @since 2022-07-24
 */
public class RecDataActivity extends Activity {
    private static final String TAG = "HandoverDemo-RecDataActivity";

    private static final String LAUNCH_ACTION = "com.hihonor.handover.ACTION_LAUNCH_APP";

    private static final String HANDOVER_ACTION = "com.hihonor.handover.ACTION_APP_DATA_HANDOVER";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (LAUNCH_ACTION.equals(intent.getAction())) {
            // TODO 当用户点击Sink端的接续触点时接续框架会先通过此Action拉起应用Activity，
            //  若是应用是进行的文件接续或者有其他耗时处理，可以在此处进行展示加载动画等相关操作;
            //  若是应用本身需要进行同账号校验等操作，则可以在此处通过获取到的对端设备ID，发送NormalMsg到Source端进行校验，
            //  Source检验成功后再发送ContinuityMsg或者ContinuityFile
            String peerNodeId = intent.getStringExtra(NODE_ID);
            Log.i(TAG, "processIntent: Show loading dialog for wait continuity file or " +
                    "continuity msg, peer device nodeId:" + peerNodeId);
        } else if (HANDOVER_ACTION.equals(intent.getAction())) {
            /*
             * 收到接续数据，跳转实际展示Activity解析接续数据
             */
            Intent mainIntent = new Intent(intent);
            mainIntent.setClass(this, MainActivity.class);
            startActivity(mainIntent);
        }
        finish();
    }
}
