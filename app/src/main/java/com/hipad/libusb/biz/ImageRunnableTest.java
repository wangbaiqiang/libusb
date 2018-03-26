package com.hipad.libusb.biz;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.hipad.libusb.activity.MainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Created by wangbaiqiang on 2017/1/10.
 * email 1036607309@qq.com
 */

public class ImageRunnableTest implements Runnable {
    private int quantity;
    private Handler mHandler;
    private int len;
    private UsbDeviceConnection mUsbDeviceConnection;
//    InputStream stream;
    private byte[] buffer;
    private UsbEndpoint mUsbEndpointOut;
    private String header;
    private MainActivity mainActivity;

    public ImageRunnableTest(MainActivity mainActivity, int quantity, Handler mHandler, UsbDeviceConnection mUsbDeviceConnection, byte[] buffer, UsbEndpoint mUsbEndpointOut, InputStream stream, int fileSize, String fileName) {
        this.quantity = quantity;
        this.mHandler = mHandler;
        this.mUsbDeviceConnection = mUsbDeviceConnection;
//        this.stream = stream;
        this.buffer = buffer;
        this.mUsbEndpointOut = mUsbEndpointOut;
//        if (mUsbEndpointOut==null){
//            throw new IllegalArgumentException("没有连接的设备");
//        }
        header = "start"+","+"txt";
        this.mainActivity = mainActivity;
    }

    public ImageRunnableTest(MainActivity mainActivity,UsbDeviceConnection mUsbDeviceConnection, UsbEndpoint mUsbEndpointOut,byte[] buffer) {
        this.mainActivity=mainActivity;
        this.mUsbDeviceConnection=mUsbDeviceConnection;
        this.buffer=buffer;
        this.mUsbEndpointOut=mUsbEndpointOut;
    }

    @Override
    public void run() {

        /**
         * 发送数据的地方 , 只接受byte数据类型的数据
         */
        //如果发送的是image需要：
        // 发送数据之前，告诉配件端我要发送的是什么和文件的大小。
        synchronized (this) {
//            String cachePath = FileUtils.getCachePath(mainActivity);
            File imageCache = Environment.getExternalStorageDirectory();
//            File imageCache=new File(cachePath);
            if (imageCache.exists()){
                int pckNum=1;
                while (true) {
                    int sta=1;
                    try {
//                    File images = new File(imageCache, "images");
//                    File images = new File(imageCache, "ey66");
                    File images = new File(imageCache, "pck");
//                    File images = new File(imageCache, "bigimage");
//                    File images = new File(imageCache, "smallimage");
//                    File[] files = images.listFiles();
                    String format = String.format(images.getAbsolutePath()+File.separator+"image%d.JPG", pckNum);
//                    String format = String.format(images.getAbsolutePath()+File.separator+"image%d.jpg", pckNum);
                    Log.e("www","开始读的图片的文件名字="+format);
                    Log.e("www111111","当前的图片位置：="+pckNum);
                    FileInputStream mfileStream=new FileInputStream(format);
//                    String start="start";
//                    mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, start.getBytes(), start.length(), 3000);

                    for(;;){
                        buffer[0]= (byte) 0xF0;
                        Log.e("www","read文件开始"+System.currentTimeMillis());
                        len = mfileStream.read(buffer,2,510);
                        Log.e("www","read文件一包结束"+System.currentTimeMillis());
                        if (sta==1){
                            buffer[1]=0x00;
                            sta=0;
                        }else if(len<510){
                            buffer[1]=0x02;
                        }else{
                            buffer[1]=0x01;
                        }
                        Log.e("www","读取的长度length=="+len);

                        Log.e("www","读取的字节值="+ Arrays.toString(buffer));
                        Log.e("www","传输图片一包开始"+System.currentTimeMillis());
                        int rr=mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, buffer, len+2, 1000);
                        Log.e("www","传输图片一包结束"+System.currentTimeMillis());

                        if (len!=510){

                            Log.e("www","发送完成一张跳出循环进行发送下一张 当前的pckNum="+pckNum);
                            break;
                        }
                        Log.e("www","发送返回值rr="+rr);
//                        Arrays.fill(buffer,(byte)0);
//                        Log.e("www","调用Arrays.fill之后buffer的全部值为="+Arrays.toString(buffer));
                        if (rr<0){
                            Log.e("www","发送失败 当前的pckNum="+pckNum);
                        }
                        }
                        Log.e("www","传输一张图片完成结束"+System.currentTimeMillis());

                        mfileStream.close();
                        pckNum++;
                        if (pckNum==20){
                            pckNum=1;
                        }
                        Thread.sleep(25);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
            }
                }
    }
}
