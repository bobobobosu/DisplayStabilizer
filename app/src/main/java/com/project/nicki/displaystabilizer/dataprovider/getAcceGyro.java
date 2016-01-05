package com.project.nicki.displaystabilizer.dataprovider;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
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
import android.view.Display;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import au.com.bytecode.opencsv.CSVWriter;

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
    private Sensor mRSensor; //raw acce
    private SensorEventListener mSensorEventListener;
    private HandlerThread mHandlerThread;
    private String TAG = "getAcceGyro";
    String csvName = "getAcceGyro.csv";


    FileWriter mFileWriter;
    public getAcceGyro() {
    }

    @Override
    public void run() {




        final proAcceGyroCali mproAcceGyroCali = new proAcceGyroCali(mContext);
        //mproAcceGyroCali.TEST();
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mLSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mRSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mHandlerThread = new HandlerThread("getAcceGyro");
        mHandlerThread.start();

        Handler mHandler = new Handler(mHandlerThread.getLooper());
        mSensorEventListener = new SensorEventListener() {
            long initTime = System.currentTimeMillis();
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(System.currentTimeMillis()-initTime>5000){
                    mproAcceGyroCali.CircularBuffer(event);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        mSensorManager.registerListener(mSensorEventListener, mGSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        mSensorManager.registerListener(mSensorEventListener, mLSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        mSensorManager.registerListener(mSensorEventListener, mMSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        mSensorManager.registerListener(mSensorEventListener, mRSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
    }

    public void LogCSV(String a, String b, String c, String d, String g, String h) {
        //init CSV logging
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = csvName;
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer = null;
        // File exist
        if (f.exists() && !f.isDirectory()) {
            try {
                mFileWriter = new FileWriter(filePath, true);
            } catch (IOException e) {
                //e.printStackTrace();
            }
            writer = new CSVWriter(mFileWriter);
        } else {
            try {
                writer = new CSVWriter(new FileWriter(filePath));
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }

        try {
            String line = String.format("%s,%s,%s,%s,%s,%s\n", a, b, c, d, g, h);
            mFileWriter.write(line);
        } catch (IOException e) {
            //e.printStackTrace();
        }

        try {
            writer.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }
}
