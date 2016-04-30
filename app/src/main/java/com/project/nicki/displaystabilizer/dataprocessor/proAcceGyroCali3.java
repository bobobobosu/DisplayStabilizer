package com.project.nicki.displaystabilizer.dataprocessor;

/**
 * Created by nickisverygood on 3/9/2016.
 */

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.dataprocessor.utils.MatMultiply;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat.Matrix3D;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat.Vector3D;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;
import com.project.nicki.displaystabilizer.init;

import java.math.BigDecimal;
import java.util.Objects;

public class proAcceGyroCali3 {
    public static float[] init_orien = new float[]{0,0,0};
    static float[] accmag_orientation = new float[]{0,0,0};

    Context mContext;
    calRk4 mcalRk4 = new calRk4();
    float[] mGravity = new float[3];
    SensorCollect.sensordata thissensordata = null;
    static float[] orientation = new float[]{0,0,0};


    public static void resetGlobal() {
        init_orien = accmag_orientation.clone();
    }
    public proAcceGyroCali3(Context context) {
        mContext = context;
    }


    public void Controller(SensorEvent mSensorEvent) {

        if (mSensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            thissensordata = new SensorCollect.sensordata(System.currentTimeMillis(), mSensorEvent.values, SensorCollect.sensordata.TYPE.ACCE);
            thissensordata.setData(new float[]{
                    mSensorEvent.values[0],
                    mSensorEvent.values[1] ,
                    mSensorEvent.values[2]
            });
        }

        if (mSensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = mSensorEvent.values;
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            orientation = mSensorEvent.values.clone();
        }

        if (thissensordata != null && orientation != null) {
            /*
            new LogCSV(init.rk4_Log+"proAcce", "",
                    new BigDecimal(thissensordata.getTime()).toPlainString(),
                    thissensordata.getData()[0],
                    thissensordata.getData()[1],
                    thissensordata.getData()[2]
            );*/


            Bundle bundle = new Bundle();
            bundle.putFloatArray("Pos", mcalRk4.calc(thissensordata).getData());
            bundle.putFloatArray("Orien", new float[]{orientation[0], orientation[1], orientation[2]});
            bundle.putLong("Time", thissensordata.getTime());
            init.initStabilize.set_Sensor(bundle);

            thissensordata = null;
            orientation = null;
        }
    }

    public void Controller(String type, long timestamp, float[] values) {
        Log.d("SENSOR",type);

        if (Objects.equals(type, " android.sensor.accelerometer"))
            mGravity =values;
        if (Objects.equals(type, "android.sensor.getR")) {
            //rotate
            accmag_orientation =values.clone();

        }


        if (Objects.equals(type, "android.sensor.linear_acceleration")) {
            thissensordata = new SensorCollect.sensordata(System.currentTimeMillis(), values, SensorCollect.sensordata.TYPE.ACCE);

            float[] deltaR = new float[3];
            for(int i =0;i<3;i++){
                deltaR[i] =init_orien[i]-accmag_orientation[i];
            }
            values = rotate(values,new float[]{0,0,0});
            new LogCSV(init.rk4_Log+"calRk4", String.valueOf(getAcceGyro.mstopdetector.getStopped(0)),
                    String.valueOf(System.currentTimeMillis()),
                    values[0],
                    values[1],
                    values[2]);
            thissensordata.setData(new float[]{
                    values[0],
                    values[1],
                    values[2]
            });
        }


        if (thissensordata != null) {


            Bundle bundle = new Bundle();
            bundle.putFloatArray("Pos", mcalRk4.calc(thissensordata).getData());
            bundle.putFloatArray("Orien", new float[]{accmag_orientation[0], accmag_orientation[1], accmag_orientation[2]});

            bundle.putLong("Time", thissensordata.getTime());
            init.initStabilize.set_Sensor(bundle);

            thissensordata = null;
            //orientation = null;
        }
    }

    public float[] rotate(float[] acce,float[] rotation){
        //construct 2d transfomation
        Vector3D mVec = new Vector3D();
        Matrix3D mMatrix = new Matrix3D();
        String Coefficient = "1";
        mMatrix.rotateX(rotation[0]);
        mMatrix.rotateY(rotation[1]);
        mMatrix.rotateZ(rotation[2]);
        double[][] _rotMatrixArray = new double[4][4];
        for (int k = 0; k < 4; k++) {
            for (int j = 0; j < 4; j++) {
                _rotMatrixArray[k][j] = (double) mMatrix.get(k).get(j);
            }
        }
        float[][] _result = new motion_Inertial().toFloatArray(MatMultiply.multiplyByMatrix(_rotMatrixArray, new double[][]{
                {(double) acce[0]},
                {(double) acce[1]},
                {(double) acce[2]},
                {1}
        }));
        Log.d("pro",String.valueOf(_result[0][0]+" "+_result[1][0]+" "+_result[2][0]));
        return new float[]{_result[0][0],_result[1][0],_result[2][0]};
    }
}
