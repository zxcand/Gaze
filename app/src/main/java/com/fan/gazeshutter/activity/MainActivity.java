package com.fan.gazeshutter.activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import com.fan.gazeshutter.MainApplication;

import java.util.HashMap;
import java.util.Iterator;


public class MainActivity extends AppCompatActivity implements View.OnGenericMotionListener  {
    private static final String TAG = "MainActivity";
    private static final String ACTION_USB_PERMISSION = "com.fan.gazeshutter.activity.USB_PERMISSION";
    UsbManager mUsbManager;
    IntentFilter filterAttached_and_Detached;
    BroadcastReceiver mUsbReceiver;

    boolean isGazing = false;
    Point gazePoint = null;
    /*
     *  lifecycle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        getWindow().getDecorView().getRootView().setOnGenericMotionListener(this);

        filterAttached_and_Detached = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        filterAttached_and_Detached.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filterAttached_and_Detached.addAction(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filterAttached_and_Detached);

        init();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
    }

    protected void setScreenSize(){
        MainApplication mainApplication = MainApplication.getInstance();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mainApplication.mScreenWidth  = size.x;
        mainApplication.mScreenHeight = size.y;
        Log.d(TAG,"x:"+size.x+" y:"+size.y);
    }


    protected void init(){
        setScreenSize();
        mUsbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if(device != null){
                            Log.d(TAG,"DEATTCHED-" + device);
                        }
                    }
                }

                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if(device != null){
                                Log.d(TAG,"ATTACHED-" + device);
                            }
                        }
                        else {
                            PendingIntent mPermissionIntent;
                            mPermissionIntent = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_ONE_SHOT);
                            mUsbManager.requestPermission(device, mPermissionIntent);
                        }
                    }
                }

                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if(device != null){
                                Log.d(TAG,"PERMISSION-" + device);
                            }
                        }
                    }
                }
            }
        };

        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Log.d(TAG, deviceList.size()+" USB device(s) found");
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            Log.d("1","" + device);
        }
    }
    public boolean getGazeState(){
        return isGazing;
    }
    public Point getGazePoint(){
        return gazePoint;
    }

     /*
     * mouse event
     */
    @Override
    public boolean onGenericMotion(View v, MotionEvent event) {
        //if((event.getSource() & InputDevice.SOURCE_MOUSE) == 0)
        if(event.getToolType(0)!=MotionEvent.TOOL_TYPE_MOUSE
                && event.getToolType(0)!=MotionEvent.TOOL_TYPE_FINGER)
            return super.onGenericMotionEvent(event);

        Log.d(TAG,"x="+event.getX()+" y="+event.getY());
        int x = (int)event.getX();
        int y = (int)event.getY();
        if(event.getButtonState() == MotionEvent.ACTION_DOWN){
            isGazing = true;
            gazePoint = new Point(x, y);
        }
        else if(event.getButtonState() == MotionEvent.ACTION_MOVE) {
            gazePoint = new Point(x, y);
        }
        else if(event.getButtonState() == MotionEvent.ACTION_UP){
            isGazing = false;
        }

        if((event.getEdgeFlags() & MotionEvent.EDGE_LEFT) != 0){
            Log.d(TAG,"edge left");
        }

        return true;
    }
}

