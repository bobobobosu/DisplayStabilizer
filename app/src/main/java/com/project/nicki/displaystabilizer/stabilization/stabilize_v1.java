package com.project.nicki.displaystabilizer.stabilization;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
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
import com.project.nicki.displaystabilizer.dataprocessor.utils.LevenbergMarquardt;
import org.ejml.data.*;
import org.ejml.data.DenseMatrix64F;

import static org.ejml.ops.CommonOps.*;
import static org.ejml.ops.SpecializedOps.diffNormF;
/**
 * Created by nicki on 11/15/2015.
 */
public class stabilize_v1 implements Runnable {

    private static final String TAG = "stabilize_v1";
    public static Handler getDatas;
    public Object[] DataCollected = new Object[4];
    public int CalibrateMode = 100;
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
                Log.d(TAG, "DEBUG: " + String.valueOf(msg.what));
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

                if (DemoDraw.drawing <2 && bundlegot != null ) {
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
                        Log.d(TAG,"DEBUGACCE: "+"ACCEGOT");
                        if (bundlegot.getFloatArray("Acce")[0] > 0 && bundlegot.getFloatArray("Acce")[1] > 0) {
                            bundlegot.getFloatArray("Acce")[0] = bundlegot.getFloatArray("Acce")[0] - dxwhenstatic;
                            bundlegot.getFloatArray("Acce")[1] = bundlegot.getFloatArray("Acce")[1] - dywhenstatic;
                            if (Math.abs(bundlegot.getFloatArray("Acce")[0]) < Math.abs(dxwhenstatic) && Math.abs(bundlegot.getFloatArray("Acce")[1]) < Math.abs(dywhenstatic)) {
                                stoppattern = stoppattern + 1;
                                if (stoppattern > 60) {
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
                    //real time stabilize
                    /*
                    if(AcceDataArr.size()%7 == 0 && AcceDataArr.size()>0){
                        tmpTime = System.currentTimeMillis();
                        DataCollected[0] = DrawDataArr;
                        DataCollected[1] = CamDataArr;
                        DataCollected[2] = AcceDataArr;
                        DataCollected[3] = GyroDataArr;
                        new Thread(new Stabilization(DataCollected)).start();
                        DataCollected = new Object[4];
                    }
                    */

                } else if (DemoDraw.drawing <2 && DrawDataArr.size() > 0 && AcceDataArr.size() > 0) {
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
                if (CalibrateMode < 1) {
                    float[][] drawDataOut;
                    int Length = drawDataIn.size();
                    drawDataOut = new float[Length][2];
                    for (int i = 0; i < Length; i++) {
                        float drawDataX = drawDataIn.get(i).Data[0];
                        float drawDataY = drawDataIn.get(i).Data[1];
                        int perferedindex = 0;

                        Log.d(TAG, "compare loop =================================================");
                        for (int k = 0; k < acceDataIn.size() - 1; k++) {
                            Log.d(TAG, "compare  " + acceDataIn.get(k).Time + " " + drawDataIn.get(i).Time);
                            if (acceDataIn.get(k).Time < drawDataIn.get(i).Time) {
                                perferedindex = k;
                            } else {
                                Log.d(TAG, "underflow");
                                break;
                            }
                        }
                        float dx = 0;
                        float dy = 0;
                        for (int h = 0; h < perferedindex; h++) {
                            dx = dx - acceDataIn.get(h).Data[0] * 30;
                            dy = dy - acceDataIn.get(h).Data[1] * 30;
                        }
                        Log.d(TAG, "dxdy " + dx + " " + dy);
                        drawDataOut[i][0] = drawDataX - dx ;
                        drawDataOut[i][1] = drawDataY - dy;
                    }
                    for (int i = 1; i < drawDataOut.length - 1; i++) {
                        drawDataOut[i][0] = drawDataOut[i][0] + drawDataIn.get(drawDataIn.size()-1).Data[0] - drawDataOut[drawDataOut.length-1][0];
                        drawDataOut[i][1] = drawDataOut[i][1] + drawDataIn.get(drawDataIn.size()-1).Data[1] - drawDataOut[drawDataOut.length-1][1];
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
