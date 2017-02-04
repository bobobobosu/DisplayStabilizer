package com.project.nicki.displaystabilizer.dataprovider;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.MotionEstimation3;
import com.project.nicki.displaystabilizer.dataprocessor.MotionEstimation4.MotionEstimation4;
import com.project.nicki.displaystabilizer.dataprovider.orientationProvider.ImprovedOrientationSensor1Provider;
import com.project.nicki.displaystabilizer.dataprovider.orientationProvider.OrientationProvider;
import com.project.nicki.displaystabilizer.globalvariable;
import com.project.nicki.displaystabilizer.init;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.ejml.simple.SimpleMatrix;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nicki on 1/26/2017.
 * getAcceGyro.java
 * DO:
 * ## 5 sensors
 * # coordinate device2world quaternion
 * # accelerometer_raw sensor
 * # accelerometer_linear sensor
 * # orientation sensor
 * # Stop Detector
 * # Static Detector
 * ## buffer format
 * # Sensor Object
 * # pass sensor buffer to MotionEstimation3
 */


public class SensorCollection implements Runnable {
    //// # accelerometer_raw sensor & accelerometer_linear sensor:　declare variables
    Context mContext;
    private Handler sensor_ThreadHandler;
    private HandlerThread sensor_Thread;
    private SensorManager mSensorManager;
    private Sensor mLSensor; //accelerometer_linear sensor
    private Sensor mRSensor; //accelerometer_raw sensor
    private SensorEventListener mSensorEventListener;
    private HandlerThread mHandlerThread;
    //// # Static Sensor: declare variables
    public StopDetector mstopdetector = new StopDetector();

    //// # orientation sensor: declare variables
    public static OrientationProvider currentOrientationProvider;

    //// # passtoME
    public MotionEstimation4 mmotionestimation = new MotionEstimation4(mContext);

    public SensorCollection(Context baseContext) {
        mContext = baseContext;
    }

    @Override
    public void run() {
        //// # orientation sensor: get quaternion
        currentOrientationProvider = new ImprovedOrientationSensor1Provider((SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE));

        //// # get sensor buffer from SensorManager:　init
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mLSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mRSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mHandlerThread = new HandlerThread("getAcceGyro");
        mHandlerThread.start();

        //// # pass sensor buffer to MotionEstimation3: init
        sensor_Thread = new HandlerThread("sensor handler");
        sensor_Thread.start();
        sensor_ThreadHandler = new Handler(sensor_Thread.getLooper());
        final MotionEstimation4 mmotionestimation = new MotionEstimation4(mContext);
        //// # pass sensor buffer to MotionEstimation3
        sensor_ThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    MotionEstimation3 mproAcceGyroCali3;
                } catch (Exception ex) {
                    Log.d("getAcce", String.valueOf(ex));
                }
            }
        });

        //// # Static Detector: init
        final StaticSensor mstaticsensor = new StaticSensor();

        //// # get sensor buffer from SensorManager:　get buffer
        Handler mHandler = new Handler(mHandlerThread.getLooper());
        mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(final SensorEvent event) {
                final SensorEvent calievent = event;
                //// # accelerometer_linear sensor
                if (calievent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION && false) {
                    //calievent.values[0] = calievent.values[0] - init.initglobalvariable.AccelerometerCali[0];
                    //calievent.values[1] = calievent.values[1] - init.initglobalvariable.AccelerometerCali[1];
                    //calievent.values[2] = calievent.values[2] - init.initglobalvariable.AccelerometerCali[2];
                    //update globalvariable
                    init.initglobalvariable.AccelerometerLinearVal = calievent.values.clone();
                    //init.initglobalvariable.sAccelerometerLinearVal.add(calievent.timestamp, calievent.values.clone());
                }

                //// # accelerometer_raw sensor
                if (calievent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    init.initglobalvariable.AccelerometerVal = calievent.values;
                    init.initglobalvariable.sAccelerometerVal.add(calievent.timestamp, calievent.values.clone());
                }

                //// # alt_accelerometer_linear sensor
                //obtain device2world
                globalvariable.SensorData.Data currData_Acce = init.initglobalvariable.sAccelerometerVal.getLatestData();
                //Rotate to world coordinate
                Rotation device2world_rot = new Rotation(
                        init.initglobalvariable.sDevice2World.getQ0(),
                        init.initglobalvariable.sDevice2World.getQ1(),
                        init.initglobalvariable.sDevice2World.getQ2(),
                        init.initglobalvariable.sDevice2World.getQ3(), false);

                double[][] currRot = device2world_rot.getMatrix();
                SimpleMatrix m = new SimpleMatrix(currRot);
                SimpleMatrix r = new SimpleMatrix(new double[][]{
                        {currData_Acce.getValues()[0]},
                        {currData_Acce.getValues()[1]},
                        {currData_Acce.getValues()[2]},
                });
                SimpleMatrix result = m.mult(r);
                init.initglobalvariable.sAccelerometerVal_world.add(calievent.timestamp, new float[]{
                        (float) result.get(0, 0),
                        (float) result.get(1, 0),
                        (float) (result.get(2, 0))
                });



                // Substract Gravity
                init.initglobalvariable.sAccelerometerLinearVal_world.add(calievent.timestamp, new float[]{
                        init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[0],
                        init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[1],
                        (float) (init.initglobalvariable.sAccelerometerVal_world.getLatestData().getValues()[2] - 9.8149735275)
                });
                //// # alt accelerometer linear acce
                double[] world = new double[]{
                        init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues()[0],
                        init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues()[1],
                        init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues()[2]
                };
                Vector3D world2device = device2world_rot.applyInverseTo(new Vector3D(world));
                init.initglobalvariable.sAccelerometerLinearVal.add(calievent.timestamp, new float[]{
                        ((float) world2device.getX()),
                        ((float) world2device.getY()),
                        ((float) world2device.getZ())
                });



                //// # Static Detector
                boolean isStatic = true;
                switch (calievent.sensor.getType()) {
                    case Sensor.TYPE_LINEAR_ACCELERATION:
                        isStatic = mstaticsensor.getStatic(calievent.values);
                        //update globalvariable
                        init.initglobalvariable.sStaticVal.add(calievent.timestamp, new float[]{(isStatic) ? 1 : 0});
                        init.initglobalvariable.StaticVarMagVal = mstaticsensor.getVarianceMagnitude(calievent.values);
                }

                //// # Stop Detector
                switch (calievent.sensor.getType()) {
                    case Sensor.TYPE_LINEAR_ACCELERATION:
                        mstopdetector.update(calievent.values);
                        //update globalvariable
                        init.initglobalvariable.StopDetectorVal = mstopdetector.switchstop;
                        init.initglobalvariable.sStopDetectorVal.add(calievent.timestamp, new float[]{
                                (mstopdetector.switchstop[0]) ? 1 : 0,
                                (mstopdetector.switchstop[1]) ? 1 : 0,
                                (mstopdetector.switchstop[2]) ? 1 : 0});
                }
                mmotionestimation.trigger();
            }


            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        //// # get sensor buffer from SensorManager:　registerListener
        mSensorManager.registerListener(mSensorEventListener, mLSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
        mSensorManager.registerListener(mSensorEventListener, mRSensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
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
                variance[i] = new StaticSensor.Statistics(mdataset[i]).getVariance();
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
                init.initglobalvariable.StaticVal = false;
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
}
