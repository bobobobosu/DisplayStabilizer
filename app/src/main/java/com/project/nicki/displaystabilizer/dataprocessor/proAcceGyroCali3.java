package com.project.nicki.displaystabilizer.dataprocessor;

/**
 * Created by nickisverygood on 3/9/2016.
 */

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw2;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LevenbergMarquardt;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.dataprocessor.utils.MatMultiply;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat.Matrix3D;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat.Vector3D;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3_1;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3_1;

import org.ejml.data.DenseMatrix64F;

import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jama.Matrix;
import jkalman.JKalman;

import static com.project.nicki.displaystabilizer.stabilization.stabilize_v3.double2String;
import static com.project.nicki.displaystabilizer.stabilization.stabilize_v3.multiply;

public class proAcceGyroCali3 {
    Context mContext;
    calRk4 mcalRk4 = new calRk4();
    float[] mGravity = new float[3];
    SensorCollect.sensordata thissensordata = null;
    float orientation[] = null;

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
}
