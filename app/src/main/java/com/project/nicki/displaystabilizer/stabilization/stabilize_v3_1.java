package com.project.nicki.displaystabilizer.stabilization;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.canvas.Stroke;
import com.project.nicki.displaystabilizer.UI.DemoDrawUI;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect.sensordata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by nickisverygood on 1/3/2016.
 */
public class stabilize_v3_1 {
    public static Handler getDraw;

    //constants
    private static float cX;
    private static float cY;
    //mods
    public boolean CalibrationMode = true;
    public ArrayList<sensordata> stroke2pos;

    //buffers

    
    ArrayList<sensordata> strokebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> strokedeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posbuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posdeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> stastrokebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> stastrokedeltabuffer = new ArrayList<sensordata>();
    //tmps
    long prevTime = 0;
    float[] prevStroke = null;
    boolean drawSTATUS = false;
    boolean prevdrawSTATUS = false;
    boolean init_yesno = false;
    sensordata tmpaccesensordata;
    sensordata tmporiensensordata;
    float[] Pos = null;
    int deltaingStatus = 0;
    sensordata tmp1accesensordata;
    //init_yesno specific
    float[] orieninit;


    public stabilize_v3_1() {
    }


    public void set_Sensor(final Bundle bundlegot) {
        try {
            Pos = bundlegot.getFloatArray("Pos");

            tmpaccesensordata = new sensordata(bundlegot.getLong("Time"), bundlegot.getFloatArray("Pos"));
            tmporiensensordata = new sensordata(bundlegot.getLong("Time"), bundlegot.getFloatArray("Orien"));
            if (DemoDraw3.orienreset == false) {
                orieninit = tmporiensensordata.getData();
                DemoDraw3.orienreset = true;
            }

            prevdrawSTATUS = drawSTATUS;
            drawSTATUS = DemoDraw3.drawing < 2;

            if (deltaingStatus == 0) {
                tmp1accesensordata = tmpaccesensordata;
                tmpaccesensordata = new sensordata(tmp1accesensordata.getTime(), new float[]{0, 0});
                deltaingStatus = 1;
            } else if (deltaingStatus == 1) {
                tmpaccesensordata = new sensordata(tmp1accesensordata.getTime(), new float[]{
                        tmpaccesensordata.getData()[0] - tmp1accesensordata.getData()[1],
                        tmpaccesensordata.getData()[1] - tmp1accesensordata.getData()[1]});
            }

            if (prevdrawSTATUS == false && drawSTATUS == true || init_yesno == false) {
                orieninit = tmporiensensordata.getData();
                
                if (CalibrationMode == true && strokebuffer.size() > 1 && posbuffer.size() > 1) {
                    CalibrationMode = false;
                }
                strokebuffer = new ArrayList<>();
                strokedeltabuffer = new ArrayList<>();
                posbuffer = new ArrayList<>();
                posdeltabuffer = new ArrayList<>();
                stastrokebuffer = new ArrayList<>();
                stastrokedeltabuffer = new ArrayList<>();
                prevTime = 0;
                prevStroke = null;

                init_yesno = true;
                tmpaccesensordata = null;
                deltaingStatus = 0;
            }
        }catch (Exception ex){
            Log.e("set_Sensor",String.valueOf(ex));
        }


    }

