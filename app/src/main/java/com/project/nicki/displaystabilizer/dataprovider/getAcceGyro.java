package com.project.nicki.displaystabilizer.dataprovider;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.project.nicki.displaystabilizer.UI.DemoDrawUI;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw2;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.dataprocessor.proAcceGyroCali2;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by nickisverygood on 12/17/2015.
 */
public class getAcceGyro implements Runnable {
    public static Handler mgetValusHT_TOUCH_handler;
    public static boolean isStatic = true;
    String csvName = "getAcceGyro.csv";
    FileWriter mFileWriter;
    private Context mContext;
    private SensorManager mSensorManager;
    private Sensor mGSensor; //gyro
    private Sensor mLSensor; //linear acce
    private Sensor mMSensor; //magn
    private Sensor mRSensor; //raw acce
    private Sensor mMASensor;
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
        final StaticSensor mstaticsensor = new StaticSensor();
        final HandlerThread mgetValusHT_TOUCH = new HandlerThread("getValues_TOUCH");
        mgetValusHT_TOUCH.start();
        mgetValusHT_TOUCH_handler = new Handler(mgetValusHT_TOUCH.getLooper());
        final HandlerThread mgetValusHT_ACCE = new HandlerThread("getValues_ACCE");
        mgetValusHT_ACCE.start();
        final Handler mgetValusHT_ACCE_handler = new Handler(mgetValusHT_ACCE.getLooper());
        final HandlerThread mgetValusHT_ORIEN = new HandlerThread("getValues_ORIEN");
        mgetValusHT_ORIEN.start();
        final Handler mgetValusHT_ORIEN_handler = new Handler(mgetValusHT_ORIEN.getLooper());

        //final proAcceGyroCali mproAcceGyroCali = new proAcceGyroCali(mContext);
        final proAcceGyroCali2 mproAcceGyroCali2 = new proAcceGyroCali2(mContext);
        //mproAcceGyroCali.TEST();
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mLSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mRSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMASensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
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

                //mproAcceGyroCali.Controller(event);
                mproAcceGyroCali2.Controller(event);
                switch (event.sensor.getType()) {
                    case Sensor.TYPE_LINEAR_ACCELERATION:
                        isStatic = mstaticsensor.getStatic(event.values);
                        //if(DemoDraw2.drawing==0 || DemoDraw2.drawing==1){
                            //mgetValusHT_ACCE_handler.post(new Runnable() {
                                //@Override
                                //public void run() {
                                    //init.initSensorCollection.append(new SensorCollect.sensordata(System.currentTimeMillis(), event.values, SensorCollect.sensordata.TYPE.ACCE));
                                //}
                            //});
                        //}
                }

                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    mGravity = event.values;
                }
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    mGeomagnetic = event.values;
                }
                if (mGravity != null && mGeomagnetic != null) {

                    float R[] = new float[9];
                    float I[] = new float[9];
                    boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                    if (success) {
                        final float orientation[] = new float[3];
                        SensorManager.getOrientation(R, orientation);
                        //if(DemoDraw2.drawing==0 || DemoDraw2.drawing==1) {
                            //mgetValusHT_ORIEN_handler.post(new Runnable() {
                                //@Override
                                //public void run() {
                                    //init.initSensorCollection.append(new SensorCollect.sensordata(System.currentTimeMillis(), orientation, SensorCollect.sensordata.TYPE.ORIEN_radian));
                                //}
                            //});
                        //}
                    }else {
                        Log.d("TESTING", "error");
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
        mSensorManager.registerListener(mSensorEventListener, mMASensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
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

    public class StaticSensor {
        float threshold = 0.000000000001f;
        int window = 100;
        List<float[]> CircularBuffer = new ArrayList<>();
        int staticNum = 0;
        boolean Moving = true;

        public boolean getStatic(float[] data) {
            //load into buffer
            if (CircularBuffer.size() < window) {
                CircularBuffer.add(data);
            } else {
                CircularBuffer.add(data);
                CircularBuffer.remove(0);
            }
            //calculate deviation
            float mdataset[][] = List2Array(CircularBuffer);
            float[] variance = new float[data.length];
            for (int i = 0; i < data.length; i++) {
                variance[i] = new Statistics(mdataset[i]).getVariance();
            }
            //calculate variance vector
            float variancevector = getVarianceMagnitude(variance);
            //compare with threshold, if static=>set to 0
            if (variancevector < threshold) {
                staticNum++;
            } else {
                staticNum = 0;
            }
            if (staticNum < 50) {
                Log.d(TAG, "Movinggg");
                getAcceGyro.isStatic = false;
                isStatic = false;
            } else {
                getAcceGyro.isStatic = true;
                isStatic = true;
            }
            return isStatic;
        }

        public float[][] List2Array(List<float[]> input) {
            float[][] toreturn = new float[input.get(0).length][input.size()];
            for (int i = 0; i < input.get(0).length; i++) {
                for (int j = 0; j < input.size(); j++) {
                    toreturn[i][j] = input.get(j)[i];
                }
            }
            return toreturn;
        }

        public float getVarianceMagnitude(float[] data) {
            MathContext mc = new MathContext(50, RoundingMode.HALF_DOWN);
            BigDecimal[] tocal = new BigDecimal[data.length];
            for (int i = 0; i < data.length; i++) {
                tocal[i] = new BigDecimal(data[i]);
            }
            BigDecimal BVarianceMagnitude = new BigDecimal(0);
            for (int i = 0; i < data.length; i++) {
                BVarianceMagnitude = BVarianceMagnitude.add(tocal[i].pow(2), mc);
            }
            double dVarianceMagnitude = Math.pow(BVarianceMagnitude.doubleValue(), 0.5);
            return (float) dVarianceMagnitude;
        }

        public class Statistics {
            float[] data;
            int size;

            public Statistics(float[] data) {
                this.data = data;
                size = data.length;
            }

            float getMean() {
                float sum = (float) 0.0;
                for (float a : data)
                    sum += a;
                return sum / size;
            }

            float getVariance() {
                float mean = getMean();
                float temp = 0;
                for (float a : data)
                    temp += (mean - a) * (mean - a);
                return temp / size;
            }

            float getStdDev() {
                return (float) Math.sqrt(getVariance());
            }

            public float median() {
                Arrays.sort(data);

                if (data.length % 2 == 0) {
                    return (float) ((data[(data.length / 2) - 1] + data[data.length / 2]) / 2.0);
                } else {
                    return data[data.length / 2];
                }
            }
        }
    }
}
