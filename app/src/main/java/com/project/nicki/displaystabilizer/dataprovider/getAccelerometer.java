package com.project.nicki.displaystabilizer.dataprovider;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.CircularBuffer;

import android.os.Bundle;
import android.os.Message;

/**
 * Created by nicki on 10/27/2015.
 */
public class getAccelerometer implements Runnable {
    public static float AcceX, AcceY, AcceZ;
    public static long AcceTime;
    String ACCEBROADCAST_STRING = "ACCEBROADCAST";
    private String TAG = "getAccelerometer";
    private Context mContext;
    private SensorManager mSensorManager = null;
    private Sensor mSensor;
    private SensorEventListener mListener;
    private HandlerThread mHandlerThread;
    private Runnable mRunnable = new proAccelerometer(mContext);
    public getAccelerometer(Context context) {
        mContext = context;
    }
    private int init = 1;

    //Noshake
    private final int SENEOR_TYPE = Sensor.TYPE_LINEAR_ACCELERATION;
    private final int ACCELEROMOTER_FPS = SensorManager.SENSOR_DELAY_FASTEST;
    private final int BUFFER_SECOND = 4;
    private final int FPS = 60;
    private final int BUFFER_DATA_SIZE = BUFFER_SECOND * FPS;
    private int OFFSET_SCALE = 30;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private CircularBuffer mBufferX;
    private CircularBuffer mBufferY;
    private int mScreenHeight, mScreenWidth;
    private float[] data;
    public static float whenstaticX, whenstaticY;


    @Override
    public void run() {
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mHandlerThread = new HandlerThread("AccelerometerLogListener");
        mHandlerThread.start();
        Handler handler = new Handler(mHandlerThread.getLooper());
        mListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                AcceX = event.values[0];
                AcceY = event.values[1];
                AcceZ = event.values[2];
                //AcceTime = event.timestamp;
                //AcceTime = (new Date()).getTime()
                //        + (event.timestamp - System.nanoTime()) / 1000000L;
                AcceTime = System.currentTimeMillis();


                mBufferX = new CircularBuffer(BUFFER_DATA_SIZE, BUFFER_SECOND);
                mBufferY = new CircularBuffer(BUFFER_DATA_SIZE, BUFFER_SECOND);


                new Thread(new proAccelerometer()).start();
                /*
                Runnable r1 =  new proAccelerometer();
                r1.run();
                */

                //noshake
                new Thread(new Runnable() {
                    public void run() {
                        /*
                        if(whenstaticX == 0 || whenstaticY == 0){
                            whenstaticX = Math.abs(AcceX);
                            whenstaticY = Math.abs(AcceY);
                        }else {
                            whenstaticX = (whenstaticX+AcceX)/2;
                            whenstaticY = (whenstaticY+AcceY)/2;
                        }
                        Log.d(TAG, "whenstatic " + whenstaticX + " " + whenstaticY);

                        if(AcceX>-whenstaticX && AcceX<whenstaticX){
                            AcceX = 0;
                        }
                        if(AcceY>-whenstaticY && AcceX<whenstaticY){
                            AcceY = 0;
                        }
*/


                        mBufferX.insert(AcceX);
                        mBufferY.insert(AcceY);
                        final float dx = -mBufferX.convolveWithH() * OFFSET_SCALE;
                        final float dy = -mBufferY.convolveWithH() * OFFSET_SCALE;
                        data = new float[3];
                        data[0] = dx;
                        data[1] = dy;
                        data[2] = AcceZ;
                        Message msg = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putFloatArray("Acce", data);
                        bundle.putLong("Time", getAccelerometer.AcceTime);
                        msg.setData(bundle);
                        //proDataFlow.AcceHandler.sendMessage(msg);
                        Log.d(TAG, String.valueOf(dx) + " " + String.valueOf(dy) + " " + String.valueOf(AcceZ));
                        mRunnable.run();
                    }
                }).start();




                //mRunnable.run();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        mSensorManager.registerListener(mListener, mSensor, SensorManager.SENSOR_DELAY_FASTEST,
                handler);
    }
}
