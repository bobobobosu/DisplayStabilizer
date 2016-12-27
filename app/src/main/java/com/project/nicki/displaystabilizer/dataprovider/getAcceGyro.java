package com.project.nicki.displaystabilizer.dataprovider;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.MotionEstimation3;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.dataprocessor.calRk4;
import com.project.nicki.displaystabilizer.init;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nickisverygood on 12/17/2015.
 * getAcceGyro.java
 * DO:
 * # get sensor data from SensorManager
 * # apply accelerometer 
 * # accelerometer sensor
 * # orientation sensor
 * # quaternion sensor
 * # Stop Detector
 * # Static Detector
 * # CircularBuffer2
 * # pass sensor data to MotionEstimation3
 */
public class getAcceGyro implements Runnable {

    //// # get sensor data from SensorManager:　declare variables
    Context mContext;
    private Handler sensor_ThreadHandler;
    private HandlerThread sensor_Thread;
    private SensorManager mSensorManager;
    private Sensor mGSensor; //gyro
    private Sensor mLSensor; //linear acce
    private Sensor mMSensor; //magn
    private Sensor mRSensor; //raw acce
    private Sensor mMASensor;
    private SensorEventListener mSensorEventListener;
    private HandlerThread mHandlerThread;


    //// # apply accelerometer :　declare variables
    public static Handler mgetValusHT_TOUCH_handler;
        private String TAG = "getAcceGyro";
    public static StopDetector mstopdetector = new StopDetector();
    //public static CircularBuffer2 AcceBuffer = new CircularBuffer2(50);

    //// # quaternion sensor
    ///Filters
    DescriptiveStatistics currOrientation_Filter0 = new DescriptiveStatistics(20);
    DescriptiveStatistics currOrientation_Filter1 = new DescriptiveStatistics(20);
    DescriptiveStatistics currOrientation_Filter2 = new DescriptiveStatistics(20);

    public getAcceGyro(Context context) {
        mContext = context;
    }

    public getAcceGyro() {
    }

