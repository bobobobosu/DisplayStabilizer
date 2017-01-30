package com.project.nicki.displaystabilizer.dataprocessor.MotionEstimation4;

import android.content.Context;
import android.opengl.Matrix;
import android.renderscript.Matrix3f;
import android.renderscript.Matrix4f;
import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.dataprocessor.calRk4;
import com.project.nicki.displaystabilizer.dataprocessor.calRk4_v4;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Filters.filterSensorData;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat.Matrix3D;
import com.project.nicki.displaystabilizer.globalvariable;
import com.project.nicki.displaystabilizer.init;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.ejml.simple.SimpleMatrix;

import java.math.BigDecimal;

/**
 * Created by nicki on 1/26/2017.
 */

public class MotionEstimation4 {
    Context mcontext;
    calRk4_v4 mcalRk4 = new calRk4_v4();


    public MotionEstimation4(Context mContext) {
        mcontext = mContext;
    }
    //public MotionEstimation4(Context mcontext){
    //    this.mcontext = mcontext;
    //}

    public void trigger() {
        //// # raw sensors -> 1!integration -> 2!integration
        mcalRk4.calc(new SensorCollect.sensordata(
                init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getTimestamp(),
                init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues()
        ));
        init.initglobalvariable.mVelocity.add(mcalRk4.prevPosition[0].t, new float[]{
                (float) mcalRk4.prevPosition[0].v,
                (float) mcalRk4.prevPosition[1].v,
                (float) mcalRk4.prevPosition[2].v
        });
        init.initglobalvariable.mPosotion.add(mcalRk4.prevPosition[0].t, new float[]{
                (float) mcalRk4.prevPosition[0].pos,
                (float) mcalRk4.prevPosition[1].pos,
                (float) mcalRk4.prevPosition[2].pos
        });
        //// Propagate back filter results
        for(int i=0;i<3;i++){
            mcalRk4.prevPosition[i].pos = init.initglobalvariable.mPosotion.getLatestData().getValues()[i];
            mcalRk4.prevPosition[i].v = init.initglobalvariable.mVelocity.getLatestData().getValues()[i];
            mcalRk4.prevPosition[i].a = init.initglobalvariable.sAccelerometerLinearVal_world.getLatestData().getValues()[i];
        }


    }


}
