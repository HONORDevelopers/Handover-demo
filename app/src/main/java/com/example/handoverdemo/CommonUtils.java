/*
 * Copyright (c) Honor Device Co., Ltd. 2022-2023. All rights reserved.
 */
package com.example.handoverdemo;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * 工具类
 *
 * @since 2022-07-24
 */
public class CommonUtils {
    private static final String TAG = "CommonUtils";

    private static final int RESULT_OK = 0;

    private static final int RESULT_ERR = -1;

    /**
     * 对Uri中的文件进行转存
     *
     * @param context 上下文
     * @param uri 原始文件Uri
     * @param savePath 要转存的路径
     * @return RESULT_OK(0)：成功  RESULT_ERR(-1)：失败
     */
    public static int copyUriFile(Context context, Uri uri, String savePath) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        int ret = RESULT_OK;
        try {
            File saveFile = new File(savePath);
            if (!saveFile.exists()) {
                if (!saveFile.createNewFile()) {
                    Log.e(TAG, "copyUriFile: saveFile create failed.");
                    return RESULT_ERR;
                }
            }
            if (!saveFile.isFile()) {
                Log.e(TAG, "copyUriFile: savePath is not point to a file.");
                return RESULT_ERR;
            }
            inputStream = context.getContentResolver().openInputStream(uri);
            outputStream = new FileOutputStream(saveFile);
            byte[] buff = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(buff)) >= 0) {
                outputStream.write(buff, 0, len);
            }
            ret = RESULT_OK;
        } catch (IOException e) {
            Log.e(TAG, "copyUriFile: catch IOException.");
            ret = RESULT_ERR;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "copyUriFile: catch IOException when close inputStream.");
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "copyUriFile: catch IOException when close outputStream.");
                }
            }
        }
        return ret;
    }

    /**
     * 提取Assets中Json数据
     *
     * @param context 上下文
     * @param fileName 文件名
     * @return Json数据
     */
    public static String getJsonStrFromAssets(Context context, String fileName) {
        if (context == null || TextUtils.isEmpty(fileName)) {
            Log.e(TAG, "getJsonStrFromAssets: param is null.");
            return "";
        }
        InputStream inputStream = null;
        Writer writer = null;
        Reader reader = null;
        String jsonString = "";
        try {
            inputStream = context.getAssets().open(fileName);
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            writer = new StringWriter();
            int byteRead;
            char[] buffer = new char[1024];
            while ((byteRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, byteRead);
            }
            jsonString = writer.toString();
        } catch (IOException e) {
            Log.e(TAG, "getJsonStrFromAssets: catch IOException.");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "getJsonStrFromAssets: catch IOException " +
                            "when close inputStream.");
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "getJsonStrFromAssets: catch IOException " +
                            "when close reader.");
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.e(TAG, "getJsonStrFromAssets: catch IOException " +
                            "when close writer.");
                }
            }
        }
        return jsonString;
    }

    /**
     * 将Assets中预置的测试图片提取到本地路径中
     *
     * @param context 上下文
     * @return 测试图片提取到设备本地后的路径
     */
    public static String extractPresetPictureToLocalPath(Context context) {
        if (context == null) {
            Log.e(TAG, "extractPresetPictureToLocalPath: context is null.");
            return "";
        }
        String path = context.getFilesDir().getPath() + "/HonorLogo.jpg";
        File file = new File(path);
        if (file.exists()) {
            return path;
        }
        try (InputStream inputStream = context.getAssets().open("HonorLogo.jpg");
             OutputStream outputStream = new FileOutputStream(path)) {
            byte[] buffer = new byte[1024];
            int byteRead;
            while ((byteRead = inputStream.read(buffer)) != -1) {
                Log.d(TAG, "byteRead : " + byteRead);
                outputStream.write(buffer, 0, byteRead);
            }
            inputStream.close();
            outputStream.flush();
            return path;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
