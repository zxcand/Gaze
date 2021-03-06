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
import android.widget.TextView;

import com.fan.gazeshutter.MainApplication;
import com.fan.gazeshutter.R;
import com.fan.gazeshutter.activity.PilotStudyActivity;
import com.fan.gazeshutter.event.GazeEvent;
import com.fan.gazeshutter.event.ModeEvent;
import com.fan.gazeshutter.utils.NetworkUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import butterknife.Bind;

/**
 * Created by fan on 3/27/16.
 * ref. http://www.whycouch.com/2013/01/how-to-overlay-view-over-everything-on.html
 */
public class OverlayService extends Service{
    private static final int LayoutParamFlags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
    static final String TAG = "OverlayService";

    ZMQReceiveTask mZMQRecvTask;
    WindowManager mWindowManager;
    LayoutInflater mLayoutInflater;
    View mCurrentView;
    PilotStudyActivity.MODE mMode = PilotStudyActivity.MODE.values()[0];

    //private View mHaloButtonLayer = new HaloButtonLayer(this);
    //ArrayList<View> mButtonLayers;

    //@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        mZMQRecvTask = new ZMQReceiveTask(this);
        mZMQRecvTask.execute(NetworkUtils.getServerIP());
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
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
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestory, cancel asyntask");
        EventBus.getDefault().unregister(this);
        stopAsyntask();
        super.onDestroy();
    }

    public void stopAsyntask(){
        mZMQRecvTask.cancel(true);
        return;
    }


    public void stimulateTouchEvent(int x, int y){
        mCurrentView.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0));
        mCurrentView.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, x, y, 0));
        return;
    }


    @Subscribe
    public void onEvent(ModeEvent me){
        mMode = me.mode;
    }
}
