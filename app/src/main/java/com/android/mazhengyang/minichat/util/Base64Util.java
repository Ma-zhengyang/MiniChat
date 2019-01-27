package com.android.mazhengyang.minichat.util;

import android.util.Base64;
import android.util.Log;

/**
 * Created by mazhengyang on 18-9-7.
 */

public class Base64Util {

    private static final String TAG = "Encryption.Base64Util";

    /**
     *   概述：算不上什么加密算法，只是对数据进行编码传输
     */

    public static String encrypt(String string) {
        String encodedString = Base64.encodeToString(string.getBytes(), Base64.DEFAULT);
        Log.d(TAG, "encrypt: encodedString=" + encodedString);
        return encodedString;
    }

    public static String decrypt(String encodedString) {
        String decodedString = Base64.encodeToString(encodedString.getBytes(), Base64.DEFAULT);
        Log.d(TAG, "decrypt: decodedString=" + decodedString);
        return decodedString;
    }

}
