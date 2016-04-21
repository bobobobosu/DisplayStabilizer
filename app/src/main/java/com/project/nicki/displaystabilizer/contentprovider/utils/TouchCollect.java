package com.project.nicki.displaystabilizer.contentprovider.utils;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MotionEvent;

import com.canvas.Stroke;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nickisverygood on 4/20/2016.
 */
public class TouchCollect {
    private Recognize mRecognize = new Recognize();

    private Thread detectState;
    private Handler stabilize_ThreadHandler;
    private HandlerThread stabilize_Thread;

    public states currentState = states.STOP;
    public List<SensorCollect.sensordata> raw_Online = new ArrayList<>();
    public List<List<SensorCollect.sensordata>> raw_Offline = new ArrayList<>();


    public ArrayList<SensorCollect.sensordata> sta_Online_raw = new ArrayList<SensorCollect.sensordata>();
    public List<List<SensorCollect.sensordata>> sta_Online_todraw_stroke = new ArrayList<>();
    public List<List<List<SensorCollect.sensordata>>> sta_Online_todraw_char = new ArrayList<>();

    public List<stabilize_v3.Point> sta_Offline = new ArrayList<>();

    public TouchCollect(){
        detectState = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //Log.e("varrays: ",String.valueOf(currentState));
                        //Log.e("varrays: ",String.valueOf(currentState+" "+sta_Online_raw.size()+" "+sta_Online_todraw_stroke.size()+" "+sta_Online_todraw_char.size()));
                        if(raw_Online != null){
                            if(raw_Online.size()>1){
                                if(System.currentTimeMillis()-raw_Online.get(raw_Online.size()-1).getTime() > 3000 && sta_Online_todraw_stroke.size()!=0) {
                                    currentState = states.STOP;
                                    sta_Online_todraw_char.add(sta_Online_todraw_stroke);
                                    sta_Online_todraw_stroke = new ArrayList<>();
                                    String[] character = mRecognize.recognize_stroke(new ArrayList<>(sta_Online_todraw_char.get(sta_Online_todraw_char.size()-1)));
                                    Log.e("REC",String.valueOf(character[0]));

                                }
                            }
                        }
                        if(sta_Online_todraw_char.size()>3){
                            Log.e("varrays: ",String.valueOf(1));
                        }

                }
            }
        });
        detectState.start();

        stabilize_Thread = new HandlerThread("sensor handler");
        stabilize_Thread.start();
        stabilize_ThreadHandler =new Handler(stabilize_Thread.getLooper());
    }
    public void set_Touch(MotionEvent event){
        raw_Online.add(new SensorCollect.sensordata(System.currentTimeMillis(),new float[]{event.getX(),event.getY(),0}, SensorCollect.sensordata.TYPE.TOUCH));
        ctrl_flow(event);
        gen_Online();
    }

    public void gen_Online(){
        if(raw_Online.size()>0){
            final Bundle drawposBundleDRAWING = new Bundle();
            drawposBundleDRAWING.putFloatArray("Draw", new float[]{raw_Online.get(raw_Online.size()-1).getData()[0],raw_Online.get(raw_Online.size()-1).getData()[1]});
            drawposBundleDRAWING.putLong("Time",  raw_Online.get(raw_Online.size()-1).getTime());

            stabilize_ThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    try{
                        gen_Online_raw(init.initStabilize.gen_Draw(drawposBundleDRAWING));
                    }catch (Exception ex){

                    }
                }
            });
        }
    }

    public void gen_Online_raw(ArrayList<SensorCollect.sensordata> newPt){
        if(currentState == states.HANDSDOWN){
            sta_Online_todraw_stroke.add(new ArrayList<>(newPt));
        }else{
            if(sta_Online_todraw_stroke.size()==0){
                sta_Online_todraw_stroke.add(new ArrayList<>(newPt));
            }
            sta_Online_todraw_stroke.set(sta_Online_todraw_stroke.size()-1,new ArrayList<>(newPt));
        }
        //generate draw_char


        /*
        if(sta_Online_todraw_stroke.size()>2){
            Log.e("varrays: ",String.valueOf(newPt.get(0).getTime() - sta_Online_todraw_stroke.get(sta_Online_todraw_stroke.size()-2).get(sta_Online_todraw_stroke.get(sta_Online_todraw_stroke.size()-2).size()-1).getTime() ));
            if(newPt.get(0).getTime() - sta_Online_todraw_stroke.get(sta_Online_todraw_stroke.size()-2).get(sta_Online_todraw_stroke.get(sta_Online_todraw_stroke.size()-2).size()-1).getTime() > 1000){
                sta_Online_todraw_char.add(new ArrayList<List<SensorCollect.sensordata>>());
                for(int i =0 ; i< sta_Online_todraw_stroke.size()-1 ; i++){
                    sta_Online_todraw_char.get(sta_Online_todraw_char.size()-1).add(sta_Online_todraw_stroke.get(i));
                }
                while (sta_Online_todraw_stroke.size()>1){
                    sta_Online_todraw_stroke.remove(0);
                }
            }
        }*/

        //draw
        List<List<stabilize_v3.Point>> toDrawList = new ArrayList<>();
        for(List<List<SensorCollect.sensordata>> mchar:sta_Online_todraw_char){
            for(List<SensorCollect.sensordata> stroke:mchar){
                toDrawList.add(sensordataList2pntList(stroke));
            }
        }
        for (List<SensorCollect.sensordata> msta_Online_todraw_stroke:sta_Online_todraw_stroke){
            toDrawList.add(sensordataList2pntList(msta_Online_todraw_stroke));
        }

        DemoDraw3.pending_to_draw = toDrawList;
        DemoDraw3.mhandler.sendEmptyMessage(0);
        Log.e("","");
    }

    private List<stabilize_v3.Point> sensordataList2pntList(List<SensorCollect.sensordata> stroke) {
        List<stabilize_v3.Point> retunList = new ArrayList<>();
        for (SensorCollect.sensordata msensordata : stroke){
            retunList.add(new stabilize_v3.Point(msensordata.getData()[0],msensordata.getData()[1]));
        }
        return retunList;
    }

    public void ctrl_flow(MotionEvent event){
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            currentState = states.HANDSDOWN;
        }else if(event.getAction() == MotionEvent.ACTION_MOVE){
            currentState = states.DRAWING;
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            currentState = states.PAUSE;
        } else {
            currentState = states.STOP;
        }
        if(raw_Online.size()>1){
            //Log.e("array: ",String.valueOf(raw_Online.get(raw_Online.size()-1).getTime()-raw_Online.get(raw_Online.size()-2).getTime()));
            if(raw_Online.get(raw_Online.size()-1).getTime()-raw_Online.get(raw_Online.size()-2).getTime() > 1000) {
                currentState = states.STOP;
            }
        }
        Log.e("array: ",String.valueOf(currentState));
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
