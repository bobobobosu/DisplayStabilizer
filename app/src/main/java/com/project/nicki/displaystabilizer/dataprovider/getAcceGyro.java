package com.project.nicki.displaystabilizer.dataprovider;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.project.nicki.displaystabilizer.R;
import com.project.nicki.displaystabilizer.dataprocessor.CircularBuffer;
import com.project.nicki.displaystabilizer.dataprocessor.proAccelerometer;
import com.project.nicki.displaystabilizer.dataprocessor.proDataFlow;
import com.project.nicki.displaystabilizer.dataprocessor.proDataProcess;
import com.project.nicki.displaystabilizer.dataprocessor.proGyroscope;
import com.project.nicki.displaystabilizer.dataprocessor.proAcceGyroCali;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by nickisverygood on 12/17/2015.
 */
public class getAcceGyro implements Runnable {
    private Context mContext;
    public getAcceGyro(Context context) {
        mContext = context;
    }
    private SensorManager mSensorManager;
    private Sensor mGSensor; //gyro
    private Sensor mLSensor; //linear acce
    private Sensor mMSensor; //magn
    private SensorEventListener mSensorEventListener;
    private HandlerThread mHandlerThread;
    private String TAG = "getAcceGyro";

    public getAcceGyro() {
    }
    @Override
    public void run() {




        final proAcceGyroCali mproAcceGyroCali = new proAcceGyroCali(mContext);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mLSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mHandlerThread = new HandlerThread("getAcceGyro");
        mHandlerThread.start();
        Handler mHandler = new Handler(mHandlerThread.getLooper());
        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                //if(proAcceGyroCali.Calibrated == false){
                    mproAcceGyroCali.CircularBuffer(event);
                //}
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        mSensorManager.registerListener(mSensorEventListener, mGSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        mSensorManager.registerListener(mSensorEventListener, mLSensor, SensorManager.SENSOR_DELAY_FASTEST,mHandler);
        mSensorManager.registerListener(mSensorEventListener, mMSensor, SensorManager.SENSOR_DELAY_FASTEST,mHandler);
    }


}
