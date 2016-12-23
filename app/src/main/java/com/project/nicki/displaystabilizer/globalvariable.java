package com.project.nicki.displaystabilizer;

import org.apache.commons.math3.complex.Quaternion;

/**
 * Created by nicki on 12/23/2016.
 * globalvariable.java
 * # sensor values
 * # states
 */

public class globalvariable {
    //// # sensor values
    // # accelerometer sensor
    public float[] AccelerometerVal = new float[]{0,0,0};
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

    //// # states

}
