package com.android.mazhengyang.logger;

import android.app.Application;

import com.android.mazhengyang.library.LogType;
import com.android.mazhengyang.library.Logger;

/**
 * Created by mazhengyang on 18-10-31.
 */

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();

    private static Logger mLogger;

    @Override
    public void onCreate() {
        super.onCreate();

        mLogger = Logger.newBuilder(this)
                .setLogType(LogType.ALL)//日志输出类型
                .withPrint(true)//是否输出到终端
                .bulid();
        mLogger.cleanPreviewLog();//清除之前全部log文件
        mLogger.start();//开始记录

        mLogger.d(TAG, "onCreate: ");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mLogger.d(TAG, "onTerminate: ");
        mLogger.stop();
    }

    public static Logger getLogger() {
        return mLogger;
    }
}
