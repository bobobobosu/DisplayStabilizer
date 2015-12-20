package com.project.nicki.displaystabilizer.dataprovider;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.CircularBuffer;
import com.project.nicki.displaystabilizer.dataprocessor.proAccelerometer;
import com.project.nicki.displaystabilizer.dataprocessor.proDataFlow;
import com.project.nicki.displaystabilizer.dataprocessor.proDataProcess;
import com.project.nicki.displaystabilizer.dataprocessor.proGyroscope;

/**
 * Created by nickisverygood on 12/17/2015.
 */
public class getAcceGyro implements Runnable {
    public static float AcceX, AcceY, AcceZ;
    public static long AcceTime;
    String ACCEBROADCAST_STRING = "ACCEBROADCAST";
    private String TAG = "getAccelerometer";
    private SensorManager mASensorManager = null;
    private Sensor mASensor;
    private SensorEventListener mAListener;
    private HandlerThread mAHandlerThread;

    public getAcceGyro(Context context) {
        mContext = context;
    }

    public static float GyroX, GyroY, GyroZ;
    private Context mContext;
    private SensorManager mGSensorManager = null;
    private Sensor mGSensor;
    private SensorEventListener mGListener;
    private HandlerThread mGHandlerThread;


    @Override
    public void run() {
        mASensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mASensor = mASensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mAHandlerThread = new HandlerThread("AccelerometerLogListener");
        mAHandlerThread.start();
        Handler Ahandler = new Handler(mAHandlerThread.getLooper());

        mGSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mGSensor = mGSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGHandlerThread = new HandlerThread("GyroscopeLogListener");
        mGHandlerThread.start();
        Handler Ghandler = new Handler(mGHandlerThread.getLooper());

        mAListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                AcceX = event.values[0];
                AcceY = event.values[1];
                AcceZ = event.values[2];
                //AcceTime = event.timestamp;
                //AcceTime = (new Date()).getTime()
                //        + (event.timestamp - System.nanoTime()) / 1000000L;
                AcceTime = System.currentTimeMillis();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        mASensorManager.registerListener(mAListener, mASensor, SensorManager.SENSOR_DELAY_FASTEST,
                Ahandler);


        mGListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                GyroX = event.values[0];
                GyroY = event.values[1];
                GyroZ = event.values[2];
                //Log.d(TAG, String.valueOf(GyroX));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        mGSensorManager.registerListener(mGListener, mGSensor, SensorManager.SENSOR_DELAY_FASTEST,
                Ghandler);

        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (AcceX != 0 && AcceY != 0 && AcceZ != 0 && GyroX != 0 && GyroY != 0 && GyroZ != 0) {
                Log.d(TAG, String.valueOf(AcceX) + String.valueOf(AcceY) + String.valueOf(AcceZ+ String.valueOf(GyroX)+ String.valueOf(GyroY)+ String.valueOf(GyroZ) ));
                proDataProcess b = new proDataProcess();
                b.decodeSensorPacket(GyroX, GyroY, GyroZ, AcceX, AcceY, AcceZ);

            }
        }


    }
}
