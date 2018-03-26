package com.hipad.libusb.base;

import android.app.Application;

import com.hipad.libusb.utils.CrashHandler;


public class MyApplication extends Application {

    private static final String TAG = "wbq";

    @Override
    public void onCreate() {
        super.onCreate();

        CrashHandler.getInstance().init(this);
    }

}
