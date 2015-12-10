package com.project.nicki.displaystabilizer.dataprocessor;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.project.nicki.displaystabilizer.UI.DemoDrawUI;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v1;

/**
 * Created by nicki on 11/14/2015.
 */
public class proDataFlow implements Runnable {
    private static final String TAG = "proDataFlow";
    public static Handler drawHandler;
    public static Handler CameraHandler;
    public static Handler AcceHandler;
    public static Handler GyroHandler;
    public boolean LOGSTATUS = true;

    Runnable run = new Runnable() {
        @Override
        public void run() {

            try {
                Log.d(TAG, "data " + "Time=" + proCamera.data[0] + " deltaX=" + proCamera.data[1] + " deltaY=" + proCamera.data[2]);
            } catch (Exception ex) {
            }


        }
    };
    private Context mContext;

    public proDataFlow(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        try {
            Log.d(TAG, "run start");
        } catch (Exception ex) {
        }

        ///Handlers
        Looper.prepare();
        drawHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                    case 1:
                        LOGSTATUS = true;
                        Log.d(TAG, "Start");
                        Bundle DrawBundle = new Bundle();
                        DrawBundle = msg.getData();
                        final float[] DrawData = DrawBundle.getFloatArray("Draw");
                        final long DrawTime = DrawBundle.getLong("Time");
                        Log.d(TAG, "DrawDATA@ " + "Time:" + String.valueOf(DrawTime) + " X:" + String.valueOf(DrawData[0]) + " Y:" + String.valueOf(DrawData[1]));
                        sendData(LOGSTATUS, msg.getData(), 0);

                        DemoDrawUI.runOnUI(new Runnable() {
                            @Override
                            public void run() {
                                DemoDrawUI.mlog_draw.setText("DrawDATA@ " + "Time:" + String.valueOf(DrawTime) + " X:" + String.valueOf(DrawData[0]) + " Y:" + String.valueOf(DrawData[1]));
                            }
                        });


                    case 2:
                        LOGSTATUS = false;

                        Bundle fDrawBundle = new Bundle();
                        fDrawBundle = msg.getData();
                        final float[] fDrawData = fDrawBundle.getFloatArray("Draw");
                        final long fDrawTime = fDrawBundle.getLong("Time");
                        Log.d(TAG, "DrawDATA@ " + "Time:" + String.valueOf(fDrawTime) + " X:" + String.valueOf(fDrawData[0]) + " Y:" + String.valueOf(fDrawData[1]));

                        DemoDrawUI.runOnUI(new Runnable() {
                            @Override
                            public void run() {
                                DemoDrawUI.mlog_draw.setText("DrawDATA@ " + "Time:" + String.valueOf(fDrawTime) + " X:" + String.valueOf(fDrawData[0]) + " Y:" + String.valueOf(fDrawData[1]));
                            }
                        });
                        //Bundle nullbundle = new Bundle();
                        sendData(LOGSTATUS, fDrawBundle, 0);
                        Log.d(TAG, "Stop");


                }

            }
        };
        CameraHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle MovementBundle = new Bundle();
                MovementBundle = msg.getData();
                double[] MovementData = new double[3];
                MovementData = MovementBundle.getDoubleArray("Movement");
                final long CameraTime = MovementBundle.getLong("Time");
                Log.d(TAG, "CameraDATA@ " + "Time:" + String.valueOf(CameraTime) + " X:" + String.valueOf(MovementData[0]) + " Y:" + String.valueOf(MovementData[1]));

                final double[] finalMovementData = MovementData;
                DemoDrawUI.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        DemoDrawUI.mlog_cam.setText("CameraDATA@ " + "Time:" + String.valueOf(CameraTime) + " X:" + String.valueOf(finalMovementData[0]) + " Y:" + String.valueOf(finalMovementData[1]));
                    }
                });
                sendData(LOGSTATUS, MovementBundle, 1);
            }
        };
        AcceHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle AcceBundle = new Bundle();
                AcceBundle = msg.getData();
                float[] AcceData = new float[3];
                final long AcceTime;
                AcceData = AcceBundle.getFloatArray("Acce");
                AcceTime = AcceBundle.getLong("Time");
                Log.d(TAG, "AcceDATA@ " + "Time:" + String.valueOf(AcceTime) + " X:" + String.valueOf(AcceData[0]) + " Y:" + String.valueOf(AcceData[1]) + " Z:" + String.valueOf(AcceData[2]));

                final float[] finalAcceData = AcceData;


                DemoDrawUI.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        DemoDrawUI.mlog_acce.setText("AcceDATA@ " + "Time:" + String.valueOf(AcceTime) + " X:" + String.valueOf(finalAcceData[0]) + " Y:" + String.valueOf(finalAcceData[1]));
                    }
                });


                sendData(LOGSTATUS, AcceBundle, 2);
            }
        };
        GyroHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle GyroBundle = new Bundle();
                GyroBundle = msg.getData();
                float[] GyroData = new float[3];
                final long GyroTime;
                GyroData = GyroBundle.getFloatArray("Gyro");
                GyroTime = GyroBundle.getLong("Time");
                //Log.d(TAG, "GyroDATA@ " + "Time:" + String.valueOf(GyroTime) + " X:" + String.valueOf(GyroData[0]) + " Y:" + String.valueOf(GyroData[1]) + " Z:" + String.valueOf(GyroData[2]));
                final float[] finalGyroData = GyroData;
                Log.d(TAG, " GyroDATA@ " + "Time:" + String.valueOf(GyroTime) + " X:" + String.valueOf(finalGyroData[0]) + " Y:" + String.valueOf(finalGyroData[1]));

                /*
                DemoDrawUI.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        DemoDrawUI.mlog_gyro.setText("GyroDATA@ " + "Time:" + String.valueOf(GyroTime) + " X:" + String.valueOf(finalGyroData[0]) + " Y:" + String.valueOf(finalGyroData[1]));
                    }
                });
                */

                DemoDrawUI.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        DemoDrawUI.mlog_gyro.setText("GyroDATA@ " + "Time:" + String.valueOf(GyroTime) + " X:" + String.valueOf(finalGyroData[0]) + " Y:" + String.valueOf(finalGyroData[1]));
                    }
                });


                sendData(LOGSTATUS, GyroBundle, 3);
            }
        };

        Looper.loop();

    }

    public void sendData(boolean LOGSTATUS, Bundle data, int type) {
        Message message = new Message();
        if (LOGSTATUS == true) {
            message.what = 1;
            switch (type) {
                case 0:
                    message.arg1 = 0;
                    break;
                case 1:
                    message.arg1 = 1;
                    break;
                case 2:
                    message.arg1 = 2;
                    break;
                case 3:
                    message.arg1 = 3;
                    break;
            }

            Log.d(TAG, "TEST " + String.valueOf(message.arg1));
            message.setData(data);
            stabilize_v1.getDatas.sendMessage(message);
        } else {
            switch (type) {
                case 0:
                    message.arg1 = 0;
                    break;
                case 1:
                    message.arg1 = 1;
                    break;
                case 2:
                    message.arg1 = 2;
                    break;
                case 3:
                    message.arg1 = 3;
                    break;
                default:
                    message.arg1 = 4;
                    break;
            }
            message.what = 0;

            message.setData(data);
            stabilize_v1.getDatas.sendMessage(message);
        }

    }


}

