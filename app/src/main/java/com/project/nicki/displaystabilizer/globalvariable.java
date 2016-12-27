package com.project.nicki.displaystabilizer;

import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.util.DoubleArray;

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
    public float[] AccelerometerVal = new float[]{0,0,0};
    public float[] AccelerometerCali = new float[]{0,0,0};
    public CircularBuffer2 AcceBuffer = new CircularBuffer2(50);
    // # gyroscope sensor
    public float[] GyroscopeVal = new float[]{0,0,0};
    // # rotation sensor
    public double[][] RotationVal = new double[][]{{1,0,0},{0,1,0},{0,0,1}};
    // # quaternion sensor
    public double[] QuaternionVal = new double[]{1,0,0,0};
    public Quaternion fromDevice2World = new Quaternion(QuaternionVal[0], QuaternionVal[1], QuaternionVal[2], QuaternionVal[3]);
    public Quaternion initQua = new Quaternion(QuaternionVal[0], QuaternionVal[1], QuaternionVal[2], QuaternionVal[3]);
    public boolean inited = false;
    // # orientation sensor
    public float[] OrientationVal = new float[]{0,0,0};
    // # Stop Detector
    public boolean[] StopDetectorVal = new boolean[]{false, false, false};
    // # Static Detector
    public boolean StaticVal = false;
    public double StaticVarMagVal = 0;

    //// # states
    public boolean  calibrate_isrunning = false;






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
}
