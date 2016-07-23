package com.project.nicki.displaystabilizer.dataprovider;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Matrix3f;
import android.renderscript.Matrix4f;
import android.util.Log;

import com.project.nicki.displaystabilizer.UI.DemoDrawUI;
import com.project.nicki.displaystabilizer.UI.UIv1.UIv1_draw0;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.dataprocessor.proAcceGyroCali2;
import com.project.nicki.displaystabilizer.dataprocessor.proAcceGyroCali3;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Matrix2Quaternion;
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
import java.util.StringTokenizer;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by nickisverygood on 12/17/2015.
 */
public class getAcceGyro implements Runnable {
    private Handler sensor_ThreadHandler;
    private HandlerThread sensor_Thread;
    //Acce Calibration
    public static float[] AcceCaliFloat = new float[]{0,0,0};
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
    public static StopDetector mstopdetector = new StopDetector();
    public static CircularBuffer2 AcceBuffer = new CircularBuffer2(50);
    public getAcceGyro(Context context) {
        mContext = context;
    }

    public getAcceGyro() {
    }

    @Override
    public void run() {
        //sensor
        sensor_Thread = new HandlerThread("sensor handler");
        sensor_Thread.start();
        sensor_ThreadHandler=new Handler(sensor_Thread.getLooper());

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
        final proAcceGyroCali3 mproAcceGyroCali3 = new proAcceGyroCali3(mContext);
        //mproAcceGyroCali.TEST();
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mLSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
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
                final SensorEvent calievent = event;
                //Calibration
                //Log.d("calibration",String.valueOf(AcceCaliFloat[0]+" "+AcceCaliFloat[1]+" "+AcceCaliFloat[2]));
                if(calievent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
                    calievent.values[0] = calievent.values[0]-AcceCaliFloat[0];
                    calievent.values[1] = calievent.values[1]-AcceCaliFloat[1];
                    calievent.values[2] = calievent.values[2]-AcceCaliFloat[2];
                }




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

                //mproAcceGyroCali.Controller(calievent);

                switch (calievent.sensor.getType()) {
                    case Sensor.TYPE_LINEAR_ACCELERATION:
                        isStatic = mstaticsensor.getStatic(calievent.values);
                        mstopdetector.update(calievent.values);
                        AcceBuffer.add(calievent.values);
                        new LogCSV(init.rk4_Log+"calRk4", String.valueOf(getAcceGyro.mstopdetector.getStopped(0)),
                                new BigDecimal(String.valueOf(calievent.timestamp)).toPlainString(),
                                calievent.values[0],
                                calievent.values[1],
                                calievent.values[2]);
                        //if(DemoDraw2.drawing==0 || DemoDraw2.drawing==1){
                        //mgetValusHT_ACCE_handler.post(new Runnable() {
                        //@Override
                        //public void run() {
                        //init.initSensorCollection.append(new SensorCollect.sensordata(System.currentTimeMillis(), calievent.values, SensorCollect.sensordata.TYPE.ACCE));
                        //}
                        //});
                        //}
                }

                float[] mRotationMatrix = new float[16];
                final float[] orientationVals = new float[4];

                if(calievent.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR){

                    // Convert the rotation-vector to a 4x4 matrix.
                    SensorManager.getRotationMatrixFromVector(mRotationMatrix,
                            calievent.values);
                    SensorManager
                            .remapCoordinateSystem(mRotationMatrix,
                                    SensorManager.AXIS_X, SensorManager.AXIS_Z,
                                    mRotationMatrix);
                    SensorManager.getOrientation(mRotationMatrix, orientationVals);

                    // Optionally convert the result from radians to degrees
                    orientationVals[0] = orientationVals[0];
                    orientationVals[1] = orientationVals[1];
                    orientationVals[2] = orientationVals[2];

                }
                if (calievent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    mGravity = calievent.values;
                }
                if (calievent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    mGeomagnetic = calievent.values;
                }
                final float orientation[] = new float[3];
                if (mGravity != null && mGeomagnetic != null) {

                    float R[] = new float[9];
                    float I[] = new float[9];
                    boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                    //R =new float[] {0.5f,0.866f,0f,-0.866f,0.5f,0f,0f,0f,1f};
                    //float[] q = Matrix2Quaternion.setQuatFromMatrix(new Matrix3f(R));
                    //Log.i("QUA",q[0]+" "+q[1]+" "+q[2]+" "+q[3]);
                    if (success) {
                        SensorManager.getOrientation(R, orientation);
                        /*
                        new LogCSV(init.rk4_Log + " ll", String.valueOf(getAcceGyro.mstopdetector.getStopped(0)),
                                String.valueOf(System.currentTimeMillis()),
                                mGravity[0],
                                mGravity[1],
                                mGravity[2],
                                mGeomagnetic[0],
                                mGeomagnetic[1],
                                mGeomagnetic[2],
                                orientation[0],
                                orientation[1],
                                orientation[2],
                        R[0],R[1],R[2],R[3],R[4],R[5],R[6],R[7],R[8]);
                        */
                        //if(DemoDraw2.drawing==0 || DemoDraw2.drawing==1) {
                        //mgetValusHT_ORIEN_handler.post(new Runnable() {
                        //@Override
                        //public void run() {
                        //init.initSensorCollection.append(new SensorCollect.sensordata(System.currentTimeMillis(), orientation, SensorCollect.sensordata.TYPE.ORIEN_radian));
                        //}
                        //});
                        //}
                    }else {
                    }
                }

                try {
                    if(orientationVals[0]!=0 && orientation[0]!=0){
                        Log.i("Orien",String.valueOf(orientationVals[0]+" "+orientation[0]));
                    }
                }catch (Exception ex){

                }


                sensor_ThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            mproAcceGyroCali3.Controller(String.valueOf(calievent.sensor.getStringType()),calievent.timestamp,calievent.values);
                        }catch (Exception ex){
                            Log.d("getAcce",String.valueOf(ex));
                        }
                        try{
                            mproAcceGyroCali3.Controller("android.sensor.getR",calievent.timestamp,orientation);
                        }catch (Exception ex){}

                    }
                });

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
    public static class StopDetector{
        public int[] error_threshold =new int[]{25,25,25};
        int timespan_threshold = 25;
        public int[] switchstop_TRUE = new int[]{0,0,0};
        public boolean[] switchstop = new boolean[]{false,false,false};
        public boolean[] changed = new boolean[]{false,false,false};
        public int[] stack = new int[]{0,0,0};
        private float[] prev = new float[]{0,0,0};
        public void update(float[] data){
            for (int i=0;i<3;i++){
                if(data[i]*prev[i]>0){
                    stack[i]++;
                    error_threshold[i] = stack[i];
                    changed[i]=false;
                }else if(data[i]*prev[i]<0){
                    if(stack[i]>timespan_threshold){
                        switchstop[i] = !switchstop[i];
                        changed[i]=true;
                    }else {
                        changed[i]=false;
                    }
                    stack[i]=0;
                }

                if(switchstop[i]){
                    switchstop_TRUE[i]++;
                    if(switchstop_TRUE[i]>error_threshold[i]*2){
                        switchstop[i] = false;
                    }
                }else {
                    switchstop_TRUE[i]=0;
                }

                if((switchstop_TRUE[0]>100 || switchstop_TRUE[0]>100 || switchstop_TRUE[0]>100 )&& UIv1_draw0.calibrate_isrunning==false){
                    Log.d("DEBUGG",String.valueOf(switchstop_TRUE[0]+" "+ switchstop_TRUE[0]+" "+  switchstop_TRUE[0]+" "+   UIv1_draw0.calibrate_isrunning));
                    //UIv1_draw0.calibrate.sendEmptyMessage(0);
                }
            }
            prev = data.clone();
        }
        public boolean getStopped(int i){
            return (switchstop[i]);
        }
        public int[] getstack(){
            return stack;
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

    public static class CircularBuffer2{
        int bufflength = 50;
        public boolean hasnew = false;
        public CircularBuffer2(int i){
            bufflength = i;
        }
        public List<float[]> data = new ArrayList<>();
        public void add(float[] idata){
            if(data.size()==0){
                data.add(idata);
            }else if(data.size()< bufflength){
                data.add(idata);
            }else {
                data.add(idata);
                data.remove(0);
            }
            hasnew = true;
        }
        public void reset(){
            data = new ArrayList<>();
        }
        public boolean full(){
            return (data.size()>= bufflength);
        }
    }
}
