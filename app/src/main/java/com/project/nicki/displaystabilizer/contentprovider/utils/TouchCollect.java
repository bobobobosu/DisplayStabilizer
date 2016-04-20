package com.project.nicki.displaystabilizer.contentprovider.utils;

import android.os.Bundle;
import android.os.Message;
import android.view.MotionEvent;

import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3_1;

import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Created by nickisverygood on 4/20/2016.
 */
public class TouchCollect {

    public void setTouch(MotionEvent event){
        //old

        //Message msgDRAWING = new Message();
        //msgDRAWING.what = 1;
        //msgDRAWING.arg1 = 0;
        Bundle drawposBundleDRAWING = new Bundle();
        drawposBundleDRAWING.putFloatArray("Draw", new float[]{event.getX(),event.getY()});
        drawposBundleDRAWING.putLong("Time",  System.currentTimeMillis());
        //msgDRAWING.setData(drawposBundleDRAWING);
        //stabilize_v3_1.getDraw.sendMessage(msgDRAWING);
        init.initStabilize.gen_Draw(drawposBundleDRAWING);

        //new
        //stabilize_v3.stabilize.mstabilizeSession.setTouchList(new SensorCollect.sensordata(System.currentTimeMillis(), new float[]{event.getX(), event.getY(), 0}, SensorCollect.sensordata.TYPE.TOUCH));

    }
}
