/*
 * Copyright (c) Honor Device Co., Ltd. 2022-2023. All rights reserved.
 */
package com.example.handoverdemo;

import static com.example.handoverdemo.HandoverManager.DATA_CONTENT;
import static com.example.handoverdemo.HandoverManager.ERROR_CODE;
import static com.example.handoverdemo.HandoverManager.HANDOVER_STATE_AVAILABLE;
import static com.example.handoverdemo.HandoverManager.HANDOVER_STATE_UNAVAILABLE;
import static com.example.handoverdemo.HandoverManager.MSG_TYPE;
import static com.example.handoverdemo.HandoverManager.NODE_ID;
import static com.example.handoverdemo.HandoverManager.ONLINE_DEV_NUM;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.hihonor.android.magicx.connect.handover.HandoverSdk;
import com.hihonor.android.magicx.connect.handover.IHandoverSdkCallback;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 应用接续演示Demo主界面Activity
 *
 * @since 2022-07-24
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HandoverDemo-MainActivity";

    private static final String ACTION_HANDOVER_DATA_TRANS = "com.hihonor.handover.ACTION_APP_DATA_HANDOVER";

    public static final String DEFAULT_CONTINUITY_MSG_STRING = "接续测试消息";

    private static final int HAND_CHANGE_TEXT_VIEW_EVENT = 1;

    private static final int HAND_SEND_CONTINUITY_FEEDBACK_EVENT = 2;

    private static final long FEEDBACK_DELAY_TIME = 1000;

    private static final int REC_CONTINUITY_DATA_SUCCESS = 1;

    private static final int REC_CONTINUITY_DATA_FAILED = 0;

    public TextView textView;

    public ImageView imageView;

    public EditText inputView;

    private Handler mHandler;

    private HandlerThread mHandlerThread;

    private HandoverManager mHandoverManager;

    private int mHandoverServiceState = HANDOVER_STATE_UNAVAILABLE;

    private boolean mIsSendBroadcastForTransFile = false;

    private String mPeerNodeId;

    private String mPresetPicturePath;

    private int mInitHandoverCount;

    private IHandoverSdkCallback mHandoverSdkCallback = new IHandoverSdkCallback() {
        @Override
        public void onStateChg(int state) {
            // TODO 此回调上报的是接续框架是否可用的状态值，当state为0代表接续服务可以使用，
            //  当state为-1代表接续服务不可用（接续框架初始化失败/长时间无数据收发导致接续服务退出等）
            mHandoverServiceState = state;
            if (mHandoverServiceState == HANDOVER_STATE_AVAILABLE) {
                mInitHandoverCount = 0;
            } else {
                initHandoverService();
            }
        }

        @Override
        public void onDataEvent(String data) {
            try {
                Log.i(TAG, "onDataEvent: " + data);
                JSONObject jsonObject = new JSONObject(data);
                String msgType = jsonObject.getString(MSG_TYPE);
                String nodeId = jsonObject.has(NODE_ID) ? jsonObject.getString(NODE_ID) : "";
                if (msgType.equals(HandoverSdk.MsgType.CONTINUITY_REQUEST.toString())) {
                    // 收到对端设备点击接续触点（接续提示图标）后发送的接续请求信令
                    mPeerNodeId = nodeId;
                    onRecContinuityRequest(mPeerNodeId);
                } else if (msgType.equals(HandoverSdk.MsgType.NORMAL_MSG.toString())) {
                    // 收到对端发送来的NORMAL_MSG消息
                    mPeerNodeId = nodeId;
                    String normalMsg = jsonObject.getString(DATA_CONTENT);
                    String showStr = "Receive NORMAL_MSG: " + normalMsg;
                    mHandler.sendMessage(mHandler.obtainMessage(HAND_CHANGE_TEXT_VIEW_EVENT, showStr));
                } else if (msgType.equals(HandoverSdk.MsgType.CONTINUITY_RESULT_FEEDBACK.toString())) {
                    // 收到对端发送来的接续结果反馈，需要对端应用主动发送CONTINUITY_RESULT_FEEDBACK消息
                    String continuityResult = jsonObject.getString(DATA_CONTENT);
                    String showStr = "Receive continuity result: " + continuityResult + " from peer device.";
                    mHandler.sendMessage(mHandler.obtainMessage(HAND_CHANGE_TEXT_VIEW_EVENT, showStr));
                } else if (msgType.equals(HandoverSdk.MsgType.CONTINUITY_FAILED.toString())) {
                    // 收到异常通知消息，具体错误码定义请参考上架文档相关章节描述
                    String errorInfo = jsonObject.getString(DATA_CONTENT);
                    int errorCode = jsonObject.getInt(ERROR_CODE);
                    String showStr = "ErrorInfo: " + errorInfo + " ErrorCode: " + errorCode;
                    mHandler.sendMessage(mHandler.obtainMessage(HAND_CHANGE_TEXT_VIEW_EVENT, showStr));
                } else if (msgType.equals(HandoverSdk.MsgType.CONTINUITY_DEVICE_EVENT.toString())) {
                    // 收到设备上下线数量变化通知消息
                    String deviceEvent = jsonObject.getString(DATA_CONTENT);
                    int onlineDevNum = jsonObject.getInt(ONLINE_DEV_NUM);
                    String showStr = deviceEvent + ", current online device number: " + onlineDevNum;
                    Log.i(TAG, "onDataEvent: " + showStr);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPresetPicturePath = CommonUtils.extractPresetPictureToLocalPath(this);
        initView();
        initHandler();
        // 接续服务初始化
        initHandoverService();
        Log.i(TAG, "onCreate");
        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.i(TAG, "onNewIntent");
        processIntent(intent);
    }

    /**
     * 初始化Handler
     */
    private void initHandler() {
        mHandlerThread = new HandlerThread( "handler-thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case HAND_CHANGE_TEXT_VIEW_EVENT:
                        String showStr = (String) msg.obj;
                        if (!TextUtils.isEmpty(showStr)) {
                            textView.setText(showStr);
                        }
                        break;
                    case HAND_SEND_CONTINUITY_FEEDBACK_EVENT:
                        // 发送接续结果反馈信令
                        mHandoverManager.continuityResultFeedback((String) msg.obj,
                                msg.arg1 == REC_CONTINUITY_DATA_SUCCESS);
                        break;
                    default:
                        Log.w(TAG, "unknown msg.what = " + msg.what);
                        break;
                }
            }
        };
    }

    /**
     * view初始化
     */
    private void initView() {
        textView = findViewById(R.id.info_textview);
        imageView = findViewById(R.id.show_view);
        inputView = findViewById(R.id.input_text_view);
    }

    /**
     * 初始化接续服务，注意若是因为当前设备不支持等原因导致接续服务一直初始化失败的话不要频繁初始化
     */
    private void initHandoverService() {
        if (mInitHandoverCount < 3) {
            if (mHandoverManager == null) {
                mHandoverManager = HandoverManager.getInstance(this);
            }
            mHandoverManager.init(mHandoverSdkCallback);
            mInitHandoverCount ++;
        }
    }

    /**
     * 判断当前接续服务是否可用
     *
     * @return true:接续服务可用 false:接续服务当前不可用
     */
    private boolean isHandoverServiceStateAvailable() {
        if (mHandoverServiceState == HANDOVER_STATE_AVAILABLE) {
            return true;
        }
        // 若是当前接续服务的状态不可用的话则重新进行初始化
        initHandoverService();
        Log.w(TAG, "isHandoverServiceStateAvailable: handover service is not available.");
        return false;
    }

    /**
     * 接续消息处理
     *
     * @param intent intent
     */
    private void processIntent(Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(ACTION_HANDOVER_DATA_TRANS)) {
            int recContinuityDataFlag = REC_CONTINUITY_DATA_FAILED;
            mPeerNodeId = intent.getStringExtra(NODE_ID);
            if (intent.hasExtra(DATA_CONTENT)) {
                // 收到接续消息，应用可根据实际情况进行相应处理
                String continuityMsg = getIntent().getStringExtra(DATA_CONTENT);
                Log.i(TAG, "processIntent: CONTINUITY_MSG:" + continuityMsg);
                textView.setText(continuityMsg);
                recContinuityDataFlag = REC_CONTINUITY_DATA_SUCCESS;
            } else if (intent.getData() != null) {
                // 收到接续文件，应用可根据实际情况进行相应处理
                Uri uri = getIntent().getData();
                String imgSavePath = getExternalFilesDir(null).getPath() + "/ContinuityFile.jpg";
                CommonUtils.copyUriFile(this, uri, imgSavePath);
                Log.i(TAG, "processIntent: CONTINUITY_FILE:" + imgSavePath);
                Bitmap bm = BitmapFactory.decodeFile(imgSavePath);
                imageView.setImageBitmap(bm);
                recContinuityDataFlag = REC_CONTINUITY_DATA_SUCCESS;
            } else {
                Log.w(TAG, "processIntent: don't receive any continuity data.");
            }
            // 1.接续成功后需要及时通知框架接续成功，收到接续反馈后框架会将结果返回给Source端
            // 2.注册接续框架后等待onStateChg回调结果为0后再进行通知
            // 3.如果是文件接续，框架收到此反馈后会进行删除接续缓存文件等操作，请完成文件转存操作再进行反馈
            Message msg = new Message();
            msg.what = HAND_SEND_CONTINUITY_FEEDBACK_EVENT;
            msg.arg1 = recContinuityDataFlag;
            msg.obj = mPeerNodeId;
            mHandler.sendMessageDelayed(msg, FEEDBACK_DELAY_TIME);
        }
    }

    /**
     * 收到对端设备点击接续触点（接续提示图标）后发送的接续请求信令，可根据业务需求发送接续消息或接续文件
     *
     * @param peerNodeId view
     */
    private void onRecContinuityRequest(String peerNodeId) {
        if (TextUtils.isEmpty(peerNodeId)) {
            Log.e(TAG, "onRecContinuityRequest: peerNodeId is null.");
            return;
        }
        Log.i(TAG, "onRecContinuityRequest: Receive ContinuityRequest from device: " + peerNodeId);
        if (!mIsSendBroadcastForTransFile) {
            // 发送接续消息
            String inputStr = TextUtils.isEmpty(inputView.getText().toString()) ?
                    DEFAULT_CONTINUITY_MSG_STRING : inputView.getText().toString();
            mHandoverManager.sendContinuityMsg(mPeerNodeId, inputStr);
        } else {
            // 发送接续文件
            mHandoverManager.sendContinuityFile(mPeerNodeId, mPresetPicturePath);
        }
    }

    /**
     * 处理发送接续广播(消息)按钮的点击事件
     *
     * @param view view
     */
    public void onSendMsgBroadcastButtonClicked(View view) {
        if (!isHandoverServiceStateAvailable()) {
            return;
        }
        mIsSendBroadcastForTransFile = false;
        mHandoverManager.sendContinuityBroadcast();
    }

    /**
     * 处理发送接续广播(文件)按钮的点击事件
     *
     * @param view view
     */
    public void onSendFileBroadcastButtonClicked(View view) {
        if (!isHandoverServiceStateAvailable()) {
            return;
        }
        mIsSendBroadcastForTransFile = true;
        mHandoverManager.sendContinuityBroadcast();
    }

    /**
     * 处理取消接续广播按钮的点击事件
     *
     * @param view view
     */
    public void onSendStopBroadcastButtonClicked(View view) {
        if (!isHandoverServiceStateAvailable()) {
            return;
        }
        mHandoverManager.sendStopBroadcast();
    }

    /**
     * 处理发送NormalMsg按钮的点击事件
     *
     * @param view view
     */
    public void onSendNormalMsgButtonClicked(View view) {
        if (!isHandoverServiceStateAvailable()) {
            return;
        }
        mHandoverManager.sendNormalMsg(mPeerNodeId, inputView.getText().toString());
    }

    @Override
    protected void onDestroy() {
        // 进行反注册操作
        mHandoverManager.deInit();
        super.onDestroy();
    }
}