    public ArrayList<sensordata> gen_Draw(final Bundle bundlegot) {
        try {
            if (DemoDraw.drawing == 0) {
                orieninit = tmporiensensordata.getData();
            }
            if (tmpaccesensordata == null || Pos == null) {
                return null;
            }
            prevdrawSTATUS = drawSTATUS;
            drawSTATUS = DemoDraw3.drawing < 2;

            if (prevdrawSTATUS == false && drawSTATUS == true || init_yesno == false) {


                if (CalibrationMode == true && strokebuffer.size() > 1 && posbuffer.size() > 1) {
                    CalibrationMode = false;
                }
                strokebuffer = new ArrayList<>();
                strokedeltabuffer = new ArrayList<>();
                posbuffer = new ArrayList<>();
                posdeltabuffer = new ArrayList<>();
                stastrokebuffer = new ArrayList<>();
                stastrokedeltabuffer = new ArrayList<>();
                prevTime = 0;
                prevStroke = null;

                init_yesno = true;
                tmpaccesensordata = null;


            }


            //manual control
            cX = -10f;
            cY = 10f;


            //buffer Draw
            strokebuffer.add(bundle2sensordata(bundlegot));
            if (strokebuffer.size() > 1) {
                strokedeltabuffer.add(getlatestdelta(strokebuffer));
            }



            if (tmpaccesensordata != null) {
                posdeltabuffer.add(tmpaccesensordata);
                tmpaccesensordata = null;
            }

            //get stabilized result
            if (strokedeltabuffer.size() > 0 && posdeltabuffer.size() > 0) {
                //generate stabilize vector
                sensordata stastrokedelta = new sensordata();
                stastrokedelta.setTime(strokedeltabuffer.get(strokedeltabuffer.size() - 1).getTime());
                stastrokedelta.setData(new float[]{
                        strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[0] - posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0] * cX / (float) com.project.nicki.displaystabilizer.init.pix2m,
                        strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[1] - posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[1] * cY / (float) com.project.nicki.displaystabilizer.init.pix2m});
                stastrokedeltabuffer.add(stastrokedelta);

                if (prevStroke != null) {
                    //generate stabilized point
                    prevStroke[0] += stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[0];
                    prevStroke[1] += stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[1];
                    stastrokebuffer.add(new sensordata(stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getTime(), prevStroke));
                    init.initTouchCollection.sta_Online_raw.add(new sensordata(stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getTime(), prevStroke));

                    //stick to finger_array
                    float tofinger[] = new float[]{strokebuffer.get(strokebuffer.size() - 1).getData()[0] - prevStroke[0],
                            strokebuffer.get(strokebuffer.size() - 1).getData()[1] - prevStroke[1]};
                    ArrayList<sensordata> r_stastrokebuffer = new ArrayList<>();

                    //init.initTouchCollection.sta_Online_todraw_stroke = new ArrayList<>();


                    List<stabilize_v3.Point> tofingerList = new ArrayList<>();
                    for (int i = 0; i < stastrokebuffer.size(); i++) {
                        sensordata tmp = new sensordata(strokebuffer.get(i));
                        tmp.setData(new float[]{
                                stastrokebuffer.get(i).getData()[0] + tofinger[0],
                                stastrokebuffer.get(i).getData()[1] + tofinger[1]
                        });
                        tmp.setTime(stastrokebuffer.get(i).getTime());

                        stabilize_v3.Point todraw = new stabilize_v3.Point();
                        todraw.setX(stastrokebuffer.get(i).getData()[0] + tofinger[0]);
                        todraw.setY(stastrokebuffer.get(i).getData()[1] + tofinger[1]);
                        tofingerList.add(todraw);

                        r_stastrokebuffer.add(tmp);

                    }

                   // DemoDraw3.pending_to_draw_direct = tofingerList;

                    return r_stastrokebuffer;



                } else {
                    prevStroke = new float[]{
                            strokebuffer.get(0).getData()[0],
                            strokebuffer.get(0).getData()[1]};
                    prevStroke = new float[]{0, 0};
                }


            }

        }catch (Exception ex){
            Log.e("gen_Draw",String.valueOf(ex));
        }

        return null;
    }


    private sensordata bundle2sensordata(Bundle bundle) {
        sensordata returesensordata = new sensordata();
        for (String keys : bundle.keySet()) {
            if (keys == "Time") {
                returesensordata.setTime(bundle.getLong(keys));
            } else {
                returesensordata.setData(bundle.getFloatArray(keys));
            }
        }
        return returesensordata;
    }

    private sensordata getlatestdelta(ArrayList<sensordata> strokebuffer) {
        sensordata msensordata = new sensordata();
        //compute delta
        float[] deltaFloat = new float[strokebuffer.get(0).getData().length];
        for (int i = 0; i < strokebuffer.get(0).getData().length; i++) {
            deltaFloat[i] = (strokebuffer.get(strokebuffer.size() - 1).getData()[i] - strokebuffer.get(strokebuffer.size() - 2).getData()[i]);
        }
        msensordata.setTime(strokebuffer.get(strokebuffer.size() - 1).getTime());
        msensordata.setData(deltaFloat);
        return msensordata;
    }



}
