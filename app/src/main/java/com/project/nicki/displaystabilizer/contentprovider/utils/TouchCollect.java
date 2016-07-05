package com.project.nicki.displaystabilizer.contentprovider.utils;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;

import com.canvas.Stroke;
import com.project.nicki.displaystabilizer.UI.UIv1.UIv1_draw0;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3;

import org.ejml.simple.SimpleMatrix;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by nickisverygood on 4/20/2016.
 */
public class TouchCollect {
    ////Threads
    private Thread detectState;
    private Handler split_ThreadHandler;
    private HandlerThread split_Thread;
    ////Buffers
    public List<SensorCollect.sensordata> raw_Online = new ArrayList<>();
    public List<List<SensorCollect.sensordata>> raw_Offline = new ArrayList<>();
    public ArrayList<SensorCollect.sensordata> sta_Online_raw = new ArrayList<SensorCollect.sensordata>();
    public List<List<SensorCollect.sensordata>> sta_Online_todraw_stroke = new ArrayList<>();
    public List<List<List<SensorCollect.sensordata>>> sta_Online_todraw_char = new ArrayList<>();
    public List<List<SensorCollect.sensordata>> ori_Online_todraw_stroke = new ArrayList<>();
    public List<List<List<SensorCollect.sensordata>>> ori_Online_todraw_char = new ArrayList<>();
    public List<StabilizeResult> recognized_result = new ArrayList<>();
    ////States
    public states currentState = states.STOP;


