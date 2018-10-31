package com.android.mazhengyang.library;

/**
 * Created by mazhengyang on 18-10-30.
 */

public class LogType {

    //日志输出类型
    public static final int VERBOSE = 0x01;
    public static final int DEBUG = 0x01 << 1;
    public static final int INFO = 0x01 << 2;
    public static final int WARN = 0x01 << 3;
    public static final int ERROR = 0x01 << 4;
    public static final int ALL = VERBOSE | DEBUG | INFO | WARN | ERROR;
}
