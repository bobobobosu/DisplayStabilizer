package com.project.nicki.displaystabilizer.dataprovider;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

import com.project.nicki.displaystabilizer.UI.DemoDrawUI;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.dataprocessor.proAcceGyroCali;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by nickisverygood on 12/17/2015.
 */
public class getAcceGyro implements Runnable {
    public static Handler mgetValusHT_TOUCH_handler;
    String csvName = "getAcceGyro.csv";
    FileWriter mFileWriter;
    private Context mContext;
    private SensorManager mSensorManager;
    private Sensor mGSensor; //gyro
    private Sensor mLSensor; //linear acce
    private Sensor mMSensor; //magn
    private Sensor mRSensor; //raw acce
    private SensorEventListener mSensorEventListener;
    private HandlerThread mHandlerThread;
    private String TAG = "getAcceGyro";


    public getAcceGyro(Context context) {
        mContext = context;
    }

    public getAcceGyro() {
    }

    @Override
    public void run() {
        final HandlerThread mgetValusHT_TOUCH = new HandlerThread("getValues_TOUCH");
        mgetValusHT_TOUCH.start();
        mgetValusHT_TOUCH_handler = new Handler(mgetValusHT_TOUCH.getLooper());
        final HandlerThread mgetValusHT_ACCE = new HandlerThread("getValues_ACCE");
        mgetValusHT_ACCE.start();
        final Handler mgetValusHT_ACCE_handler = new Handler(mgetValusHT_ACCE.getLooper());
        final HandlerThread mgetValusHT_ORIEN = new HandlerThread("getValues_ORIEN");
        mgetValusHT_ORIEN.start();
        final Handler mgetValusHT_ORIEN_handler = new Handler(mgetValusHT_ORIEN.getLooper());

        final proAcceGyroCali mproAcceGyroCali = new proAcceGyroCali(mContext);
        //mproAcceGyroCali.TEST();
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mLSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mRSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mHandlerThread = new HandlerThread("getAcceGyro");
        mHandlerThread.start();

        Handler mHandler = new Handler(mHandlerThread.getLooper());
        mSensorEventListener = new SensorEventListener() {
            float[] mGravity;
            float[] mGeomagnetic;
            long initTime = System.currentTimeMillis();

            @Override
            public void onSensorChanged(final SensorEvent event) {
                DemoDrawUI.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DemoDrawUI.mlog_draw.setText("cX= " + String.valueOf(stabilize_v2.getcX()));
                            DemoDrawUI.mlog_cam.setText("cY= " + String.valueOf(stabilize_v2.getcY()));
                        } catch (Exception ex) {

                        }
                    }
                });

                mproAcceGyroCali.Controller(event);
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_LINEAR_ACCELERATION:
                        if(DemoDraw.drawing==0 || DemoDraw.drawing==1){
                            mgetValusHT_ACCE_handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //init.initSensorCollection.append(new SensorCollect.sensordata(System.currentTimeMillis(), event.values, SensorCollect.sensordata.TYPE.ACCE));
                                }
                            });
                        }

                }

                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                    mGravity = event.values;
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                    mGeomagnetic = event.values;
                if (mGravity != null && mGeomagnetic != null) {
                    float R[] = new float[9];
                    float I[] = new float[9];
                    boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                    if (success) {
                        final float orientation[] = new float[3];
                        SensorManager.getOrientation(R, orientation);
                        if(DemoDraw.drawing==0 || DemoDraw.drawing==1){
                            mgetValusHT_ORIEN_handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //init.initSensorCollection.append(new SensorCollect.sensordata(System.currentTimeMillis(), orientation, SensorCollect.sensordata.TYPE.ORIEN_radian));
                                }
                            });
                        }
                    }
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
