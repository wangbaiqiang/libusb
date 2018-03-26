package com.hipad.libusb.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.hipad.libusb.R;
import com.hipad.libusb.utils.ConstantUtils;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    static {
        System.loadLibrary("native-lib");
    }

    private static final String ACTION_USB_PERMISSION = "com.hipad.USB_PERMISSION";
    private Button buttonB;
    private ExecutorService mThreadPool;
    private UsbManager mUsbManager;
    private UsbEndpoint outEndpoint;
    private UsbEndpoint inEndpoint;
    private UsbInterface usbInterface;
    private UsbDeviceConnection deviceConnection;
    private int bufferSize = 512;//1024
    byte[] writeBuffer = new byte[bufferSize];
    private static final int REQUEST_PERMISSIONS = 2;
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ConstantUtils.SEND_FAILED://发送失败
                    Toast.makeText(MainActivity.this, " 发送失败，请重新发送。。。", Toast.LENGTH_SHORT).show();
                    break;
                case ConstantUtils.SEND_MESSAGE_SUCCESS://成功发送数据
                    Toast.makeText(MainActivity.this, " 发送成功。。。", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private int productId;
    private int venddorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT >= M&&hasPermissions()){
        } else if(Build.VERSION.SDK_INT >= M){
            requestPermissions();
        }
        init();
        initListener();
        registerReceiver();
        setUpDevice();

    }



    private void registerReceiver() {
        //监听otg插入 拔出
        IntentFilter usbDeviceStateFilter = new IntentFilter();
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbDeviceStateFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbDeviceStateFilter);
        //注册监听自定义广播
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
    }

    private void setUpDevice() {
        mThreadPool = Executors.newFixedThreadPool(5);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        openDevices();
    }

    private void initListener() {
        buttonB.setOnClickListener(this);
    }

    private void init() {
        bindView();
    }

    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_USB_PERMISSION://接受到自定义广播
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {  //允许权限申请
                        if (usbDevice != null) {  //Do something
                            initAccessory(usbDevice);
                        } else {
                            Toast.makeText(context, "未获取到设备信息", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "用户未授权，读取失败", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_ATTACHED://接收到存储设备插入广播
                    UsbDevice device_add = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device_add != null) {
                        Toast.makeText(context, "接收到存储设备插入广播，尝试读取", Toast.LENGTH_SHORT).show();
                        setUpDevice();
                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED://接收到存储设备拔出广播
                    UsbDevice device_remove = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device_remove != null) {
                        Toast.makeText(context, "接收到存储设备拔出广播", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    /**
     * 打开设备 , 连接两端
     */
    private void openDevices() {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        //列举设备(手机)
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
//        Toast.makeText(this, "连接的设备数量" + deviceList.toString(), Toast.LENGTH_SHORT).show();
//        mUsbManager.openDevice()
        if (deviceList != null) {
            for (UsbDevice device : deviceList.values()) {
                Toast.makeText(this, "循环设备执行次数", Toast.LENGTH_SHORT).show();
                int interfaceCount = device.getInterfaceCount();
                int productId0 = device.getProductId();
                int vendorId0= device.getVendorId();
                Log.e("www","可用的的pid="+productId0+"......"+"vid="+vendorId0);
                Log.e("www","拿到的接口数据size=="+interfaceCount);
//                usbInterface = device.getInterface(1);
//                int endpointCount = usbInterface.getEndpointCount();
//                for (int i = 0; i < endpointCount; i++) {
//                    UsbEndpoint usbEndpoint = usbInterface.getEndpoint(i);
//                    if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
//                        if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
//                            outEndpoint = usbEndpoint;
//                            int productId = device.getProductId();
//                            int vendorId = device.getVendorId();
//                            Log.e("www","拿到的pid="+productId+"......"+"vid="+vendorId);
//                            Log.e("www","协议是==="+usbInterface.getInterfaceProtocol());
//                        } else if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
//                            inEndpoint = usbEndpoint;
//                        }
//                    }
//                }
//                if (outEndpoint != null && inEndpoint != null) {
//                    Log.e("www","连接成功，拿到端点"+"输出端点out="+outEndpoint.getEndpointNumber()+"输入端点="+inEndpoint.getEndpointNumber());
//                }
                if (mUsbManager.hasPermission(device)) {
                    initAccessory(device);
                } else {
                    mUsbManager.requestPermission(device, pendingIntent);
                }
            }
        } else {
            Toast.makeText(this, "请连接USB", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 发送命令 , 让手机进入Accessory模式
     *
     * @param usbDevice
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB_MR1)
    private void initAccessory(UsbDevice usbDevice) {
        if (mUsbManager.hasPermission(usbDevice)) {
            deviceConnection = mUsbManager.openDevice(usbDevice);
            int fd = deviceConnection.getFileDescriptor();
            // TODO: 2018/3/19  libusb可以把该描述符传递给native层去打开设备进行操作
             productId = usbDevice.getProductId();
             venddorId=usbDevice.getVendorId();
//             libBulkTransfer(productId,venddorId);
//            if (deviceConnection == null) {
//                throw new IllegalArgumentException("deviceConnection is null!");
//            }

//            boolean claim = deviceConnection.claimInterface(usbInterface, true);
//            if (!claim) {
//                throw new IllegalArgumentException("could not claim interface!");
//            }
        } else {
            throw new IllegalStateException("Missing permission to access usb device: " + usbDevice);
        }

    }

    private void bindView() {
        buttonB = (Button) findViewById(R.id.start_shareb_button);
    }

    /**
     * request permission
     */
    @TargetApi(M)
    private void requestPermissions() {
        if (!shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)
                && !shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage("Using your mic to record audio and your sd card to save video file")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS))
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            // we request 2 permissions
            if (grantResults.length == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            }
        }
    }

    private boolean hasPermissions() {
        PackageManager pm = getPackageManager();
        String packageName = getPackageName();
        int granted = pm.checkPermission(READ_EXTERNAL_STORAGE, packageName)
                | pm.checkPermission(WRITE_EXTERNAL_STORAGE, packageName);
        return granted == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_shareb_button:
                try {
//                    if (outEndpoint == null) {
//                        Log.e("www","没有连接的设备outEndpoint拿到null");
//                        throw new IllegalArgumentException("没有连接的设备");
//                    }
                    libBulkTransfer(productId,venddorId);
//                    ImageRunnableTest sendImageRunnableTest = new ImageRunnableTest(MainActivity.this, deviceConnection, outEndpoint, writeBuffer);
//                    mThreadPool.execute(sendImageRunnableTest);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

        }
    }
    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
        if (deviceConnection != null) {
            deviceConnection.releaseInterface(usbInterface);
            deviceConnection.close();
            deviceConnection = null;
        }
        outEndpoint = null;
        inEndpoint = null;
        mThreadPool.shutdownNow();
        unregisterReceiver(mUsbReceiver);
    }
    public native void libBulkTransfer(int pid, int vid);

}
