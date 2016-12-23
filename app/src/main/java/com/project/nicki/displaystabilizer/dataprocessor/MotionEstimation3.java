package com.project.nicki.displaystabilizer.dataprocessor;

/**
 * Created by nickisverygood on 3/9/2016.
 * MotionEstimation3.java
 * DO:
 * # gen RotationMatrix from Quaternion
 * # gen Quaternion from RotationMatrix
 * # pass motion to initStabilize
 */

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;
import com.project.nicki.displaystabilizer.init;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class MotionEstimation3 {
    ////Current State
    public static double[][] currRot = new double[][]{{1d, 0d, 0d}, {0d, 1d, 0d}, {0d, 0d, 1d}};
    boolean inited = false;
    ////Variables
    Context mContext;
    calRk4 mcalRk4 = new calRk4();
    public float[] currOrientation = new float[]{0, 0, 0};
    double[] currQuaternionArray = null;
    SensorCollect.sensordata currSensordata = null;
    Quaternion initQua = null;
    ///Filters
    DescriptiveStatistics currOrientation_Filter0 = new DescriptiveStatistics(20);
    DescriptiveStatistics currOrientation_Filter1 = new DescriptiveStatistics(20);
    DescriptiveStatistics currOrientation_Filter2 = new DescriptiveStatistics(20);


    public MotionEstimation3(Context context) {
        mContext = context;
    }

    public void Controller() {

        //// # OBSOLETE
        /*

        //Quaternion2Rot
            Quaternion relative2init = fromDevice2World.multiply(initQua.getInverse()).getInverse();
            float[] currRotf_new = new float[]{1, 0, 0, 0, 1, 0, 0, 0, 1};
            SensorManager.getRotationMatrixFromVector(currRotf_new, new float[]{(float) relative2init.getQ1(), (float) relative2init.getQ2(), (float) relative2init.getQ3(), (float) relative2init.getQ0()});

            currRot = new double[][]{
                    {(double) currRotf_new[0],
                            (double) currRotf_new[1],
                            (double) currRotf_new[2]},
                    {(double) currRotf_new[3],
                            (double) currRotf_new[4],
                            (double) currRotf_new[5]},
                    {(double) currRotf_new[6],
                            (double) currRotf_new[7],
                            (double) currRotf_new[8]}};

        if (Objects.equals(type, "android.sensor.rotation_vector") && false) {
            currQuaternionArray = new double[]{values[0], values[1], values[2], values[3]};

            //update init
            if (!Arrays.equals(currQuaternionArray, new double[]{1, 0, 0, 0}) && inited == false) {
                initQua = new Quaternion(currQuaternionArray[0], currQuaternionArray[1], currQuaternionArray[2], currQuaternionArray[3]);
                inited = true;
            }

            //update if new stroke
            if (DemoDraw3.pending_quaternion_reset == true && false) {
                initQua = new Quaternion(currQuaternionArray[0], currQuaternionArray[1], currQuaternionArray[2], currQuaternionArray[3]);
                DemoDraw3.pending_quaternion_reset = false;
            }

            //Quaternion2Rot
            Quaternion relative2init = new Quaternion(1, 0, 0, 0);
            float[] rotvec = new float[]{1, 0, 0, 0, 1, 0, 0, 0, 1};
            float[] rotvec_raw = new float[]{1, 0, 0, 0, 1, 0, 0, 0, 1};
            SensorManager.getRotationMatrixFromVector(rotvec_raw, new float[]{(float) relative2init.getQ0(), (float) relative2init.getQ1(), (float) relative2init.getQ2(), (float) relative2init.getQ3()});
            float[] myorien = new float[]{0, 0, 0};
            SensorManager.getOrientation(rotvec_raw, myorien);
            SensorManager.remapCoordinateSystem(rotvec_raw, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_Y, rotvec);

            currRot = new double[][]{
                    {rotvec[0], rotvec[1], rotvec[2]},
                    {rotvec[3], rotvec[4], rotvec[5]},
                    {rotvec[6], rotvec[7], rotvec[8]}};
        }
           */

        //// # pass motion to initStabilize: currSensordata, currQuaternionArray, currOrientation

            currSensordata = new SensorCollect.sensordata(System.currentTimeMillis(), init.initglobalvariable.AccelerometerVal, SensorCollect.sensordata.TYPE.ACCE);
            Bundle bundle = new Bundle();
            bundle.putFloatArray("Pos", mcalRk4.calc(currSensordata).getData());
            bundle.putFloatArray("Orien", new float[]{init.initglobalvariable.OrientationVal[0], init.initglobalvariable.OrientationVal[1], init.initglobalvariable.OrientationVal[2]});
            bundle.putDoubleArray("Quaternion", init.initglobalvariable.QuaternionVal);
            bundle.putLong("Time", currSensordata.getTime());
            init.initStabilize.set_Sensor(bundle);
            currSensordata = null;

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
