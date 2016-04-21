package com.project.nicki.displaystabilizer.contentprovider.utils;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3_1;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nickisverygood on 4/20/2016.
 */
public class TouchCollect {
    public states currentState = states.STOP;
    public List<SensorCollect.sensordata> raw_Online = new ArrayList<>();
    public List<List<SensorCollect.sensordata>> raw_Offline = new ArrayList<>();


    public ArrayList<SensorCollect.sensordata> sta_Online_raw = new ArrayList<SensorCollect.sensordata>();
    public List<List<stabilize_v3.Point>> sta_Online_todraw = new ArrayList<>();

    public List<stabilize_v3.Point> sta_Offline = new ArrayList<>();
    public void set_Touch(MotionEvent event){
        raw_Online.add(new SensorCollect.sensordata(System.currentTimeMillis(),new float[]{event.getX(),event.getY(),0}, SensorCollect.sensordata.TYPE.TOUCH));
        ctrl_flow(event);
        gen_Online();
    }

    public void gen_Online(){
        if(raw_Online.size()>0){
            Bundle drawposBundleDRAWING = new Bundle();
            drawposBundleDRAWING.putFloatArray("Draw", new float[]{raw_Online.get(raw_Online.size()-1).getData()[0],raw_Online.get(raw_Online.size()-1).getData()[1]});
            drawposBundleDRAWING.putLong("Time",  raw_Online.get(raw_Online.size()-1).getTime());

            gen_Online_todraw(init.initStabilize.gen_Draw(drawposBundleDRAWING));


        }
    }

    public void gen_Online_todraw(List<stabilize_v3.Point> sta_List){

        Log.e("currentState",String.valueOf(currentState));
        try {
            if(currentState == states.HANDSDOWN){
                sta_Online_todraw.add(sta_List);
            }
            else if(currentState ==(states.DRAWING) ){
                sta_Online_todraw.set(sta_Online_todraw.size()-1,sta_List);
            }
            else if(currentState == states.PAUSE){
                sta_Online_todraw.add(sta_List);
            }else {
                sta_Online_todraw.add(sta_List);
            }
        }catch (Exception ex){
            Log.e("gen_Online_todraw",String.valueOf(ex));
        }



        //DemoDraw3.pending_to_draw = new ArrayList<>();
        DemoDraw3.pending_to_draw = sta_Online_todraw;


    }


    public void ctrl_flow(MotionEvent event){
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            currentState = states.HANDSDOWN;
        }else if(event.getAction() == MotionEvent.ACTION_MOVE){
            currentState = states.DRAWING;
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            currentState = states.PAUSE;
        }else {
            currentState = states.STOP;
        }
        gen_raw_Offline();
    }
    public void gen_raw_Offline(){
        if (raw_Online.size() > 2){
            long last1_time = raw_Online.get(raw_Online.size()-1).getTime();
            long last2_time = raw_Online.get(raw_Online.size()-2).getTime();
            if(last1_time-last2_time > 2000){
                raw_Offline.add(raw_Online);
                raw_Online = new ArrayList<>();
            }
        }
    }
    private enum states{
        STOP,
        HANDSDOWN,
        DRAWING,
        PAUSE,
        HANDSUP
    }
}