    @Override
    public void run() {

        //// # get sensor data from SensorManager:　init
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mLSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        mRSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMASensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mHandlerThread = new HandlerThread("getAcceGyro");
        mHandlerThread.start();

        //// # pass sensor data to MotionEstimation3: init
        sensor_Thread = new HandlerThread("sensor handler");
        sensor_Thread.start();
        sensor_ThreadHandler = new Handler(sensor_Thread.getLooper());
        final MotionEstimation3 mproAcceGyroCali3 = new MotionEstimation3(mContext);

        //// # Static Detector: init
        final StaticSensor mstaticsensor = new StaticSensor();

        //// # get sensor data from SensorManager:　get data
        Handler mHandler = new Handler(mHandlerThread.getLooper());
        mSensorEventListener = new SensorEventListener() {
            float[] mGravity;
            float[] mGeomagnetic;

            @Override
            public void onSensorChanged(final SensorEvent event) {
                final SensorEvent calievent = event;


                //// # apply accelerometer : iterate through events and calculate AcceCaliFloat
                if (calievent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    calievent.values[0] = calievent.values[0] - init.initglobalvariable.AccelerometerCali[0];
                    calievent.values[1] = calievent.values[1] - init.initglobalvariable.AccelerometerCali[1];
                    calievent.values[2] = calievent.values[2] - init.initglobalvariable.AccelerometerCali[2];
                    //update globalvariable
                    init.initglobalvariable.AccelerometerVal =  calievent.values.clone();
                }


                //// # Static Detector
                boolean isStatic = true;
                switch (calievent.sensor.getType()) {
                    case Sensor.TYPE_LINEAR_ACCELERATION:
                        isStatic = mstaticsensor.getStatic(calievent.values);
                        init.initglobalvariable.AcceBuffer.add(calievent.values);
                        //update globalvariable
                        init.initglobalvariable.StaticVal =  isStatic;
                        init.initglobalvariable.StaticVarMagVal = mstaticsensor.getVarianceMagnitude(calievent.values);
                }

                //// # Stop Detector
                switch (calievent.sensor.getType()) {
                    case Sensor.TYPE_LINEAR_ACCELERATION:
                        mstopdetector.update(calievent.values);
                        init.initglobalvariable.AcceBuffer.add(calievent.values);
                        //update globalvariable
                        init.initglobalvariable.StopDetectorVal =  mstopdetector.switchstop;
                }


                //// # gyroscope sensor
                if (calievent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    //update globalvariable
                    init.initglobalvariable.GyroscopeVal =  calievent.values;
                }

                //// # quaternion sensor
                //Euler2Quaternion
                float[] currOrientation;
                double[] currQuaternionArray;
                currOrientation = init.initglobalvariable.OrientationVal.clone();
                currOrientation_Filter0.addValue(currOrientation[0]);
                currOrientation_Filter1.addValue(currOrientation[1]);
                currOrientation_Filter2.addValue(currOrientation[2]);
                currOrientation[0] = (float) currOrientation_Filter0.getMean();
                currOrientation[1] = (float) currOrientation_Filter1.getMean();
                currOrientation[2] = (float) currOrientation_Filter2.getMean();
                currQuaternionArray = getQuaternionfromEuler(currOrientation[0], currOrientation[1], currOrientation[2]);
                //update init
                if (!Arrays.equals(init.initglobalvariable.QuaternionVal, new double[]{1, 0, 0, 0}) && init.initglobalvariable.inited == false) {
                    init.initglobalvariable.initQua = new Quaternion(init.initglobalvariable.QuaternionVal[0], init.initglobalvariable.QuaternionVal[1], init.initglobalvariable.QuaternionVal[2], init.initglobalvariable.QuaternionVal[3]);
                    init.initglobalvariable.inited = true;
                }
                //update if new stroke
                if (DemoDraw3.pending_quaternion_reset == true) {
                    init.initglobalvariable.initQua = new Quaternion(currQuaternionArray[0], currQuaternionArray[1], currQuaternionArray[2], currQuaternionArray[3]);
                    currOrientation_Filter0.clear();
                    currOrientation_Filter1.clear();
                    currOrientation_Filter2.clear();
                    DemoDraw3.pending_quaternion_reset = false;
                }
                //get inverse
                init.initglobalvariable.fromDevice2World = new Quaternion(currQuaternionArray[0], currQuaternionArray[1], currQuaternionArray[2], currQuaternionArray[3]);
                //update globalvariable
                init.initglobalvariable.QuaternionVal = currQuaternionArray.clone();


                //// # rotation sensor
                init.initglobalvariable.initQua = new Quaternion(init.initglobalvariable.QuaternionVal[0], init.initglobalvariable.QuaternionVal[1], init.initglobalvariable.QuaternionVal[2],  init.initglobalvariable.QuaternionVal[3]);
                Quaternion relative2init = init.initglobalvariable.fromDevice2World.multiply(init.initglobalvariable.initQua.getInverse()).getInverse();
                float[] currRotf_new = new float[]{1, 0, 0, 0, 1, 0, 0, 0, 1};
                SensorManager.getRotationMatrixFromVector(currRotf_new, new float[]{(float) relative2init.getQ1(), (float) relative2init.getQ2(), (float) relative2init.getQ3(), (float) relative2init.getQ0()});
                init.initglobalvariable.RotationVal = new double[][]{
                        {(double) currRotf_new[0],
                                (double) currRotf_new[1],
                                (double) currRotf_new[2]},
                        {(double) currRotf_new[3],
                                (double) currRotf_new[4],
                                (double) currRotf_new[5]},
                        {(double) currRotf_new[6],
                                (double) currRotf_new[7],
                                (double) currRotf_new[8]}};


                //// # orientation sensor
                float[] mRotationMatrix = new float[16];
                final float[] orientationVals = new float[4];

                if (calievent.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
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
                    if (success) {
                        SensorManager.getOrientation(R, orientation);
                    } else {
                    }
                }
                //update globalvariable
                init.initglobalvariable.OrientationVal = orientation.clone();


                //// # pass sensor data to MotionEstimation3
                sensor_ThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mproAcceGyroCali3.Controller();
                        } catch (Exception ex) {
                            Log.d("getAcce", String.valueOf(ex));
                        }
                    }
                });

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        //// # get sensor data from SensorManager:　registerListener
        mSensorManager.registerListener(mSensorEventListener, mGSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        mSensorManager.registerListener(mSensorEventListener, mLSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        mSensorManager.registerListener(mSensorEventListener, mMSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        mSensorManager.registerListener(mSensorEventListener, mRSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        mSensorManager.registerListener(mSensorEventListener, mMASensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
    }


    //// # Stop Detector
    public static class StopDetector {
        public int[] error_threshold = new int[]{25, 25, 25};
        int timespan_threshold = 25;
        public int[] switchstop_TRUE = new int[]{0, 0, 0};
        public boolean[] switchstop = new boolean[]{false, false, false};
        public boolean[] changed = new boolean[]{false, false, false};
        public int[] stack = new int[]{0, 0, 0};
        private float[] prev = new float[]{0, 0, 0};

        public void update(float[] data) {
            for (int i = 0; i < 3; i++) {
                if (data[i] * prev[i] > 0) {
                    stack[i]++;
                    error_threshold[i] = stack[i];
                    changed[i] = false;
                } else if (data[i] * prev[i] < 0) {
                    if (stack[i] > timespan_threshold) {
                        switchstop[i] = !switchstop[i];
                        changed[i] = true;
                    } else {
                        changed[i] = false;
                    }
                    stack[i] = 0;
                }

                if (switchstop[i]) {
                    switchstop_TRUE[i]++;
                    if (switchstop_TRUE[i] > error_threshold[i] * 2) {
                        switchstop[i] = false;
                    }
                } else {
                    switchstop_TRUE[i] = 0;
                }

            }
            prev = data.clone();
        }

        public boolean getStopped(int i) {
            return (switchstop[i]);
        }
    }


    //// # Static Detector
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
                init.initglobalvariable.StaticVal= false;
            } else {
                init.initglobalvariable.StaticVal = true;
            }
            return init.initglobalvariable.StaticVal;
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

    public final double[] getQuaternionfromEuler(double mroll, double mpitch, double myaw) {
        //note that original source mistaken roll as yaw, thus switch them (input parms still yaw,pithh,yaw [zy'x''])
        float yaw = (float) myaw;
        float pitch = (float) mpitch;
        float roll = (float) mroll;
        final float hr = roll * 0.5f;
        final float shr = (float) Math.sin(hr);
        final float chr = (float) Math.cos(hr);
        final float hp = pitch * 0.5f;
        final float shp = (float) Math.sin(hp);
        final float chp = (float) Math.cos(hp);
        final float hy = yaw * 0.5f;
        final float shy = (float) Math.sin(hy);
        final float chy = (float) Math.cos(hy);
        final float chy_shp = chy * shp;
        final float shy_chp = shy * chp;
        final float chy_chp = chy * chp;
        final float shy_shp = shy * shp;

        float x = (chy_shp * chr) + (shy_chp * shr); // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) * sin(roll/2)
        float y = (shy_chp * chr) - (chy_shp * shr); // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) * sin(roll/2)
        float z = (chy_chp * shr) - (shy_shp * chr); // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) * cos(roll/2)
        float w = (chy_chp * chr) + (shy_shp * shr); // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) * sin(roll/2)
        return new double[]{w, x, y, z};
    }




}
