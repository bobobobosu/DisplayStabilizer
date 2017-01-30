package com.project.nicki.displaystabilizer;

import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Filters.filterSensorData;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.util.DoubleArray;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nicki on 12/23/2016.
 * globalvariable.java
 * # sensor values
 * # states
 * # others
 */

public class globalvariable {
    //// # sensor values
    // # accelerometer sensor
    public float[] AccelerometerLinearVal = new float[]{0, 0, 0};
    public float[] AccelerometerVal = new float[]{0, 0, 0};
    public float[] AccelerometerCali = new float[]{0, 0, 0};
    public CircularBuffer2 AcceBuffer = new CircularBuffer2(50);
    // # gyroscope sensor
    public float[] GyroscopeVal = new float[]{0, 0, 0};
    // # magnet sensor
    public float[] MagnetometerVal = new float[]{0, 0, 0};
    public CircularBuffer2 MagnBuffer = new CircularBuffer2(50);
    // # rotation sensor
    public double[][] RotationVal = new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
    // # quaternion sensor
    public double[] QuaternionVal = new double[]{1, 0, 0, 0};
    public Quaternion fromDevice2World = new Quaternion(QuaternionVal[0], QuaternionVal[1], QuaternionVal[2], QuaternionVal[3]);
    public Quaternion initQua = new Quaternion(QuaternionVal[0], QuaternionVal[1], QuaternionVal[2], QuaternionVal[3]);
    public boolean inited = false;
    // # orientation sensor
    public float[] OrientationVal = new float[]{0, 0, 0};
    // # Stop Detector
    public boolean[] StopDetectorVal = new boolean[]{false, false, false};
    // # Static Detector
    public boolean StaticVal = false;
    public double StaticVarMagVal = 0;
    // # Hover Position
    public float[] HoverVal = new float[]{0, 0};


    //// # NEW sensor values
    // # accelerometer sensor
    public SensorData sAccelerometerLinearVal = new SensorData(50, 3);
    public SensorData sAccelerometerVal = new SensorData(50, 3);
    public SensorData sAccelerometerVal_world = new SensorData(50, 3);
    public SensorData sAccelerometerLinearVal_world = new SensorData(50, 3);
    public float const_g = 9.78952f;
    public BigDecimal gravval = new BigDecimal(const_g);
    public float[] sAccelerometerCali = new float[]{0, 0, 0};
    // # rotation sensor
    public SensorData sRotationVal = new SensorData(50, 9);
    // # quaternion sensor
    public Quaternion sDevice2World = new Quaternion(0, 0, 0, 1);
    public SensorData sQuaternionVal = new SensorData(50, 4);
    // # Stop Detector
    public SensorData sStopDetectorVal = new SensorData(50, 3);
    // # Static Detector
    public SensorData sStaticVal = new SensorData(50, 1);
    public double sStaticVarMagVal = 0;
    // # Hover Position
    public SensorData sHoverVal = new SensorData(50, 2);
    // # Integration
    public SensorData mVelocity = new globalvariable.SensorData(1, 3);
    public SensorData mPosotion = new globalvariable.SensorData(1, 3);//// # filters

    //// # states
    public boolean calibrate_isrunning = false;

    ////Settings
    public String ipportVal = "192.168.42.149:11000";


    public static class CircularBuffer2 {
        int bufflength = 50;
        public boolean hasnew = false;

        public CircularBuffer2(int i) {
            bufflength = i;
        }

        public List<float[]> data = new ArrayList<>();

        public void add(float[] idata) {
            if (data.size() == 0) {
                data.add(idata);
            } else if (data.size() < bufflength) {
                data.add(idata);
            } else {
                data.add(idata);
                data.remove(0);
            }
            hasnew = true;
        }

        public void reset() {
            data = new ArrayList<>();
        }

        public boolean full() {
            return (data.size() >= bufflength);
        }
    }


    public static class SensorData {
        //FILTER
        filterSensorData mfilter = new filterSensorData(false, 1, 1, 1, false, Float.MAX_VALUE);
        float[] filterparam = new float[]{0, 1, 1, 1, 0, Float.MAX_VALUE};

        int bufflength = 50;
        int valueNum;
        public boolean hasnew = false;

        public void setFilterParam(String[] param) {
            //ex.  /sQuaternionVal:1:10:1:1:0:10000/
            float[] param_f = new float[]{
                    Float.parseFloat(param[1]),
                    Float.parseFloat(param[2]),
                    Float.parseFloat(param[3]),
                    Float.parseFloat(param[4]),
                    Float.parseFloat(param[5]),
                    Float.parseFloat(param[6])
            };
            this.filterparam = param_f;
        }

        public class Data {
            long timestamp;
            float[] values;

            public Data(long timestamp, float[] values) {
                this.timestamp = timestamp;
                this.values = values;
            }

            public float[] getValues() {
                return values;
            }

            public long getTimestamp() {
                return timestamp;
            }
        }

        public SensorData(int i, int valueNum) {
            this.bufflength = i;
            this.valueNum = valueNum;
        }

        //Data
        public Data initData = null;
        //main buffer
        public List<Data> buffer = new ArrayList<>();

        //operations
        public void add(long timestamp, float[] idata) {
            //FILTER
            //Log.d("param",String.valueOf(filterparam[2]));
            mfilter.paramUpdate(filterparam);

            idata = mfilter.filter(idata);

            if (initData == null) {
                initData = new Data(timestamp, idata);
            }
            if (buffer.size() == 0) {
                buffer.add(new Data(timestamp, idata));
            } else if (buffer.size() < bufflength) {
                buffer.add(new Data(timestamp, idata));
            } else {
                buffer.add(new Data(timestamp, idata));
                buffer.remove(0);
            }
            hasnew = true;
        }

        public void resetbuffer() {
            buffer = new ArrayList<>();
        }

        public void resetinit() {
            initData = null;
        }

        public boolean full() {
            return (buffer.size() >= bufflength);
        }

        //get
        public Data getInitData() {
            return initData;
        }

        public Data getFirstData() {
            if (buffer.size() > 0) {
                return buffer.get(0);
            } else {
                return new Data(System.currentTimeMillis(), new float[valueNum]);
            }

        }

        public Data getLatestData() {
            if (buffer.size() > 0) {
                return buffer.get(buffer.size() - 1);
            } else {
                return new Data(System.currentTimeMillis(), new float[valueNum]);
            }

        }
    }


}
