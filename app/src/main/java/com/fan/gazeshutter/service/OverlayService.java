package com.fan.gazeshutter.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.fan.gazeshutter.R;
import com.fan.gazeshutter.utils.NetworkUtils;

import java.util.ArrayList;

import butterknife.Bind;

/**
 * Created by fan on 3/27/16.
 * ref. http://www.whycouch.com/2013/01/how-to-overlay-view-over-everything-on.html
 */
public class OverlayService extends Service{
    private static final int LayoutParamFlags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            //| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
    static final boolean SHOWING_MARKER = false;
    static final String TAG = "OverlayService";

    ZMQReceiveTask mZMQRecvTask;
    WindowManager mWindowManager;
    LayoutInflater mLayoutInflater;
    View mCurrentView;

    //private View mHaloButtonLayer = new HaloButtonLayer(this);
    ArrayList<View> mButtonLayers;


    //@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        mZMQRecvTask = new ZMQReceiveTask(this);
        mZMQRecvTask.execute(NetworkUtils.getServerIP());
        Log.d(TAG,"onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mButtonLayers = new ArrayList<View>();
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mLayoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        if (mLayoutInflater == null) {
            throw new AssertionError("LayoutInflater not found.");
        }


        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                LayoutParamFlags,
                PixelFormat.RGBA_8888);
        params.gravity = Gravity.LEFT | Gravity.TOP;
        View v = mLayoutInflater.inflate(R.layout.overlay, null);
        mWindowManager.addView(v, params);
        mButtonLayers.add(v);


        final ImageView btn = (ImageView) v.findViewById(R.id.btn);
        btn.setOnTouchListener(new View.OnTouchListener() {
               @Override
               public boolean onTouch(View v, MotionEvent event) {
                   return false;
               }
           }
        );

        v.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long downTime;

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i("downTime", downTime + "");
                        downTime = SystemClock.elapsedRealtime();
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return false;
                    case MotionEvent.ACTION_UP:
                        long currentTime = SystemClock.elapsedRealtime();
                        Log.i("currentTime - downTime", currentTime - downTime + "");
                        if (currentTime - downTime < 200) { // 當按下圖片按鈕時
                            v.performClick(); // 自動點擊事件
                        } else {
                            // updateViewLocation(); //黏住邊框功能
                        }
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX
                                + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY
                                + (int) (event.getRawY() - initialTouchY);
                        Log.d("X,Y",""+params.x+"  "+params.y);
                        mWindowManager.updateViewLayout(v, params);
                        return false;
                }
                return false;
            }
        });
/*
        final   WindowManager.LayoutParams params2 = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.RGBA_8888);
        mWindowManager.addView(mHaloButtonLayer, params2);
*/

        if(SHOWING_MARKER){
            ViewGroup mView =  (ViewGroup) mLayoutInflater.inflate(R.layout.overlay, null);
            mWindowManager.addView(mView, params);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for(View btnLayer:mButtonLayers){
            mWindowManager.removeView(btnLayer);
        }
        mButtonLayers = new ArrayList<View>();
    }


    public void stimulateTouchEvent(int x, int y){
        mCurrentView.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0));
        mCurrentView.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, x, y, 0));
        return;
    }
}
