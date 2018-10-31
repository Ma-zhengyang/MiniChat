package com.android.mazhengyang.logger;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LogUtil.d(TAG, "onCreate: ");

        LogUtil.v(TAG, "this is a verbose log");
        LogUtil.d(TAG, "this is a debug log");
        LogUtil.i(TAG, "this is a info log");
        LogUtil.w(TAG, "this is a warn log");
        LogUtil.e(TAG, "this is a error log");
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.d(TAG, "onResume: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtil.d(TAG, "onPause: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "onDestroy: \n");
    }

}
