/*
 * Copyright (c) Honor Device Co., Ltd. 2022-2023. All rights reserved.
 */
package com.example.handoverdemo;

import com.hihonor.android.magicx.connect.handover.HandoverSdk;
import com.hihonor.android.magicx.connect.handover.IHandoverSdkCallback;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Optional;

/**
 * 接续能力管理类
 *
 * @since 2022-07-24
 */
public class HandoverManager {
    public static final int HANDOVER_STATE_AVAILABLE = 0;

    public static final int HANDOVER_STATE_UNAVAILABLE = -1;

    public static final String MSG_TYPE = "msgType";

    public static final String DATA_CONTENT = "dataContent";

    public static final String NODE_ID = "nodeId";

    public static final String ELIGIBILITY = "eligibility";

    public static final String ERROR_CODE = "errorCode";

    public static final String ONLINE_DEV_NUM= "onlineDevNum";

    private static final String TAG = "HandoverDemo-HandoverManager";

    private static volatile HandoverManager sInstance;

    private static HandoverSdk sHandoverSdk;

    private Context mContext;

    private HandoverManager(Context context){
        this.mContext = context;
    }

    /**
     * 获得HandoverSdkDelegate单例对象
     *
     * @return HandoverSdkDelegate单例对象
     */
    public static synchronized HandoverManager getInstance(Context context){
        if (sInstance == null) {
            sInstance = new HandoverManager(context);
        }
        return sInstance;
    }

    /**
     * 注册接续框架
     *
     * @param callback 接续消息接收回调
     */
    public void init(IHandoverSdkCallback callback) {
        Log.i(TAG, "Handover SDK init");
        sHandoverSdk = HandoverSdk.getInstance();
        sHandoverSdk.registerHandover(mContext, callback);
    }

    /**
     * 反注册接续框架
     */
    public void deInit() {
        sHandoverSdk.unregisterHandover(mContext);
    }

    /**
     * 发送接续广播
     */
    public void sendContinuityBroadcast() {
        JSONObject jsonObject = new JSONObject();
        try {
            Optional<JSONObject> jsonOptional = getEligibilityJson();
            if (!jsonOptional.isPresent()) {
                Log.e(TAG, "sendContinuityBroadcast: get eligibility json failed.");
                return;
            }
            jsonObject.put(MSG_TYPE, HandoverSdk.MsgType.CONTINUITY_BROADCAST);
            jsonObject.put(ELIGIBILITY, jsonOptional.get());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "sendContinuityBroadcast:" + jsonObject);
        sHandoverSdk.handoverSend(mContext, jsonObject);
    }

    /**
     * 从assets的预置资源中读取能力协商策略Json，该Json用于配置要通知哪些类型设备显示接续的触点，以及对应设备上什么应用能接续本应用的内容
     * 该JSON中相关字段参数的具体说明请到荣耀开发者网站接续服务接入说明的相关章节进行查询
     *
     * @return 能力协商策略Json对象
     */
    private Optional<JSONObject> getEligibilityJson() {
        String eligibilityJsonStr = CommonUtils.getJsonStrFromAssets(mContext, "eligibility_demo.json");
        if (TextUtils.isEmpty(eligibilityJsonStr)) {
            Log.e(TAG, "getEligibilityJson: get json string from assets failed.");
            return Optional.empty();
        }
        try {
            JSONObject jsonObject = new JSONObject(eligibilityJsonStr);
            return Optional.of(jsonObject);
        } catch (JSONException e) {
            Log.e(TAG, "getEligibilityJson: catch JSONException.");
        }
        return Optional.empty();
    }

    /**
     * 发送接续消息, 对端收到此消息后会拉起三方App
     *
     * @param peerNodeId 对端设备ID
     * @param msgStr 要发送的接续消息内容
     */
    public void sendContinuityMsg(String peerNodeId, String msgStr) {
        if (TextUtils.isEmpty(peerNodeId) || TextUtils.isEmpty(msgStr)) {
            Log.e(TAG, "sendContinuityMsg: param is null.");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(MSG_TYPE, HandoverSdk.MsgType.CONTINUITY_MSG);
            jsonObject.put(DATA_CONTENT, msgStr);
            // 发送接续消息时需要指定Sink端NodeId, 此ID从onDataEvent回调中获取
            jsonObject.put(NODE_ID, peerNodeId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "sendContinuityMsg: " + jsonObject.toString());
        sHandoverSdk.handoverSend(mContext, jsonObject);
    }

    /**
     * 发送接续文件,对端收到改文件后会拉起三方App
     *
     * @param peerNodeId 对端设备ID
     * @param filePath 接续文件的路径
     */
    public void sendContinuityFile(String peerNodeId, String filePath) {
        if (TextUtils.isEmpty(peerNodeId) || TextUtils.isEmpty(filePath)) {
            Log.e(TAG, "sendContinuityFile: param is null.");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(MSG_TYPE, HandoverSdk.MsgType.CONTINUITY_FILE);
            // 发送接续消息时需要指定Sink端NodeId, 此ID从onDataEvent回调中获取
            jsonObject.put(NODE_ID, peerNodeId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        File file = new File(filePath);
        if(!file.exists()) {
            Log.e(TAG, "sendContinuityFile: file is not exit.");
        }
        Uri uri = FileProvider.getUriForFile(mContext,"com.example.handoverdemo.provider", file);
        Log.i(TAG, "sendContinuityFile: Uri:" + uri);
        sHandoverSdk.handoverSend(mContext, jsonObject, uri);
    }

    /**
     * 发送接续结果给Source端
     *
     * @param peerNodeId Source端设备ID
     * @param isSuccess 接续是否成功
     */
    public void continuityResultFeedback(String peerNodeId, boolean isSuccess) {
        if (TextUtils.isEmpty(peerNodeId)) {
            Log.e(TAG, "continuityResultFeedback: param is null.");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            String result = isSuccess ? (HandoverSdk.ContinuityResult.SUCCESS.toString())
                    : (HandoverSdk.ContinuityResult.FAILED.toString());
            jsonObject.put(MSG_TYPE, HandoverSdk.MsgType.CONTINUITY_RESULT_FEEDBACK);
            jsonObject.put(DATA_CONTENT, result);
            jsonObject.put(NODE_ID, peerNodeId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "continuityResultFeedback: " + jsonObject.toString());
        sHandoverSdk.handoverSend(mContext, jsonObject);
    }

    /**
     * 发送一般消息, 需要对端也注册接续框架后才能收到此消息, 此消息通过onDataEvent回调接收
     *
     * @param peerNodeId 对端设备ID
     * @param normalMsgStr 消息内容
     */
    public void sendNormalMsg(String peerNodeId, String normalMsgStr) {
        if (TextUtils.isEmpty(peerNodeId) || TextUtils.isEmpty(normalMsgStr)) {
            Log.e(TAG, "sendNormalMsg: param is null.");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(MSG_TYPE, HandoverSdk.MsgType.NORMAL_MSG);
            jsonObject.put(DATA_CONTENT, normalMsgStr);
            jsonObject.put(NODE_ID, peerNodeId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "sendNormalMsg: " + jsonObject.toString());
        sHandoverSdk.handoverSend(mContext, jsonObject);
    }

    /**
     * 发送取消广播
     */
    public void sendStopBroadcast() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(MSG_TYPE, HandoverSdk.MsgType.CONTINUITY_STOP_BROADCAST);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "sendStopReq: " + jsonObject.toString());
        sHandoverSdk.handoverSend(mContext, jsonObject);
    }
}
