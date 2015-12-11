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

import java.util.ArrayList;
import com.project.nicki.displaystabilizer.dataprocessor.proDataFlow;

/**
 * Created by nicki on 11/15/2015.
 */
public class stabilize_v1 implements Runnable {

    private static final String TAG = "stabilize_v1";
    public static Handler getDatas;
    public boolean LOGSTATUS;
    public int bundlenum = 1;
    public Object[] DataCollected = new Object[4];
    public int CalibrateMode = -1;
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
                if (DemoDraw.drawing==true && bundlegot != null) {
                    if (msg.arg1 == 1) {
                        if (bundlegot.getFloatArray("Movement") != null) {
                            CamDataArr.add(new stabilize_v1(bundlegot.getLong("Time"), bundlegot.getFloatArray("Movement")));
                            Log.d(TAG,"cameracameracamera "+CamDataArr.size());
                        }
                    }
                    if (msg.arg1 == 0) {
                        if (bundlegot.getFloatArray("Draw") != null) {
                            DrawDataArr.add(new stabilize_v1(bundlegot.getLong("Time"), bundlegot.getFloatArray("Draw")));
                        }
                    }
                    if (msg.arg1 == 2) {
                        AcceDataArr.add(new stabilize_v1(bundlegot.getLong("Time"), bundlegot.getFloatArray("Acce")));
                    }
                    if (msg.arg1 == 3) {
                        GyroDataArr.add(new stabilize_v1(bundlegot.getLong("Time"), bundlegot.getFloatArray("Gyro")));
                    }
                } else if (DrawDataArr.size() > 0) {
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
            for(int i =0 ; i<camDataIn.size();i++){
                Log.d(TAG,"cameramovement "+ String.valueOf(camDataIn.size())+" "+String.valueOf(camDataIn.get(i).Data[0]) +" "+ String.valueOf(camDataIn.get(i).Data[1]));
            }

            if (drawDataIn != null) {
                if (CalibrateMode < 0) {

                    float[][] drawDataOut;
                    int Length = drawDataIn.size();
                    drawDataOut = new float[Length - 1][2];
                    for (int i = 0; i < Length - 1; i++) {

                        //if (i < camDataIn.size() - 1) {
                        float drawDataX = drawDataIn.get(i).Data[0];
                        float drawDataY = drawDataIn.get(i).Data[1];


                        //stabilization here


                        try{
                            drawDataOut[i][0] = drawDataX ;// - camDataIn.get(i).Data[0] * 10;
                            drawDataOut[i][1] = drawDataY;// - camDataIn.get(i).Data[1] * 10;
                            Log.d(TAG, "iiiiii " + drawDataX + " " + drawDataY + " " + String.valueOf(drawDataOut[i][0]) + " " + String.valueOf(drawDataOut[i][0]));
                        }catch (Exception ec){

                        }


                        //}


                    }
                    Log.d(TAG, "iiiiii ");
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("DrawPoints", drawDataOut);
                    Message msg2 = new Message();
                    msg2.setData(bundle);
                    //DemoDrawUI.DrawStabilizerHandler.sendMessage(msg2);


                    for (int j = 0; j < drawDataOut.length - 1; j++) {
                        Log.d(TAG, "aaaaaaaaaaaaaaaaa " + drawDataOut[j][0] + " " + drawDataOut[j][1]);
                    }
                    //Log.d(TAG, "This is Override    " + String.valueOf(DrawPoints[2][0]) + " " + String.valueOf(DrawPoints[0][0]));

                    DemoDraw.paint2.setColor(Color.BLUE);

                    for (int i = 1; i < drawDataOut.length - 1; i++) {
                        DemoDraw.path2.moveTo(drawDataOut[i][0], drawDataOut[i][1]);
                        DemoDraw.path2.lineTo(drawDataOut[i + 1][0], drawDataOut[i + 1][1]);
                    }
                    Message msg3 = new Message();
                    msg3.what = 1;
                    DemoDraw.mhandler.sendMessage(msg3);


                } else {
                    CalibrateMode = CalibrateMode - 1;

                    try {
                        double drawlength = Math.pow(Math.pow(drawDataIn.get(drawDataIn.size() - 1).Data[0] - drawDataIn.get(0).Data[0], 2) + Math.pow(drawDataIn.get(drawDataIn.size() - 1).Data[1] - drawDataIn.get(0).Data[1], 2), 0.5);
                        double camlength = 0;
                        double camsumX = 0;
                        double camsumY = 0;
                        for (int k = 0; k < camDataIn.size(); k++) {
                            camsumX = camsumX + camDataIn.get(k).Data[0];
                            camsumY = camsumY + camDataIn.get(k).Data[1];
                            Log.d(TAG, "CAMERAVALUE " + String.valueOf(camsumX) + " " + String.valueOf(camsumY));
                        }
                        camlength = Math.pow(Math.pow(camsumX, 2) + Math.pow(camsumY, 2), 0.5);
                        Log.d(TAG, "drawlength " + String.valueOf(drawlength) + " camlength " + String.valueOf(camDataIn.get(0).Data[0]));
                    } catch (Exception ex) {

                    }

                }

            }
        }
    }
}
