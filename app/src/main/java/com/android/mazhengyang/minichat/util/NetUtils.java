package com.android.mazhengyang.minichat.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by mazhengyang on 18-11-28.
 */

public class NetUtils {

    private static final String TAG = "MiniChat." + NetUtils.class.getSimpleName();

    private static String ip;

    /**
     * 判断wifi是否连接
     *
     * @param context
     * @return
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        return false;
    }

    /**
     * 得到本机IP地址
     *
     * @return
     */
    public static String getLocalIpAddress() {
        if (ip == null) {
            try {
                //获得当前可用的wifi网络
                Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                while (en.hasMoreElements()) {
                    NetworkInterface nif = en.nextElement();
                    Enumeration<InetAddress> enumIpAddr = nif.getInetAddresses();
                    while (enumIpAddr.hasMoreElements()) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address
                        /*&& InetAddressUtils.isIPv4Address(mInetAddress.getHostAddress())*/) {
                            ip = inetAddress.getHostAddress();
                            return ip;
                        }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
                Log.e(TAG, "getLocalIpAddress: fail to access ip, " + e);
            }
        }
        return ip;
    }


    public static void resetLocalIpAddress() {
        ip = null;
    }

}
