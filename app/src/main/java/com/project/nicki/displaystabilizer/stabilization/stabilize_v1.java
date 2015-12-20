package com.project.nicki.displaystabilizer.stabilization;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.project.nicki.displaystabilizer.UI.DemoDrawUI;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;

import java.math.BigInteger;
import java.util.ArrayList;

import com.project.nicki.displaystabilizer.dataprocessor.proDataFlow;
import com.project.nicki.displaystabilizer.dataprovider.getAccelerometer;

/**
 * Created by nicki on 11/15/2015.
 */
public class stabilize_v1 implements Runnable {

    private static final String TAG = "stabilize_v1";
    public static Handler getDatas;
    public boolean LOGSTATUS;
    public int bundlenum = 1;
    public Object[] DataCollected = new Object[4];
    public int CalibrateMode = 100;
    public boolean switchLOGpre = false;
    public boolean switchLOGcur = false;
    public boolean switchLOG = false;
    public float camera_screen_multiplyfactor;
    ArrayList<stabilize_v1> DrawDataArr = new ArrayList<stabilize_v1>();
    ArrayList<stabilize_v1> CamDataArr = new ArrayList<stabilize_v1>();
    ArrayList<stabilize_v1> AcceDataArr = new ArrayList<stabilize_v1>();
    ArrayList<stabilize_v1> GyroDataArr = new ArrayList<stabilize_v1>();
    private Context mContext;
    private long Time = 0;
    private float[] Data = new float[2];
    private float dxwhenstatic, dywhenstatic;
    private long tmpTime = System.currentTimeMillis();
    private int stoppattern = 0;

    public stabilize_v1(Context context) {
        mContext = context;
    }

    public stabilize_v1(long time, float[] data) {
        Time = time;
        Data = data;
    }


