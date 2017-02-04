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
import android.os.Bundle;
import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.globalvariable;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MotionEstimation3 {
    ////Variables
    Context mContext;
    calRk4 mcalRk4 = new calRk4();
    globalvariable.CircularBuffer2 rawMot = new globalvariable.CircularBuffer2(10);
    BigDecimal CoeffX = new BigDecimal(0);
    BigDecimal CoeffY = new BigDecimal(0);
    Thread draw = new Thread(new Runnable() {
        @Override
        public void run() {

        }
    });

    public MotionEstimation3(Context context) {
        mContext = context;
    }

    public void Controller() {
        SensorCollect.sensordata currSensordata = new SensorCollect.sensordata(System.currentTimeMillis(), init.initglobalvariable.AccelerometerLinearVal, SensorCollect.sensordata.TYPE.ACCE);
        Bundle bundle = new Bundle();
        float[] pos = mcalRk4.calc(currSensordata).getData();
        bundle.putFloatArray("Pos", pos);
        bundle.putFloatArray("Orien", new float[]{init.initglobalvariable.OrientationVal[0], init.initglobalvariable.OrientationVal[1], init.initglobalvariable.OrientationVal[2]});
        bundle.putDoubleArray("Quaternion", init.initglobalvariable.QuaternionVal);
        bundle.putLong("Time", currSensordata.getTime());

        //drawMotionPath(pos);
        init.initStabilize_v4.set_Sensor(bundle);
    }

    public void drawMotionPath(float[] pos) {
        if (DemoDraw3.motion_path.size() == 0) {
            DemoDraw3.motion_path.add(new ArrayList<stabilize_v3.Point>());
        }

        /*
        if (pos[0] * pos[1] != 0 || true) {

        }
        */
        rawMot.add(pos);


        if (!draw.isAlive()) {

            draw = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        scale();
                    } catch (Exception ex) {
                        //Log.e("mot", String.valueOf(ex));
                    }
                }
            });
            draw.start();


        }


    }

    public void scale() {

        // x array
        List<Float> xpos = new ArrayList<>();
        List<Float> ypos = new ArrayList<>();
        for (float[] rawMotf : rawMot.data) {
            if(rawMotf[0]*rawMotf[1] !=0){
            xpos.add(rawMotf[0]);
            ypos.add(rawMotf[1]);
            }

        }

        BigDecimal MinNegativeX = new BigDecimal(Collections.min(xpos));
        BigDecimal MinNegativeY = new BigDecimal(Collections.min(ypos));
        BigDecimal MaxPositiveX = new BigDecimal(Collections.max(xpos));
        BigDecimal MaxPositiveY = new BigDecimal(Collections.max(ypos));

        BigDecimal MaxX = MinNegativeX.subtract(MaxPositiveX).abs();
        BigDecimal MaxY = MinNegativeY.subtract(MaxPositiveY).abs();


        //float MaxX = Math.max(Math.abs(MinNegativeX), Math.abs(MaxPositiveX));
        //float MaxY = Math.max(Math.abs(MinNegativeY), Math.abs(MaxPositiveY));


        try {

            BigDecimal tCoeffX = new BigDecimal(2560).divide(MaxX, 10, BigDecimal.ROUND_HALF_EVEN);
            BigDecimal tCoeffY = new BigDecimal(1440).divide(MaxY, 10, BigDecimal.ROUND_HALF_EVEN);


            CoeffX = tCoeffX;
            CoeffY = tCoeffY;
        } catch (Exception ex) {

        }

        //BigDecimal CoeffY = 1080/ MaxY;
        //BigDecimal Coeff = Math.max(CoeffX, CoeffY);


        DemoDraw3.motion_path.get(0).clear();
        for (int i = 0; i < rawMot.data.size(); i++) {


            //DemoDraw3.motion_path.get(0).get(i).x = new BigDecimal(xpos.get(i)).multiply(CoeffX).floatValue();
            //DemoDraw3.motion_path.get(0).get(i).y = new BigDecimal(ypos.get(i)).multiply(CoeffY).floatValue() ;

            //DemoDraw3.motion_path.get(0).get(i).x = new BigDecimal(xpos.get(i)).multiply(BigDecimal.valueOf(10000000)).floatValue();
            //DemoDraw3.motion_path.get(0).get(i).y = new BigDecimal(ypos.get(i)).multiply(BigDecimal.valueOf(10000000)).floatValue();


            float newX = new BigDecimal(xpos.get(i)).multiply(CoeffX).floatValue();
            float newY = new BigDecimal(ypos.get(i)).multiply(CoeffY).floatValue();

            DemoDraw3.motion_path.get(0).add(new stabilize_v3.Point(newX + 640, newY + 850));

            //Log.d("mot", String.valueOf(DemoDraw3.motion_path.get(0).size()) + "  " + CoeffX.floatValue() + "   " + CoeffY.floatValue());

            //Log.d("mot", String.valueOf(DemoDraw3.motion_path.get(0).size()) + "  " + CoeffX.floatValue() + "   " + CoeffY.floatValue());
            DemoDraw3.refresh.sendEmptyMessage(0);


        }


    }


}