    ////Initialization
    public TouchCollect() {
        detectState = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(20);
                        if (raw_Online != null) {
                            if (raw_Online.size() > 1) {
                                if (System.currentTimeMillis() - raw_Online.get(raw_Online.size() - 1).getTime() > 1000) {
                                    currentState = states.STOP;
                                }

                                if (currentState == states.STOP && sta_Online_todraw_stroke.size() > 0 && ori_Online_todraw_stroke.size() > 0) {
                                    //Log.e("THREAD", String.valueOf(currentState + " " + sta_Online_todraw_stroke.size() + " " + ori_Online_todraw_stroke.size()));
                                    recognized_and_save();
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
        detectState.start();

        split_Thread = new HandlerThread("sensor handler");
        split_Thread.start();
        split_ThreadHandler = new Handler(split_Thread.getLooper());
    }

    public void recognized_and_save() {
        //split to char sta
        sta_Online_todraw_char.add(sta_Online_todraw_stroke);
        //split to char ori
        ori_Online_todraw_char.add(ori_Online_todraw_stroke);
        sta_Online_todraw_stroke = new ArrayList<>();
        ori_Online_todraw_stroke = new ArrayList<>();

        try {
            DemoDraw3.recognized_data sta_result = DemoDraw3.recognize_stroke(new ArrayList<>(sta_Online_todraw_char.get(sta_Online_todraw_char.size() - 1)));
            DemoDraw3.recognized_data ori_result = DemoDraw3.recognize_stroke(new ArrayList<>(ori_Online_todraw_char.get(ori_Online_todraw_char.size() - 1)));
            Log.e("REC", String.valueOf(
                    "STA: " + sta_result.getCharIndex(0) + " " + sta_result.getConfidenceIndex(0) +
                            " ORI: " + ori_result.getCharIndex(0) + " " + ori_result.getConfidenceIndex(0)) +
                    " " + (sta_result.getConfidenceIndex(0) > ori_result.getConfidenceIndex(0)));
            recognized_result.add(new StabilizeResult(
                    new ArrayList<>(ori_Online_todraw_char.get(ori_Online_todraw_char.size() - 1)),
                    new ArrayList<>(sta_Online_todraw_char.get(sta_Online_todraw_char.size() - 1)),
                    ori_result,
                    sta_result
            ));

            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("ORI_CHAR", ori_result.getCharIndex(0));
            bundle.putString("STA_CHAR", sta_result.getCharIndex(0));
            bundle.putFloat("ORI_CONF", ori_result.getConfidenceIndex(0));
            bundle.putFloat("STA_CONF", sta_result.getConfidenceIndex(0));
            msg.setData(bundle);
            UIv1_draw0.update_results.sendMessage(msg);
        } catch (Exception ex) {
            Log.e("REC", String.valueOf(ex));
        }
    }

    public void set_Touch(MotionEvent event) {
        raw_Online.add(new SensorCollect.sensordata(System.currentTimeMillis(), new float[]{event.getX(), event.getY(), 0}, SensorCollect.sensordata.TYPE.TOUCH));
        ctrl_flow(event);
        gen_Online(event);
    }

    public void save_and_clean() {
        try {
            recognized_and_save();
        } catch (Exception ex) {
            //Log.e("save_and_clean",String.valueOf(ex));
        }

        raw_Online = new ArrayList<>();
        raw_Offline = new ArrayList<>();
        sta_Online_raw = new ArrayList<SensorCollect.sensordata>();
        sta_Online_todraw_stroke = new ArrayList<>();
        sta_Online_todraw_char = new ArrayList<>();
        ori_Online_todraw_stroke = new ArrayList<>();
        ori_Online_todraw_char = new ArrayList<>();
        DemoDraw3.sta_pending_to_draw = new ArrayList<>();
        DemoDraw3.clean_and_refresh.sendEmptyMessage(0);
    }

    public void gen_Online(final MotionEvent event) {
        if (raw_Online.size() > 0) {
            final Bundle drawposBundleDRAWING = new Bundle();
            drawposBundleDRAWING.putFloatArray("Draw", new float[]{raw_Online.get(raw_Online.size() - 1).getData()[0], raw_Online.get(raw_Online.size() - 1).getData()[1]});
            drawposBundleDRAWING.putLong("Time", raw_Online.get(raw_Online.size() - 1).getTime());

            split_ThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                    } catch (Exception ex) {
                        //Log.e("EX",String.valueOf(ex));
                    }
                    try {
                        gen_Online_ori(event);
                    } catch (Exception ex) {
                        //Log.e("EX1",String.valueOf(ex));
                    }
                    try {
                        gen_Online_sta(event, init.initStabilize.gen_Draw(drawposBundleDRAWING));
                    } catch (Exception ex) {
                        //  Log.e("EX2",String.valueOf(ex));
                    }

                }
            });
        }
    }

    public void gen_Online_ori(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            ori_Online_todraw_stroke.add(new ArrayList<SensorCollect.sensordata>());
            ori_Online_todraw_stroke.get(ori_Online_todraw_stroke.size() - 1).add(raw_Online.get(raw_Online.size() - 1));
        } else {
            if (ori_Online_todraw_stroke.size() == 0) {
                ori_Online_todraw_stroke.add(new ArrayList<SensorCollect.sensordata>());
            }
            ori_Online_todraw_stroke.get(ori_Online_todraw_stroke.size() - 1).add(raw_Online.get(raw_Online.size() - 1));
        }
    }

    public void gen_Online_sta(MotionEvent event, ArrayList<SensorCollect.sensordata> newPt) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            sta_Online_todraw_stroke.add(new ArrayList<SensorCollect.sensordata>());
        } else {
            if (newPt != null) {
                if (sta_Online_todraw_stroke.size() == 0) {
                    sta_Online_todraw_stroke.add(new ArrayList<SensorCollect.sensordata>());
                }
                sta_Online_todraw_stroke.set(sta_Online_todraw_stroke.size() - 1, (List<SensorCollect.sensordata>) newPt.clone());
            }
        }

        upadteDraw();
    }

    public void upadteDraw() {
        //draw sta
        List<List<stabilize_v3.Point>> sta_toDrawList = new ArrayList<>();
        for (List<List<SensorCollect.sensordata>> mchar : sta_Online_todraw_char) {
            for (List<SensorCollect.sensordata> stroke : mchar) {
                sta_toDrawList.add(sensordataList2pntList(stroke));
            }
        }
        for (List<SensorCollect.sensordata> msta_Online_todraw_stroke : sta_Online_todraw_stroke) {
            sta_toDrawList.add(sensordataList2pntList(msta_Online_todraw_stroke));
        }

        //draw ori
        List<List<stabilize_v3.Point>> ori_toDrawList = new ArrayList<>();
        for (List<List<SensorCollect.sensordata>> mchar : ori_Online_todraw_char) {
            for (List<SensorCollect.sensordata> stroke : mchar) {
                ori_toDrawList.add(sensordataList2pntList(stroke));
            }
        }
        for (List<SensorCollect.sensordata> mori_Online_todraw_stroke : ori_Online_todraw_stroke) {
            ori_toDrawList.add(sensordataList2pntList(mori_Online_todraw_stroke));
        }


        DemoDraw3.sta_pending_to_draw = sta_toDrawList;
        DemoDraw3.ori_pending_to_draw = ori_toDrawList;
        DemoDraw3.refresh.sendEmptyMessage(0);
    }

    public List<stabilize_v3.Point> sensordataList2pntList(List<SensorCollect.sensordata> stroke) {
        List<stabilize_v3.Point> retunList = new ArrayList<>();
        for (SensorCollect.sensordata msensordata : stroke) {
            retunList.add(new stabilize_v3.Point(msensordata.getData()[0], msensordata.getData()[1]));
        }
        return retunList;
    }

    public void ctrl_flow(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            currentState = states.HANDSDOWN;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            currentState = states.DRAWING;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            currentState = states.PAUSE;
        } else {
            currentState = states.STOP;
        }
        //Log.e("array: ", String.valueOf(currentState));
        gen_raw_Offline();
    }

    public void gen_raw_Offline() {
        if (raw_Online.size() > 2) {
            long last1_time = raw_Online.get(raw_Online.size() - 1).getTime();
            long last2_time = raw_Online.get(raw_Online.size() - 2).getTime();
            if (last1_time - last2_time > 2000) {
                raw_Offline.add(raw_Online);
                raw_Online = new ArrayList<>();
            }
        }
    }

    private enum states {
        STOP,
        HANDSDOWN,
        DRAWING,
        PAUSE,
        HANDSUP
    }

    public class StabilizeResult {
        public List<List<SensorCollect.sensordata>> sta_Online_todraw_char;
        public List<List<SensorCollect.sensordata>> ori_Online_todraw_char;
        public DemoDraw3.recognized_data sta_result;
        public DemoDraw3.recognized_data ori_result;
        public String Time = getDateTime();

        public StabilizeResult(
                List<List<SensorCollect.sensordata>> ori_Online_todraw_char,
                List<List<SensorCollect.sensordata>> sta_Online_todraw_char,
                DemoDraw3.recognized_data ori_result,
                DemoDraw3.recognized_data sta_result
        ) {
            this.ori_Online_todraw_char = ori_Online_todraw_char;
            this.sta_Online_todraw_char = sta_Online_todraw_char;
            this.ori_result = ori_result;
            this.sta_result = sta_result;
        }
    }

    public String getDateTime() {
        SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
        Date date = new Date();
        String strDate = sdFormat.format(date);
        return strDate;
    }
}
