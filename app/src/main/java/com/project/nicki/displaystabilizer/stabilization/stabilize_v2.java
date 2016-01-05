package com.project.nicki.displaystabilizer.stabilization;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;
import com.project.nicki.displaystabilizer.dataprocessor.proAcceGyroCali;

import org.opencv.core.Mat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by nickisverygood on 1/3/2016.
 */
public class stabilize_v2 implements Runnable {
    public static Handler getDatas;
    private String csvName = "stabilize_v2.csv";
    private String TAG = "stabilize_v2";
    private Context mContext;
    FileWriter mFileWriter;
    //buffers
    ArrayList<sensordata> strokebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> strokedeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> accebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posbuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posdeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> stastrokedeltabuffer = new ArrayList<sensordata>();
    //tmps
    long prevTime = 0;
    float[] prevStroke ;
    boolean drawSTATUS =false;
    boolean prevdrawSTATUS=false;
    public stabilize_v2(Context context) {
        mContext = context;
    }
    //constants
    float toDrawScalar = 3;
    public class sensordata {
        private long Time;
        private float[] Data = new float[3];

        public sensordata() {
            this(0, new float[]{0, 0, 0});
        }

        public sensordata(long time, float[] data) {
            this.Time = time;
            for (int i = 0; i < data.length; i++) {
                this.Data[i] = data[i];
            }
        }

        public void setsensordata(long time, float[] data) {
            this.Time = time;
            for (int i = 0; i < data.length; i++) {
                this.Data[i] = data[i];
            }
        }

        public void setTime(long time) {
            this.Time = time;
        }

        public void setData(float[] data) {
            for (int i = 0; i < data.length; i++) {
                this.Data[i] = data[i];
            }
        }

        public long getTime() {
            return Time;
        }