    @Override
    public void run() {
        Looper.prepare();
        getDatas = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle bundlegot = msg.getData();

                //Calibration

                if (CalibrateMode > 0) {
                    if (msg.arg1 == 2) {
                        CalibrateMode = CalibrateMode - 1;
                        float accetmpX = bundlegot.getFloatArray("Acce")[0];
                        float accetmpY = bundlegot.getFloatArray("Acce")[1];
                        if (dxwhenstatic == 0 || dywhenstatic == 0) {
                            dxwhenstatic = Math.abs(accetmpX);
                            dywhenstatic = Math.abs(accetmpY);
                        } else {
                            dxwhenstatic = (dxwhenstatic + accetmpX) / 2;
                            dywhenstatic = (dywhenstatic + accetmpY) / 2;
                        }
                        Log.d(TAG, "whenstatic " + dxwhenstatic + " " + dywhenstatic);
                    }
                }

                    if (DemoDraw.drawing == true && bundlegot != null) {
                        if (msg.arg1 == 1) {
                            CamDataArr.add(new stabilize_v1(bundlegot.getLong("Time"), bundlegot.getFloatArray("Movement")));
                            Log.d(TAG, "cameracameracamera " + CamDataArr.size());

                        }
                        if (msg.arg1 == 0) {
                            if (bundlegot.getFloatArray("Draw") != null) {
                                DrawDataArr.add(new stabilize_v1(bundlegot.getLong("Time"), bundlegot.getFloatArray("Draw")));
                            }
                        }

                        if (msg.arg1 == 2) {
                            if (bundlegot.getFloatArray("Acce")[0] > 0 && bundlegot.getFloatArray("Acce")[1] > 0) {
                                bundlegot.getFloatArray("Acce")[0] = bundlegot.getFloatArray("Acce")[0] - dxwhenstatic;
                                bundlegot.getFloatArray("Acce")[1] = bundlegot.getFloatArray("Acce")[1] - dywhenstatic;
                                if (Math.abs(bundlegot.getFloatArray("Acce")[0]) < Math.abs(dxwhenstatic) && Math.abs(bundlegot.getFloatArray("Acce")[1]) < Math.abs(dywhenstatic)) {
                                    stoppattern = stoppattern+1;
                                    if(stoppattern>60){
                                        bundlegot.getFloatArray("Acce")[0] = 0;
                                        bundlegot.getFloatArray("Acce")[0] = 0;
                                        stoppattern = 0;
                                    }
                                }

                                AcceDataArr.add(new stabilize_v1(bundlegot.getLong("Time"), bundlegot.getFloatArray("Acce")));
                            }
                        }



                        if (msg.arg1 == 3) {
                            GyroDataArr.add(new stabilize_v1(bundlegot.getLong("Time"), bundlegot.getFloatArray("Gyro")));
                        }
                    }else if(DemoDraw.drawing == false && DrawDataArr.size() > 0 && AcceDataArr.size()>0){
                        tmpTime = System.currentTimeMillis();
                        DataCollected[0] = DrawDataArr;
                        DataCollected[1] = CamDataArr;
                        DataCollected[2] = AcceDataArr;
                        DataCollected[3] = GyroDataArr;


                        new Thread(new Stabilization(DataCollected)).start();
                        Log.d(TAG, "Collect stopped");
                        DataCollected = new Object[4];
                        DrawDataArr = new ArrayList<stabilize_v1>();
                        CamDataArr = new ArrayList<stabilize_v1>();
                        AcceDataArr = new ArrayList<stabilize_v1>();
                        GyroDataArr = new ArrayList<stabilize_v1>();
                    }

/*
                if (DrawDataArr.size() > 20 && AcceDataArr.size()>20 ) {
                         tmpTime = System.currentTimeMillis();
                        DataCollected[0] = DrawDataArr;
                        DataCollected[1] = CamDataArr;
                        DataCollected[2] = AcceDataArr;
                        DataCollected[3] = GyroDataArr;


                        new Thread(new Stabilization(DataCollected)).start();
                        Log.d(TAG, "Collect stopped");
                        DataCollected = new Object[4];
                        DrawDataArr = new ArrayList<stabilize_v1>();
                        CamDataArr = new ArrayList<stabilize_v1>();
                        AcceDataArr = new ArrayList<stabilize_v1>();
                        GyroDataArr = new ArrayList<stabilize_v1>();

                    }

                //clean data if hands up
                if(DemoDraw.drawing == false && DrawDataArr.size() > 0 && AcceDataArr.size()>0 )   {
                    tmpTime = System.currentTimeMillis();
                    DataCollected[0] = DrawDataArr;
                    DataCollected[1] = CamDataArr;
                    DataCollected[2] = AcceDataArr;
                    DataCollected[3] = GyroDataArr;
                    new Thread(new Stabilization(DataCollected)).start();
                    Log.d(TAG, "Collect stopped");
                    DataCollected = new Object[4];
                    DrawDataArr = new ArrayList<stabilize_v1>();
                    CamDataArr = new ArrayList<stabilize_v1>();
                    AcceDataArr = new ArrayList<stabilize_v1>();
                    GyroDataArr = new ArrayList<stabilize_v1>();
                }
                */
            }
        };
        Looper.loop();
    }

    public class Stabilization implements Runnable {
        Object[] threadDataCollected = new Object[4];

        public Stabilization(Object[] gotDataPackage) {
            this.threadDataCollected = gotDataPackage;
        }

        @Override
        public void run() {
            Log.d(TAG, "now on thread");
            ArrayList<stabilize_v1> drawDataIn = (ArrayList<stabilize_v1>) threadDataCollected[0];
            ArrayList<stabilize_v1> camDataIn = (ArrayList<stabilize_v1>) threadDataCollected[1];
            ArrayList<stabilize_v1> acceDataIn = (ArrayList<stabilize_v1>) threadDataCollected[2];
            ArrayList<stabilize_v1> gyroDataIn = (ArrayList<stabilize_v1>) threadDataCollected[3];
            for (int i = 0; i < camDataIn.size(); i++) {
                Log.d(TAG, "cameramovement " + String.valueOf(camDataIn.size()) + " " + String.valueOf(camDataIn.get(i).Data[0]) + " " + String.valueOf(camDataIn.get(i).Data[1]));
            }
            Log.d(TAG, "SIZE: =" + camDataIn.size());
            if (drawDataIn != null) {
                if (CalibrateMode < 1
                        ) {

                    float[][] drawDataOut;
                    int Length = drawDataIn.size();
                    drawDataOut = new float[Length][2];

                    Log.d(TAG, "cameraperfered ");
                    Log.d(TAG, "FIXNOFIXED" + " dx: yyyyyyyyy");
                    for (int i = 0; i < Length; i++) {

                        float drawDataX = drawDataIn.get(i).Data[0];
                        float drawDataY = drawDataIn.get(i).Data[1];


                        long timetocompare = acceDataIn.get(0).Time;
                        int perferedindex = 0;
                        //Stabilization
                        /*
                        Log.d(TAG, "DEBUG");
                        for (int k = 0; k < acceDataIn.size(); k++) {
                            Log.d(TAG,"DEBUG this  "+String.valueOf(Math.abs(drawDataIn.get(i).Time - acceDataIn.get(k).Time)) );
                            Log.d(TAG,"DEBUG bef   "+ String.valueOf(Math.abs(drawDataIn.get(i).Time - timetocompare)));
                            if (Math.abs(drawDataIn.get(i).Time - acceDataIn.get(k).Time) < Math.abs(drawDataIn.get(i).Time - timetocompare)) {
                                Log.d(TAG, "DEBUG stabil " + " this " + drawDataIn.get(i).Time + " innnerloop " + drawDataIn.get(k).Time + " prevloop " + String.valueOf(timetocompare) + " innerk " + k + " this " + i);
                                timetocompare = acceDataIn.get(k).Time;
                                perferedindex = k;
                            }

                            // && drawDataIn.get(i).Time > acceDataIn.get(k).Time
                        }
*/
                        Log.d(TAG, "compare loop =================================================");
                        for (int k = 0; k < acceDataIn.size() - 1; k++) {
                            Log.d(TAG, "compare  " + acceDataIn.get(k).Time + " " + drawDataIn.get(i).Time);
                            if (acceDataIn.get(k).Time < drawDataIn.get(i).Time) {
                                perferedindex = k;
                            } else {
                                Log.d(TAG, "underflow");
                                break;
                            }
                            Log.d(TAG, "compare loop " + perferedindex);
                        }
                        Log.d(TAG, "cameraperfered " + perferedindex + " " + drawDataIn.get(i).Time + " " + acceDataIn.get(perferedindex).Time);

                        float dx = 0;
                        float dy = 0;



/*
                        dx = acceDataIn.get(perferedindex).Data[0] *((drawDataIn.get(i).Time - acceDataIn.get(perferedindex).Data[0])/(acceDataIn.get(perferedindex+1).Time - acceDataIn.get(perferedindex).Time))+
                                acceDataIn.get(perferedindex+1).Data[0]*(acceDataIn.get(perferedindex +1).Time-drawDataIn.get(i).Time)/(acceDataIn.get(perferedindex+1).Time - acceDataIn.get(perferedindex).Time);
                        dy = acceDataIn.get(perferedindex).Data[1] *((drawDataIn.get(i).Time - acceDataIn.get(perferedindex).Data[1])/(acceDataIn.get(perferedindex+1).Time - acceDataIn.get(perferedindex).Time))+
                                acceDataIn.get(perferedindex+1).Data[1]*(acceDataIn.get(perferedindex +1).Time-drawDataIn.get(i).Time)/(acceDataIn.get(perferedindex+1).Time - acceDataIn.get(perferedindex).Time);
                                */

                        Log.d(TAG, "cameraperfered  " + perferedindex);

                        for (int h = 0; h < perferedindex; h++) {
                            dx = dx - acceDataIn.get(h).Data[0] * 30;
                            dy = dy - acceDataIn.get(h).Data[1] * 30;
                            Log.d(TAG, "FIXNOFIXED" + " dx: " + String.valueOf(dx) + " per " + perferedindex + " " + h);
                        }
                        Log.d(TAG, "dxdy " + dx + " " + dy);
                        drawDataOut[i][0] = drawDataX - dx;
                        drawDataOut[i][1] = drawDataY - dy;
                        Log.d(TAG, "FIXNOFIXED fin " + " BEF: " + String.valueOf(drawDataX) + " FIX " + String.valueOf(dx) + " AFT " + String.valueOf(drawDataOut[i][0]) + "        munaully: " + String.valueOf(drawDataOut[i][0] = drawDataX + dx));

                        /*
                        drawDataOut[i][0] = 400;
                        drawDataOut[i][1] = 400;
                        try{
                        */

                        /*
                        for (int q = 0; q < perferedindex + 1; q++) {
                            drawDataOut[i][0] = drawDataOut[i][0] - acceDataIn.get(q).Data[1] * 80;
                            drawDataOut[i][1] = drawDataOut[i][1] - acceDataIn.get(q).Data[2] *80;
*/
/*
                                drawDataOut[i][0] = drawDataOut[i][0] - camDataIn.get(q).Data[0]*3;
                                drawDataOut[i][1] = drawDataOut[i][1] - camDataIn.get(q).Data[1]*3;



                        }
                    */

                    }


                    DemoDraw.paint2.setColor(Color.BLUE);

                    for (int i = 1; i < drawDataOut.length - 1; i++) {
                        DemoDraw.path2.moveTo(drawDataOut[i][0], drawDataOut[i][1]);
                        DemoDraw.path2.lineTo(drawDataOut[i + 1][0], drawDataOut[i + 1][1]);
                    }
                    Message msg3 = new Message();
                    msg3.what = 1;
                    DemoDraw.mhandler.sendMessage(msg3);
                    Log.d(TAG, "FIXNOFIXED" + "LOOP");

                } else {


                }

            }
        }
    }


}
