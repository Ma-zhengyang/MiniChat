package com.android.mazhengyang.minichat.util;

import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by mazhengyang on 18-11-28.
 */

public class Utils {

    private static final String TAG = "MiniChat." + Utils.class.getSimpleName();

    /**
     * 得到本机IP地址
     *
     * @return
     */
    public static String getLocalIpAddress() {
        try {
            //获得当前可用的wifi网络
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface nif = en.nextElement();
                Enumeration<InetAddress> enumIpAddr = nif.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress mInetAddress = enumIpAddr.nextElement();
                    if (!mInetAddress.isLoopbackAddress() /*&& InetAddressUtils.isIPv4Address(mInetAddress.getHostAddress())*/) {
                        return mInetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            Log.e(TAG, "getLocalIpAddress: fail to access ip, " + e);
        }
        return null;
    }

}