        public float[] getData() {
            return Data;
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        getDatas = new Handler() {
            calEularIntegration meularIntegration = new calEularIntegration();
            Position initPosX = new Position(0, 0);
            Position initPosY = new Position(0, 0);
            RK4 mrk4 = new RK4();
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle bundlegot = msg.getData();
                prevdrawSTATUS = drawSTATUS;
                if(DemoDraw.drawing<2){
                    drawSTATUS = true;
                }else{
                    drawSTATUS = false;
                }
                if(prevdrawSTATUS == false && drawSTATUS == true){
                    strokebuffer = new ArrayList<sensordata>();
                    strokedeltabuffer = new ArrayList<sensordata>();
                    accebuffer = new ArrayList<sensordata>();
                    posbuffer = new ArrayList<sensordata>();
                    posdeltabuffer = new ArrayList<sensordata>();
                    stastrokedeltabuffer = new ArrayList<sensordata>();
                    meularIntegration = new calEularIntegration();
                    initPosX = new Position(0, 0);
                    initPosY = new Position(0, 0);
                    mrk4 = new RK4();
                    prevTime = 0;
                    prevStroke =new float[2];
                    drawSTATUS =false;
                    prevdrawSTATUS=false;
                }else {


                    //draw
                    if (msg.arg1 == 0) {
                        Log.d(TAG, "delta2: " + bundlegot.getFloatArray("Draw")[0] + " " + drawSTATUS);
                        strokebuffer.add(new sensordata(bundlegot.getLong("Time"), bundlegot.getFloatArray("Draw")));
                        if (strokebuffer.size() > 1) {
                            strokedeltabuffer.add(getlatestdelta(strokebuffer));
                            Log.d(TAG, "delta: " + getlatestdelta(strokebuffer).getData()[0]);
                        }
                    }
                    //acce
                    if (msg.arg1 == 2) {
                        //rk4
                        mrk4.integrate(initPosX, 0, 0.01, bundlegot.getFloatArray("Acce")[0]);
                        mrk4.integrate(initPosY, 0, 0.01, bundlegot.getFloatArray("Acce")[1]);
                        meularIntegration.addNew(bundlegot.getFloatArray("Acce"), bundlegot.getLong("Time"));

                        LogCSV(String.valueOf(initPosX.pos),
                                String.valueOf(initPosY.pos),
                                String.valueOf(meularIntegration.getPos()[0]),
                                String.valueOf(meularIntegration.getPos()[1]), "", "");

                        meularIntegration.addNew(bundlegot.getFloatArray("Acce"), bundlegot.getLong("Time"));
                        posbuffer.add(meularIntegration.getData());
                        if (posbuffer.size() > 1) {
                            posdeltabuffer.add(getlatestdelta(posbuffer));
                            if (posdeltabuffer.size() > 0) {

                            /*
                            LogCSV(String.valueOf(posdeltabuffer.get(posdeltabuffer.size()-1).getData()[0])
                                    ,String.valueOf(posdeltabuffer.get(posdeltabuffer.size()-1).getData()[1])
                                    ,String.valueOf(posdeltabuffer.get(posdeltabuffer.size()-1).getData()[2])
                                    ,String.valueOf(posbuffer.get(posbuffer.size()-1).getData()[0])
                                    ,String.valueOf(posbuffer.get(posbuffer.size()-1).getData()[1])
                                    ,String.valueOf(posbuffer.get(posbuffer.size()-1).getData()[2]));
                                    */
                            }
                        }
                        Log.d(TAG, "POSITION: " + String.valueOf(meularIntegration.getPos()[0]) + " " + String.valueOf(initPosX.pos));
                        if (drawSTATUS == true) {
                            accebuffer.add(new sensordata(bundlegot.getLong("Time"), bundlegot.getFloatArray("Acce")));
                        }
                    }


                    Log.d(TAG,"stop");
                    //stabilize
                    if(strokedeltabuffer.size()>1 &&  posdeltabuffer.size()>1 && posbuffer.size()>1){
                        sensordata stastrokedelta= new sensordata();
                        stastrokedelta.setTime(strokedeltabuffer.get(strokedeltabuffer.size() - 1).getTime());
                        stastrokedelta.setData(new float[]{
                                posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0] + strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[0],
                                posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[1] + strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[1]});
                        Log.d(TAG, "stab: " +posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0]+" "+posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[1]);
                        stastrokedeltabuffer.add(stastrokedelta);
                        //draw results
                        DemoDraw.paint2.setColor(Color.BLUE);
                        if(prevStroke != null){
                            DemoDraw.path2.moveTo(prevStroke[0], prevStroke[1]);
                            DemoDraw.path2.lineTo(
                                    prevStroke[0]+stastrokedeltabuffer.get(stastrokedeltabuffer.size()-1).getData()[0],
                                    prevStroke[1]+stastrokedeltabuffer.get(stastrokedeltabuffer.size()-1).getData()[1]);
                            prevStroke[0] += stastrokedeltabuffer.get(stastrokedeltabuffer.size()-1).getData()[0];
                            prevStroke[1] += stastrokedeltabuffer.get(stastrokedeltabuffer.size()-1).getData()[1];
                        }else {
                            prevStroke = new float[]{
                                    strokebuffer.get(2).getData()[0],
                                    strokebuffer.get(2).getData()[1]};
                        }
                        Log.d(TAG, "drawpos: " + prevStroke[0]+" "+prevStroke[1]);
                    /*
                    for(int k=0 ; k<stastrokedeltabuffer.size(); k++){
                        for(int m=0 ; m<startDraw.length;m++){
                            Log.d(TAG,"stastrokedeltabuffer "+stastrokedeltabuffer.get(k).getData()[m]);
                            startDraw[m] += stastrokedeltabuffer.get(k).getData()[m];
                        }
                        DemoDraw.path2.moveTo(startDraw[0],startDraw[1]);
                        DemoDraw.path2.lineTo(
                                startDraw[0]+stastrokedeltabuffer.get(k).getData()[0],
                                startDraw[1]+stastrokedeltabuffer.get(k).getData()[1]);
                    }
                    */
                    }


                    Message msg3 = new Message();
                    msg3.what = 1;
                    DemoDraw.mhandler.sendMessage(msg3);
                }

            }


        };
        Looper.loop();
    }

    private sensordata getlatestdelta(ArrayList<sensordata> strokebuffer) {
        sensordata msensordata = new sensordata();
        //compute delta
        float[] deltaFloat = new float[strokebuffer.get(0).getData().length];
        for (int i = 0; i < strokebuffer.get(0).getData().length; i++) {
            deltaFloat[i] = (strokebuffer.get(strokebuffer.size() - 1).getData()[i] - strokebuffer.get(strokebuffer.size() - 2).getData()[i])*toDrawScalar;
        }
        msensordata.setTime(strokebuffer.get(strokebuffer.size() - 1).getTime());
        msensordata.setData(deltaFloat);
        return msensordata;
    }

    public void LogCSV(String a, String b, String c, String d, String g, String h) {
        //init CSV logging
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = csvName;
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer = null;
        // File exist
        if (f.exists() && !f.isDirectory()) {
            try {
                mFileWriter = new FileWriter(filePath, true);
            } catch (IOException e) {
                //e.printStackTrace();
            }
            writer = new CSVWriter(mFileWriter);
        } else {
            try {
                writer = new CSVWriter(new FileWriter(filePath));
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }

        try {
            String line = String.format("%s,%s,%s,%s,%s,%s\n", a, b, c, d, g, h);
            mFileWriter.write(line);
        } catch (IOException e) {
            //e.printStackTrace();
        }

        try {
            writer.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    //Eular integration
    public class calEularIntegration {
        boolean init = true;
        private long Tprev = 0;
        private float[] acce;
        private float[] velo;
        private float[] pos;
        private float Tdelta;

        public void addNew(float[] inacce, long intime) {
            if (init) {
                acce = inacce;
                pos = new float[3];
                velo = new float[3];
                for (int i = 0; i < pos.length; i++) {
                    pos[i] = 0;
                    velo[i] = 0;
                }
                Tprev = intime;
                init = false;
                //return new sensordata(intime,pos);
            } else {
                acce = inacce;
                this.Tdelta = ((float) (intime - Tprev)) / 1000;
                for (int i = 0; i < acce.length; i++) {
                    velo[i] += Tdelta * acce[i];
                    pos[i] += velo[i] * Tdelta + 0.5 * acce[i] * Tdelta * Tdelta;
                    //set velo to 0 if static
                    if (proAcceGyroCali.getcaliLogSTATUS == true) {
                        velo[i] = 0;
                    }
                }

                Log.d(TAG, "POS: " + String.valueOf(acce[0]) + " " + String.valueOf(acce[1]) + " " + String.valueOf(velo[0]) + " " + String.valueOf(velo[1]) + " " + String.valueOf(pos[0]) + " " + String.valueOf(pos[1]) + " " + String.valueOf(Tdelta));
                Tprev = intime;
                /*
                LogCSV(String.valueOf(acce[0]),
                        String.valueOf(acce[1]),
                        String.valueOf(velo[0]),
                        String.valueOf(velo[1]),
                        String.valueOf(pos[0]),
                        String.valueOf(pos[1]));
*/
                //return new sensordata(intime,pos);
            }

        }

        public float[] getPos() {
            return pos;
        }

        public sensordata getData() {
            return new sensordata(Tprev, pos);
        }
    }

    //RK4
    public class Position {
        public double pos;      //position
        public double v;        //velocity
        public double a;        //acceleration

        public Position(double pos, double v) {
            this.pos = pos;
            this.v = v;
            a = 0;
        }
    }

    public class Derivative {
        public double dp;       //change in position
        public double dv;       //change in velocity

        public Derivative(double dp, double dv) {
            this.dp = dp;
            this.dv = dv;
        }
    }

    public class RK4 {
        public RK4() {
        }

        public Position integrate(Position position, double t, double dt, double acceleration) { //Heart of the RK4 integrator - I don't know what most of this is
            Derivative a = evaluate(position, t, 0, new Derivative(0, 0), acceleration);
            Derivative b = evaluate(position, t + dt * 0.5, dt * 0.5, a, acceleration);
            Derivative c = evaluate(position, t + dt * 0.5, dt * 0.5, b, acceleration);
            Derivative d = evaluate(position, t + dt, dt, c, acceleration);

            double dpdt = 1.0 / 6.0 * (a.dp + 2.0 * (b.dp + c.dp) + d.dp);
            double dvdt = 1.0 / 6.0 * (a.dv + 2.0 * (b.dv + c.dv) + d.dv);

            position.pos += dpdt * dt;
            position.v += dvdt * dt;
            if (proAcceGyroCali.getcaliLogSTATUS == true) {
                position.v = 0;
            }
            return position;
        }

        public double acceleration(Position position, double t) {        //Calculate all acceleration here - modify as needed
            double f = position.a;
            System.out.println(position.a);
            return f;
        }

        public Derivative evaluate(Position initial, double t, double dt, Derivative d, double acceleration) {   //Calculate new position based on change over time
            Position position = new Position(initial.pos + d.dp * dt, initial.v + d.dv * dt);       //New state influenced by derivatives of pos and v
            return new Derivative(position.v, acceleration);//acceleration(position, t));   //Calculate new derivative for new position
        }
    }


}
