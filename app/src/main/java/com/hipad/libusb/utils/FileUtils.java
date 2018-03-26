package com.hipad.libusb.utils;

import android.content.Context;

/**
 * Created by wangbaiqiang on 2017/12/15.
 * email 1036607309@qq.com
 */

public class FileUtils {
    public static String getCachePath(Context context){
        String path = context.getCacheDir().getPath();
        return path;
    }
}
