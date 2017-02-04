package com.project.nicki.displaystabilizer.stabilization;

import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.Interpolation;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.dataprovider.representation.MatrixF4x4;
import com.project.nicki.displaystabilizer.globalvariable;
import com.project.nicki.displaystabilizer.init;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.genetics.ListPopulation;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nicki on 2/1/2017.
 */

public class TranslationProvider {
    private float[] initOffset = new float[]{0, 0, 0};
    private float[] prevPosition = new float[]{0,0,0};
    private float[] currPosition = new float[]{0,0,0};
    private float[] currVelocity = new float[]{0,0,0};

    float tx =0 ;
    float ty =0 ;
    float tz =0 ;



    public float[] recoveryFactor = new float[]{1000f,1000f};
    private Interpolation minterpolate = new Interpolation();

    public float[] TranslationOnInitPlane(Quaternion currentQua, Quaternion initQua, long time, int nthtouchpoint) {

        //tx++;
       // ty++;
       // tz++;
       // init.initglobalvariable.mPosotion.add(System.currentTimeMillis(), new float[]{tx,ty,tz});




        //init
        if (nthtouchpoint == 0) {
            initOffset = init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues();
            Log.d("First", "HI");
        }

        //get data
        globalvariable.SensorData copy_sAccelerometerLinearVal_world = init.initglobalvariable.sAccelerometerLinearVal_world;
        globalvariable.SensorData copy_sAccelerometerLinearVal = init.initglobalvariable.sAccelerometerLinearVal;
        globalvariable.SensorData copy_sAccelerometerVal = init.initglobalvariable.sAccelerometerVal;
        globalvariable.SensorData copy_sAccelerometerVal_world = init.initglobalvariable.sAccelerometerVal_world;
        globalvariable.SensorData copy_mVelocity = init.initglobalvariable.mVelocity;
        globalvariable.SensorData copy_mPosotion = init.initglobalvariable.mPosotion;

        //interpolation
        SensorCollect.sensordata rawPosition = minterpolate.getbyTime(time, SensorData2sensordata(copy_sAccelerometerLinearVal_world ));
        currPosition = new float[]{
                rawPosition.getData()[0] - initOffset[0],
                rawPosition.getData()[1] - initOffset[1],
                rawPosition.getData()[2] - initOffset[2]
        };
        currVelocity = new float[]{
                init.initglobalvariable.mVelocity.getLatestData().getValues()[0],
                init.initglobalvariable.mVelocity.getLatestData().getValues()[1],
                init.initglobalvariable.mVelocity.getLatestData().getValues()[2]

        };


        //get position
        SimpleMatrix deltaPosition = new SimpleMatrix(new double[][]{
                {currPosition[0] - prevPosition[0]},
                {currPosition[1] - prevPosition[1]},
                {currPosition[2] - prevPosition[2]}

        });
        com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion q_initQua = new com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion();
        q_initQua.setXYZW((float) initQua.getQ1(), (float) initQua.getQ2(), (float) initQua.getQ3(), (float) initQua.getQ0());
        MatrixF4x4 initQua_m4x4 = q_initQua.getMatrix4x4();
        SimpleMatrix initQua_m = new SimpleMatrix(new double[][]{
                {initQua_m4x4.getMatrix()[0], initQua_m4x4.getMatrix()[1], initQua_m4x4.getMatrix()[2]},
                {initQua_m4x4.getMatrix()[4], initQua_m4x4.getMatrix()[5], initQua_m4x4.getMatrix()[6]},
                {initQua_m4x4.getMatrix()[8], initQua_m4x4.getMatrix()[9], initQua_m4x4.getMatrix()[10]}
        });
        SimpleMatrix returnPos = initQua_m.mult(deltaPosition);


        prevPosition = currPosition;
        Log.d("delta", String.valueOf((float) returnPos.getMatrix().get(0, 0)*recoveryFactor[0] + " " +
                -(float) returnPos.getMatrix().get(1, 0)*recoveryFactor[1]));
        return new float[]{
                -(float) returnPos.getMatrix().get(0, 0)*recoveryFactor[0],
                -(float) returnPos.getMatrix().get(1, 0)*recoveryFactor[1]
        };
    }

    public float[] subtractOffset(float[] input) {
        for (int i = 0; i < input.length; i++) {
            input[i] = input[i] - initOffset[i];
        }
        return input;
    }

    public List<SensorCollect.sensordata> SensorData2sensordata(globalvariable.SensorData Sensordata) {
        List<SensorCollect.sensordata> returnsnesordataList = new ArrayList<>();
        for (int i = 0; i < Sensordata.buffer.size(); i++) {
            returnsnesordataList.add(new SensorCollect.sensordata(Sensordata.buffer.get(i).getTimestamp(),
                    Sensordata.buffer.get(i).getValues()));
        }
        return returnsnesordataList;
    }
}
