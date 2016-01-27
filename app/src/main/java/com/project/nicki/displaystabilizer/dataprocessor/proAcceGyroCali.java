package com.project.nicki.displaystabilizer.dataprocessor;

/**
 * Created by nickisverygood on 12/25/2015.
 */

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.project.nicki.displaystabilizer.UI.DemoDrawUI;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LevenbergMarquardt;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v2;

import org.ejml.data.DenseMatrix64F;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import jama.Matrix;
import jkalman.JKalman;


public class proAcceGyroCali extends getAcceGyro {
    public static boolean getcaliLogSTATUS = false;
    public static int selectedMethod;
    public static boolean pendingUpdate = false;
    public static Handler applypara = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d("TAG", "Parameters");
            pendingUpdate = true;
        }
    };
    /////////////////////////////////////
    public static float multiplier = 0;
    public static sensordata tmpgyrodata;
    public static int nowparam = 1;
    public static float nowmultip = 1;
    //noshake
    private final int SENEOR_TYPE = Sensor.TYPE_LINEAR_ACCELERATION;
    private final int ACCELEROMOTER_FPS = SensorManager.SENSOR_DELAY_FASTEST;
    private final int BUFFER_SECOND = 4;
    private final int FPS = 60;
    private final int BUFFER_DATA_SIZE = BUFFER_SECOND * FPS;
    public int HistoryLength = 100;
    public ArrayList<sensordata> AcceCircular = new ArrayList<>();
    //for staticdetetc()
    public ArrayList<sensordata> lAcceCircular = new ArrayList<>();
    public boolean caliLogSTATUS = false;
    public List<sensordata> cirbuff = new ArrayList<sensordata>();
    //init
    public boolean controllerinit = false;
    public boolean Calibrationinit = false;
    public boolean ctrlerCalibrationinit = false;
    public boolean NoShakeinit = false;
    public boolean RK4init = false;
    public boolean Eularinit = false;
    public boolean rotationMatrixinit = false;
    //rotateVector
    public rotateVector mrotateVector;
    public Rolling avg1;
    public Rolling avg2;
    public Rolling avg3;
    public highPass RK4_HP_a_X = new highPass(0.99f);
    public long prevautotime = System.currentTimeMillis();
    public float lowpass = 0;
    public float highpass = 0;
    public float coeff = 0;
    public int modauto = 0;
    long static_start_time;
    float prevdx = 0;
    float prevdy = 0;
    String TAG = "proAcceGyroCali";
    String csvName = "proAcceGyroCali.csv";
    ArrayList<sensordata> tmpcalibuffer = new ArrayList<>();
    ArrayList<ArrayList<sensordata>> caliAccebuffer;
    double g = 9.806 - (1 / 2) * (9.832 - 9.780) * Math.cos(2 * 25.048212 * Math.PI / 180);
    //LogEverything
    ArrayList<sensordata> rawAcceAll = new ArrayList<sensordata>();
    //rk4
    RK4 mrk4_X = new RK4();
    Position RK4_initX = new Position(0, 0);
    RK4 mrk4_Y = new RK4();
    Position RK4_initY = new Position(0, 0);
    calLowPass RK4_LP_a_X;
    calLowPass RK4_LP_a_Y;
    double previnitX = 0;
    double previnitY = 0;
    //eular
    calEular mcalEular = new calEular();
    LevenbergMarquardt mLM;
    DenseMatrix64F paramgot;
    FileWriter mFileWriter;
    sensordata Eular_sensordataOUT;
    highPass RK4_HP_v_Y = new highPass(0f);
    highPass RK4_HP_p_X = new highPass(0f);
    highPass Eular_HP_a_Y = new highPass(0f);
    highPass Eular_HP_v_Y = new highPass(0f);
    highPass Eular_HP_p_Y = new highPass(0f);
    highPass Eular_HP_a_X = new highPass(0f);
    highPass Eular_HP_v_X = new highPass(0f);
    highPass Eular_HP_p_X = new highPass(0f);
    highPass RK4_HP_a_Y = new highPass(0f);
    highPass RK4_HP_v_X = new highPass(0f);
    highPass RK4_HP_p_Y = new highPass(0f);
    Rolling Eular_avg1;
    Rolling Eular_avg2;
    Rolling Eular_avg3;
    Rolling RK4_avg1;
    Rolling RK4_avg2;
    Rolling RK4_avg3;
    Rolling NSK_avg1;
    Rolling NSK_avg2;
    // private tcpipdata mtcpipdata = new tcpipdata();
    calLowPass NSK_LP_a_X = new calLowPass(0.9f);
    calLowPass NSK_LP_a_Y = new calLowPass(0.9f);
    highPass NSK_HP_a_X = new highPass(0.99f);
    highPass NSK_HP_v_X = new highPass(0.99f);
    highPass NSK_HP_p_X = new highPass(0.99f);
    highPass NSK_HP_a_Y = new highPass(0.99f);
    highPass NSK_HP_v_Y = new highPass(0.99f);
    highPass NSK_HP_p_Y = new highPass(0.99f);
    long RK4_last_timestamp;
    /////////////////////////////paras
    Rolling NSK_avg3;
    float Eular_staticOFFSET;
    float RK4_staticOFFSET;
    int staticnum;
    Context mContext;
    //gyro
    gyrointegration mgyrointegration = new gyrointegration();
    float[] NSK_Pos = new float[]{0, 0, 0};
    long NSK_last_timestamp = System.currentTimeMillis();
    //time related
    double sampleduration = 0;
    samplerate msamplerate = new samplerate();
    calAllan mcalAllan = new calAllan();
    private float[] prevEular;
    private calLowPass Eular_LP_a_X;
    private calLowPass Eular_LP_a_Y;
    //gyro
    private RK4 RK4_gyro;
    private int Asamplenum = 50;
    private float[] AcceXsam = new float[Asamplenum];
    private float[] AcceYsam = new float[Asamplenum];
    private float[] AcceZsam = new float[Asamplenum];
    private int Aintsam = 0;
    private int OFFSET_SCALE = 30;
    private CircularBuffer mBufferX = new CircularBuffer(50, 10);
    private CircularBuffer mBufferY = new CircularBuffer(50, 10);
    private boolean didCalibraiotn = false;
    private float[] lAcceXsam = new float[Asamplenum];
    private float[] lAcceYsam = new float[Asamplenum];
    private float[] lAcceZsam = new float[Asamplenum];
    private KalmanFilter Eularkal_a = new KalmanFilter();
    private KalmanFilter Eularkal_v = new KalmanFilter();
    private KalmanFilter Eularkal_p = new KalmanFilter();
    private KalmanFilter RK4kal_a = new KalmanFilter();
    private KalmanFilter RK4kal_v = new KalmanFilter();
    private KalmanFilter RK4kal_p = new KalmanFilter();
    private KalmanFilter NoShakekal_a = new KalmanFilter();
    private KalmanFilter NoShakekal_v = new KalmanFilter();
    private KalmanFilter NoShakekal_p = new KalmanFilter();
    private boolean precaliLogSTATUS = caliLogSTATUS;
    private display mdisplay = new display();

    public proAcceGyroCali(Context context) {
        super(context);
        mContext = context;
        //LM init
        mLM = new LevenbergMarquardt(new LevenbergMarquardt.Function() {
            @Override
            public void compute(DenseMatrix64F param, DenseMatrix64F x, DenseMatrix64F y, ArrayList<sensordata> data) {
                Log.d(TAG, "LM: datasize: " + String.valueOf(data.size()));
                for (int i = 0; i < x.getNumElements(); i++) {
                    DenseMatrix64F getxfromdata = new DenseMatrix64F(3, 1);
                    getxfromdata.set(0, 0, data.get(i).getData()[0]);
                    getxfromdata.set(1, 0, data.get(i).getData()[1]);
                    getxfromdata.set(2, 0, data.get(i).getData()[2]);
                    y.set(i, 0, acceCali(getxfromdata, param));
                }
            }
        });


    }

    public static float getVarianceMagnitude(float a, float b, float c) {
        BigDecimal Ba = new BigDecimal(String.valueOf(a));
        BigDecimal Bb = new BigDecimal(String.valueOf(b));
        BigDecimal Bc = new BigDecimal(String.valueOf(c));
        MathContext mc = new MathContext(50, RoundingMode.HALF_DOWN);
        BigDecimal BVarianceMagnitude = new BigDecimal(String.valueOf(Ba.pow(2).add(Bb.pow(2).add(Bc.pow(2)))), mc);
        double dVarianceMagnitude = Math.pow(BVarianceMagnitude.doubleValue(), 0.5);
        float VarianceMagnitude = (float) dVarianceMagnitude;
        return VarianceMagnitude;
    }

    public static double[][] multiplyByMatrix(double[][] m1, double[][] m2) {
        int m1ColLength = m1[0].length; // m1 columns length
        int m2RowLength = m2.length;    // m2 rows length
        if (m1ColLength != m2RowLength) return null; // matrix multiplication is not possible
        int mRRowLength = m1.length;    // m result rows length
        int mRColLength = m2[0].length; // m result columns length
        double[][] mResult = new double[mRRowLength][mRColLength];
        for (int i = 0; i < mRRowLength; i++) {         // rows from m1
            for (int j = 0; j < mRColLength; j++) {     // columns from m2
                for (int k = 0; k < m1ColLength; k++) { // columns from m1
                    mResult[i][j] += m1[i][k] * m2[k][j];
                }
            }
        }
        return mResult;
    }

    public static double[] convertFloatsToDoubles(float[] input) {
        if (input == null) {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = input[i];
        }
        return output;
    }

    public void updateParas() {
        NSK_avg1 = new Rolling(Integer.parseInt(DemoDrawUI.mMovingAvg.getText().toString()));
        NSK_avg2 = new Rolling(Integer.parseInt(DemoDrawUI.mMovingAvg.getText().toString()));
        NSK_avg3 = new Rolling(Integer.parseInt(DemoDrawUI.mMovingAvg.getText().toString()));
        RK4_avg1 = new Rolling(Integer.parseInt(DemoDrawUI.mMovingAvg.getText().toString()));
        RK4_avg2 = new Rolling(Integer.parseInt(DemoDrawUI.mMovingAvg.getText().toString()));
        RK4_avg3 = new Rolling(Integer.parseInt(DemoDrawUI.mMovingAvg.getText().toString()));
        RK4_LP_a_X = new calLowPass(Float.valueOf(DemoDrawUI.mLP.getText().toString()));
        RK4_LP_a_Y = new calLowPass(Float.valueOf(DemoDrawUI.mLP.getText().toString()));
        RK4_HP_a_X = new highPass(Float.valueOf(DemoDrawUI.mHPa.getText().toString()));
        RK4_HP_v_X = new highPass(Float.valueOf(DemoDrawUI.mHPv.getText().toString()));
        RK4_HP_p_X = new highPass(Float.valueOf(DemoDrawUI.mHPp.getText().toString()));
        RK4_HP_a_Y = new highPass(Float.valueOf(DemoDrawUI.mHPa.getText().toString()));
        RK4_HP_v_Y = new highPass(Float.valueOf(DemoDrawUI.mHPv.getText().toString()));
        RK4_HP_p_Y = new highPass(Float.valueOf(DemoDrawUI.mHPp.getText().toString()));
        RK4_staticOFFSET = Float.valueOf(DemoDrawUI.mStaticOffset.getText().toString());
        Eular_avg1 = new Rolling(Integer.parseInt(DemoDrawUI.mMovingAvg.getText().toString()));
        Eular_avg2 = new Rolling(Integer.parseInt(DemoDrawUI.mMovingAvg.getText().toString()));
        Eular_avg3 = new Rolling(Integer.parseInt(DemoDrawUI.mMovingAvg.getText().toString()));
        Eular_LP_a_X = new calLowPass(Float.valueOf(DemoDrawUI.mLP.getText().toString()));
        Eular_LP_a_Y = new calLowPass(Float.valueOf(DemoDrawUI.mLP.getText().toString()));
        Eular_HP_a_X = new highPass(Float.valueOf(DemoDrawUI.mHPa.getText().toString()));
        Eular_HP_v_X = new highPass(Float.valueOf(DemoDrawUI.mHPv.getText().toString()));
        Eular_HP_p_X = new highPass(Float.valueOf(DemoDrawUI.mHPp.getText().toString()));
        Eular_HP_a_Y = new highPass(Float.valueOf(DemoDrawUI.mHPa.getText().toString()));
        Eular_HP_v_Y = new highPass(Float.valueOf(DemoDrawUI.mHPv.getText().toString()));
        Eular_HP_p_Y = new highPass(Float.valueOf(DemoDrawUI.mHPp.getText().toString()));
        Eular_staticOFFSET = Float.valueOf(DemoDrawUI.mStaticOffset.getText().toString());
        stabilize_v2.setcX(Float.valueOf(DemoDrawUI.mMultiplier.getText().toString()));
        stabilize_v2.setcY(Float.valueOf(DemoDrawUI.mMultiplier.getText().toString()));


    }

    public void updateParasauto(float lowpass, float highpass) {
        NSK_LP_a_X = new calLowPass(lowpass);
        //NSK_LP_a_Y = new calLowPass(0.9f);
        NSK_HP_a_X = new highPass(highpass);
        NSK_HP_v_X = new highPass(highpass);
        NSK_HP_p_X = new highPass(highpass);
        NSK_HP_a_Y = new highPass(highpass);
        NSK_HP_v_Y = new highPass(highpass);
        NSK_HP_p_Y = new highPass(highpass);
        RK4_LP_a_X = new calLowPass(lowpass);
        RK4_LP_a_Y = new calLowPass(lowpass);
        RK4_HP_a_X = new highPass(highpass);
        RK4_HP_v_X = new highPass(highpass);
        RK4_HP_p_X = new highPass(highpass);
        RK4_HP_a_Y = new highPass(highpass);
        RK4_HP_v_Y = new highPass(highpass);
        RK4_HP_p_Y = new highPass(highpass);
        Eular_LP_a_X = new calLowPass(lowpass);
        Eular_LP_a_Y = new calLowPass(lowpass);
        Eular_HP_a_X = new highPass(highpass);
        Eular_HP_v_X = new highPass(highpass);
        Eular_HP_p_X = new highPass(highpass);
        Eular_HP_a_Y = new highPass(highpass);
        Eular_HP_v_Y = new highPass(highpass);
        Eular_HP_p_Y = new highPass(highpass);
        mcalEular = new calEular();
        mrk4_X = new RK4();
        mrk4_Y = new RK4();
        RK4_initX = new Position(0, 0);
        RK4_initY = new Position(0, 0);
        /*
        stabilize_v2.setcX(coeff);
        stabilize_v2.setcY(coeff);
        NoShakekal_a = new KalmanFilter();
        NoShakeinit = true;
        prevdx = 0;
        prevdy = 0;
        NSK_avg1 = new Rolling(50);
        NSK_avg2 = new Rolling(50);
        NSK_avg3 = new Rolling(50);
        mBufferX = new CircularBuffer(50, (float) sampleduration);
        mBufferY = new CircularBuffer(50, (float) sampleduration);
        mrk4_X = new RK4();
        mrk4_Y = new RK4();
        RK4_initX = new Position(0, 0);
        RK4_initY = new Position(0, 0);
        RK4kal_a = new KalmanFilter();
        RK4kal_v = new KalmanFilter();
        RK4kal_p = new KalmanFilter();
        RK4_initX.a = 0;
        RK4_initX.v = 0;
        previnitX = 0;
        previnitY = 0;
        RK4_last_timestamp = 0;
        Eular_avg1 = new Rolling(50);
        Eular_avg2 = new Rolling(50);
        Eular_avg3 = new Rolling(50);
        Eularkal_a = new KalmanFilter();
        Eularkal_v = new KalmanFilter();
        Eularkal_p = new KalmanFilter();
        mcalEular = new calEular();
        */
    }

    public void Controller(SensorEvent mSensorEvent) {
        float[][] paramauto = new float[1331][3];
        if (System.currentTimeMillis() - prevautotime > 5000) {
            int num = 0;

            for (int i = 1; i < 3; i++) {
                for (float j = 0; j <= 1.1; j += 0.1) {
                    for (float k = 0; k <= 1.1; k += 0.1) {
                        num++;
                        paramauto[num - 1][0] = i;
                        paramauto[num - 1][1] = j;
                        paramauto[num - 1][2] = k;
                        //modauto = (int) i;
                        Log.d(TAG, "All " + num + " " + i + " " + j + " " + k);
                    }
                }
            }

            /*
            for (int i = 0; i < 3; i++) {
                for (float k = 0.050f; k <= 0.060; k += 0.001) {
                    num++;
                    paramauto[num - 1][0] = i;
                    paramauto[num - 1][1] = k;
                    paramauto[num - 1][2] = k;
                    Log.d(TAG, "All " + num + " " + i + " "  + k);
                }
            }
            */
            /*
            for (float i = 0; i < 5.5; i += 0.1) {
                num++;
                paramauto[num - 1][0] = i;
            }
            */
            modauto = (int) paramauto[nowparam][0];
            Log.d(TAG, "now param " + String.valueOf(paramauto[nowparam][0]) + " " + String.valueOf(paramauto[nowparam][1]) + " " + paramauto[nowparam][2]);
            mdisplay.displaystatus1(String.valueOf(paramauto[nowparam][0]) + " " + String.valueOf(paramauto[nowparam][1]) + " " + String.valueOf(paramauto[nowparam][2]));
            //mdisplay.displaystatus1(String.valueOf(paramauto[num - 1][1]));
            //updateParasauto(paramauto[nowparam][1], paramauto[nowparam][2]);
            nowmultip = paramauto[nowparam][1];
            //stabilize_v2.setcX(paramauto[nowparam][1]);
            //stabilize_v2.setcY(paramauto[nowparam][1]);
            nowparam++;
            prevautotime = System.currentTimeMillis();
        }
        Log.d(TAG, "parameters: " + String.valueOf(pendingUpdate));
        if (pendingUpdate == true) {
            Log.d(TAG, "Parameters");
            updateParas();
            Toast.makeText(mContext, "Parameters Updated " + stabilize_v2.getcX(),
                    Toast.LENGTH_LONG).show();
            pendingUpdate = false;
        }
        //mdisplay.displaystatus1("AppliedPara");
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            //stabilize_v2.setcX(1);
            //stabilize_v2.setcY(1);
            //////////////////////////////init///////////////////////
            if (controllerinit == false) {
                //test
                //stabilize_v2.setcX(0);
                //stabilize_v2.setcY(0);

                String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                String fileName = csvName;
                String filePath = baseDir + File.separator + fileName;
                File f = new File(filePath);
                CSVWriter writer = null;
                // File exist
                if (f.exists()) {
                    f.delete();
                }

                controllerinit = true;
                cirbuff = new ArrayList<sensordata>();
                mdisplay.displaystatus2("Initialized");
                mrotateVector = new rotateVector();
                avg1 = new Rolling(50);
                avg2 = new Rolling(50);
                avg3 = new Rolling(50);

            } else {

                avg1.add(mSensorEvent.values[0]);
                avg2.add(mSensorEvent.values[1]);
                avg3.add(mSensorEvent.values[2]);
                //mtcpipdata.tcpipdatasend(mSensorEvent.values[0]);


                /*
                sensordata thissensordata = new sensordata(System.currentTimeMillis(), new float[]{
                        (float) avg1.getAverage(),
                        (float) avg2.getAverage(),
                        (float) avg3.getAverage()});
*/

                sensordata thissensordata = new sensordata(mSensorEvent.timestamp, mSensorEvent.values);
                /*
                sampleduration = msamplerate.getTimeDelta(thissensordata);
                //Allan
                if (cirbuff.size() < 50) {
                    cirbuff.add(thissensordata);
                } else {
                    cirbuff.add(thissensordata);
                    cirbuff.remove(0);
                    sensordata Allan = mcalAllan.getAvgAllan(cirbuff);
                    LogCSV("Allan", String.valueOf(Allan.getData()[0]), String.valueOf(Allan.getData()[1]), String.valueOf(Allan.getData()[2]), String.valueOf(thissensordata.getTime()), "", " ");
                    Log.d(TAG, "Allan: " + Allan.getData()[0]);
                }
                */
/*
                //vm
                if (AcceCircular.size() < 10) {

                    AcceCircular.add(thissensordata);
                } else {
                    AcceCircular.remove(0);
                    AcceCircular.add(thissensordata);
                }
                //(2)put sample into buffer
                for (int j = 0; j < AcceCircular.size(); j++) {
                    Aintsam = 0;
                    if (Aintsam < Asamplenum) {
                        AcceXsam[Aintsam] = AcceCircular.get(AcceCircular.size() - j - 1).getData()[0];
                        AcceYsam[Aintsam] = AcceCircular.get(AcceCircular.size() - j - 1).getData()[1];
                        AcceZsam[Aintsam] = AcceCircular.get(AcceCircular.size() - j - 1).getData()[2];
                        Aintsam++;
                    }
                }
                float AcceXvar = new Statistics(AcceXsam).getVariance();
                float AcceYvar = new Statistics(AcceYsam).getVariance();
                float AcceZvar = new Statistics(AcceZsam).getVariance();
                Aintsam = 0;
                //(3)put values into tmpbuffer when static, add to LM buffer if size>100
                precaliLogSTATUS = caliLogSTATUS;
                double tmpmagnitude = getVarianceMagnitude(AcceXvar, AcceYvar, AcceZvar);

                if (tmpmagnitude > paramauto[nowparam][0]) {
                    caliLogSTATUS = false;
                    static_start_time = thissensordata.getTime();
                    LogCSV(
                            "Magni",
                            String.valueOf(nowparam),
                            String.valueOf(tmpmagnitude), String.valueOf(1),
                            String.valueOf(thissensordata.getData()[0]),
                            String.valueOf(thissensordata.getData()[1]),
                            String.valueOf(thissensordata.getData()[2]));
                    Log.d(TAG, "MOving");
                } else {
                    if (thissensordata.getTime() - static_start_time > 50) {
                        LogCSV(
                                "Magni",
                                String.valueOf(nowparam),
                                String.valueOf(tmpmagnitude), String.valueOf(2),
                                String.valueOf(thissensordata.getData()[0]),
                                String.valueOf(thissensordata.getData()[1]),
                                String.valueOf(thissensordata.getData()[2]));
                    } else {
                        LogCSV(
                                "Magni",
                                String.valueOf(nowparam),
                                String.valueOf(tmpmagnitude), String.valueOf(1),
                                String.valueOf(thissensordata.getData()[0]),
                                String.valueOf(thissensordata.getData()[1]),
                                String.valueOf(thissensordata.getData()[2]));
                        Log.d(TAG, "MOving");
                    }
                }
*/

                //LogCSV(String.valueOf(thissensordata.getData()[0]),String.valueOf(thissensordata.getData()[1]),String.valueOf(thissensordata.getData()[2]),"","","");

                //NoShake(thissensordata);

                if (tmpgyrodata != null) {
                    //thissensordata = rotateInput(thissensordata);
                    Log.d(TAG, "stop");
                    //thissensordata.setData(new float[]{(float) result[0][0], (float) result[0][1]});
                    //mrotateVector.rotate(new sensordata(mSensorEvent.timestamp, new float[]{mSensorEvent.values[0], mSensorEvent.values[1]}));
                }

                if (selectedMethod == 0 || modauto == 0) {
                    mdisplay.displaystatus2("Method: " + "NoShake");
                    //thissensordata = mrotateVector.rotate(thissensordata);
                    NoShake(thissensordata);
                } else if (selectedMethod == 1 || modauto == 1) {
                    mdisplay.displaystatus2("Method: " + "RK4");
                    //thissensordata = mrotateVector.rotate(thissensordata);
                    RK4(thissensordata);
                } else if (selectedMethod == 2 || modauto == 2) {
                    mdisplay.displaystatus2("Method: " + "Eular");
                    //thissensordata = mrotateVector.rotate(thissensordata);
                    Eular(thissensordata);
                }
            }
        }
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            if (DemoDraw.drawing == 0) {
                mgyrointegration = new gyrointegration();
            }
            sensordata ttmpgyrodata = new sensordata(mSensorEvent.timestamp, mSensorEvent.values);
            mgyrointegration.addgyro(ttmpgyrodata);
            tmpgyrodata = mgyrointegration.getDelta();
        }
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && didCalibraiotn == false) {
            //LogCSV("perfectdata",String.valueOf(mSensorEvent.values[0]),String.valueOf(mSensorEvent.values[1]),String.valueOf(mSensorEvent.values[2]),"","","");
            if (ctrlerCalibrationinit == false) {
                avg1 = new Rolling(50);
                avg2 = new Rolling(50);
                avg3 = new Rolling(50);
                ctrlerCalibrationinit = true;
                mdisplay.displaystatus1("Calibrating...");
            } else {
                sensordata thissensordata = new sensordata(mSensorEvent.timestamp, mSensorEvent.values);
                Calibration(thissensordata);
            }
        }
    }


    public sensordata rotateInput(sensordata msensordata) {
        double[][] rotationArray = {{Math.cos(tmpgyrodata.getData()[0]), -Math.sin(tmpgyrodata.getData()[0])}, {Math.sin(tmpgyrodata.getData()[0]), Math.cos(tmpgyrodata.getData()[0])}};
        double[][] input = new double[1][2];
        input[0][0] = msensordata.getData()[0];
        input[0][1] = msensordata.getData()[1];
        double[][] result = multiplyByMatrix(input, rotationArray);
        msensordata.setData(new float[]{(float) result[0][0], (float) result[0][1]});
        return msensordata;
    }

    public void NoShake(sensordata sensordataIN) {

        sensordata msensordataCALI;
        //if (NoShakeinit == false || DemoDraw.drawing == 0) {
        if (NoShakeinit == false) {
            NoShakekal_a = new KalmanFilter();
            NoShakekal_v = new KalmanFilter();
            NoShakekal_p = new KalmanFilter();
            NoShakeinit = true;
            prevdx = 0;
            prevdy = 0;
            NSK_avg1 = new Rolling(50);
            NSK_avg2 = new Rolling(50);
            NSK_avg3 = new Rolling(50);
            NSK_LP_a_X = new calLowPass(0.9f);
            //NSK_LP_a_Y = new calLowPass(0.9f);
            NSK_HP_a_X = new highPass(0.99f);
            NSK_HP_v_X = new highPass(0.99f);
            NSK_HP_p_X = new highPass(0.99f);
            NSK_HP_a_Y = new highPass(0.99f);
            NSK_HP_v_Y = new highPass(0.99f);
            NSK_HP_p_Y = new highPass(0.99f);
            NSK_Pos = new float[]{0, 0, 0};
            //mBufferX = new CircularBuffer(50, (float) sampleduration);
            //mBufferY = new CircularBuffer(50, (float) sampleduration);
            try {
                updateParas();
            } catch (Exception ex) {

            }
            NSK_last_timestamp = System.currentTimeMillis();
        } else {
            /*
            float NS2S = 1.0f / 1000000000.0f;
            float dt = (sensordataIN.getTime() - NSK_last_timestamp) * NS2S;
            NSK_last_timestamp = sensordataIN.getTime();

            Log.d(TAG, "kanmanNSK -3 " + sensordataIN.getData()[0]);
            //lowpass
            float[] tmpnk = NSK_LP_a_X.lowPass(sensordataIN.getData());
            sensordataIN.setData(tmpnk);
            Log.d(TAG, "kanmanNSK -2 " + sensordataIN.getData()[0]);
            //mavg+kalman
            NSK_avg1.add(sensordataIN.getData()[0]);
            NSK_avg2.add(sensordataIN.getData()[1]);
            Log.d(TAG, "kanmanNSK -1 " + NSK_avg1.getAverage());
            sensordataIN.setData(NoShakekal_a.calKalman(new float[]{(float) NSK_avg1.getAverage(), (float) NSK_avg2.getAverage(), 0}));
            Log.d(TAG, "kanmanNSK 0 " + sensordataIN.getData()[0]);
            //highpass2a
            sensordataIN.setData(new float[]{
                    NSK_HP_a_X.calhighPass(sensordataIN.getData()[0]),
                    NSK_HP_a_Y.calhighPass(sensordataIN.getData()[1])});
                    */
            mBufferX.insert(sensordataIN.getData()[0]);
            mBufferY.insert(sensordataIN.getData()[1]);
            final float dx = mBufferX.convolveWithH();
            final float dy = mBufferY.convolveWithH();
            /*
            sensordata sensordataOUT = new sensordata(sensordataIN.getTime(), new float[]{dx, dy, dy});
            Log.d(TAG, "kanmanNSK 1 " + dx);
            //kalman2v
            sensordataOUT.setData(NoShakekal_v.calKalman(new float[]{dx, dy, dy}));
            Log.d(TAG, "kanmanNSK 2 " + sensordataOUT.getData()[0]);
            //highpass_v
            sensordataOUT.setData(new float[]{
                    NSK_HP_a_X.calhighPass(sensordataOUT.getData()[0]),
                    NSK_HP_a_Y.calhighPass(sensordataOUT.getData()[1])});

            Log.d(TAG, "kanmanNSK 3 " + sensordataOUT.getData()[0]);
            //NSK_Pos
            NSK_Pos[0] += sensordataOUT.getData()[0] * dt;
            NSK_Pos[1] += sensordataOUT.getData()[1] * dt;
            Log.d(TAG, "kanmanNSK 4 " + NSK_Pos[0]);
            //NSK_Pos_kalman
            NSK_Pos = NoShakekal_p.calKalman(NSK_Pos);
            Log.d(TAG, "kanmanNSK 5 " + NSK_Pos[0]);
            //highpass_p
            NSK_Pos[0] = NSK_HP_p_X.calhighPass(NSK_Pos[0]);
            NSK_Pos[1] = NSK_HP_p_Y.calhighPass(NSK_Pos[1]);
            Log.d(TAG, "kanmanNSK 6 " + NSK_Pos[0] + " " + dt);
*/
            Log.d(TAG, "Noshake " + String.valueOf(sensordataIN.getData()[0]) + " " + String.valueOf(dx));
            if (NSK_Pos[0] == 0) {
                Log.d(TAG, "FUCK");
            }
            //LogCSV("Noshake", String.valueOf(dx), String.valueOf(dy), "", "", "", "");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //mtcpipdata.tcpipdatasend((float)dx,(float)dy);
                }
            }).start();
            Message msg = new Message();
            Bundle bundle = new Bundle();
            msg.arg1 = 2;
            bundle.putFloatArray("Acce", new float[]{dx, dy});
            bundle.putLong("Time", sensordataIN.getTime());
            bundle.putFloatArray("Pos", NSK_Pos);
            msg.setData(bundle);
            stabilize_v2.getSensor.sendMessage(msg);
        }


    }

    public void RK4(sensordata sensordataIN) {
        if (RK4init == false || DemoDraw.drawing == 0) {
            mrk4_X = new RK4();
            mrk4_Y = new RK4();
            RK4_initX = new Position(0, 0);
            RK4_initY = new Position(0, 0);
            RK4_avg1 = new Rolling(70);
            RK4_avg2 = new Rolling(70);
            RK4_avg3 = new Rolling(70);
            RK4_LP_a_X = new calLowPass(0.9f);
            RK4_LP_a_Y = new calLowPass(0.9f);
            RK4_HP_a_X = new highPass(0.99f);
            RK4_HP_v_X = new highPass(0.99f);
            RK4_HP_p_X = new highPass(0.99f);
            RK4_HP_a_Y = new highPass(0.99f);
            RK4_HP_v_Y = new highPass(0.99f);
            RK4_HP_p_Y = new highPass(0.99f);
            RK4kal_a = new KalmanFilter();
            RK4kal_v = new KalmanFilter();
            RK4kal_p = new KalmanFilter();
            RK4_initX.a = 0;
            RK4_initX.v = 0;
            RK4init = true;
            previnitX = 0;
            previnitY = 0;
            RK4_staticOFFSET = 0.009f;
            RK4_last_timestamp = 0;
            try {
                updateParas();
            } catch (Exception ex) {

            }

        }
        float NS2S = 1.0f / 1000000000.0f;
        float dt = (sensordataIN.getTime() - RK4_last_timestamp) * NS2S;
        RK4_last_timestamp = sensordataIN.getTime();
        //X
        previnitX = RK4_initX.pos;
        previnitY = RK4_initY.pos;


        //lowpass
        float[] applylowpass = RK4_LP_a_X.lowPass(sensordataIN.getData());
        sensordataIN.setData(applylowpass);

        //moving average
        RK4_avg1.add((double) sensordataIN.getData()[0]);
        RK4_avg2.add((double) sensordataIN.getData()[1]);
        RK4_avg3.add((double) sensordataIN.getData()[2]);
        sensordataIN.setData(new float[]{(float) RK4_avg1.getAverage(), (float) RK4_avg2.getAverage(), (float) RK4_avg3.getAverage()});

        //0 if static
        if (detectStatic(sensordataIN)) {
            RK4_initY.v = 0;
            RK4_initX.v = 0;
        }

        //KF
        sensordata sensordataOUT_a = new sensordata(sensordataIN.getTime(), RK4kal_a.calKalman(sensordataIN.getData()));
        sensordata sensordataOUT_v = new sensordata(sensordataIN.getTime(), RK4kal_v.calKalman(new float[]{(float) RK4_initX.v, (float) RK4_initY.v, (float) RK4_initY.v}));
        sensordata sensordataOUT = new sensordata(sensordataIN.getTime(), RK4kal_p.calKalman(new float[]{((float) RK4_initX.pos), ((float) RK4_initY.pos), ((float) RK4_initY.pos)}));
        RK4_initX.v = sensordataOUT_v.getData()[0];
        RK4_initY.v = sensordataOUT_v.getData()[1];
        RK4_initX.a = sensordataOUT_a.getData()[0];
        RK4_initY.a = sensordataOUT_a.getData()[1];

        //apply highpass
        RK4_initX.v = RK4_HP_v_X.calhighPass((float) RK4_initX.v);
        RK4_initY.v = RK4_HP_v_Y.calhighPass((float) RK4_initY.v);
        RK4_initX.pos = RK4_HP_p_X.calhighPass((float) RK4_initX.pos);
        RK4_initY.pos = RK4_HP_p_Y.calhighPass((float) RK4_initY.pos);


        mrk4_X.integrate(RK4_initX, sensordataIN.getTime(), 0.01, sensordataIN.getData()[0]);
        mrk4_Y.integrate(RK4_initY, sensordataIN.getTime(), 0.01, sensordataIN.getData()[1]);
        if (DemoDraw.drawing < 2) {
            //LogCSV("RK4", String.valueOf(sensordataIN.getTime()), String.valueOf(RK4_initX.pos), String.valueOf(RK4_initY.pos), String.valueOf(RK4_initX.v), String.valueOf(RK4_initX.v), "");
        }


        //KF

        //sensordata sensordataOUT_a = new sensordata(sensordataIN.getTime(), RK4kal_a.calKalman(new float[]{(float) RK4_initX.a, (float) RK4_initY.a}));
        //sensordata sensordataOUT_v = new sensordata(sensordataIN.getTime(), RK4kal_v.calKalman(new float[]{(float) RK4_initX.v, (float) RK4_initY.v}));

        sensordata sensordataOUT_p = new sensordata(sensordataIN.getTime(), new float[]{(float) (RK4_initX.pos), (float) (RK4_initY.pos)});
        sensordata sensordataOUT_d = new sensordata(sensordataIN.getTime(), new float[]{(float) (RK4_initX.pos - previnitX), (float) (RK4_initY.pos - previnitY)});
        //sensordata sensordataOUT_p = new sensordata(sensordataIN.getTime(), new float[]{(float) (RK4_initX.pos), (float) (RK4_initY.pos)});
        previnitX = RK4_initX.pos;
        previnitY = RK4_initY.pos;

        new Thread(new Runnable() {
            @Override
            public void run() {
                //mtcpipdata.tcpipdatasend((float)RK4_initX.pos,(float)RK4_initY.pos);
            }
        }).start();

        //LogCSV("RK4_n", String.valueOf(sensordataOUT_p.getData()[0]), String.valueOf(sensordataOUT_p.getData()[1]), String.valueOf(sensordataOUT_p.getData()[2]), "", "", "");
        Log.d(TAG, "forlogging1 " + String.valueOf(sensordataIN.getData()[0]) + " " + String.valueOf(RK4_initX.pos) + " " + String.valueOf(RK4_initY.pos) + " " + String.valueOf(mrk4_X.getDeltaPos()) + " " + String.valueOf(mrk4_Y.getDeltaPos()));
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putFloatArray("Acce", sensordataOUT_d.getData());
        bundle.putFloatArray("Pos", new float[]{(float) RK4_initX.pos, (float) RK4_initY.pos});
        Log.d(TAG, "thisone p " + RK4_initX.pos);
        bundle.putLong("Time", sensordataOUT_p.getTime());
        msg.setData(bundle);
        msg.arg1 = 2;

        //proDataFlow.AcceHandler.sendMessage(msg);
        stabilize_v2.getSensor.sendMessage(msg);
    }

    public void Eular(sensordata sensordataIN) {
        if (Eularinit == false || DemoDraw.drawing == 0) {
            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            String fileName = "Eular.csv";
            String filePath = baseDir + File.separator + fileName;
            File f = new File(filePath);
            CSVWriter writer = null;
            // File exist
            if (f.exists()) {
                f.delete();
            }
            Eular_avg1 = new Rolling(50);
            Eular_avg2 = new Rolling(50);
            Eular_avg3 = new Rolling(50);
            Eular_LP_a_X = new calLowPass(0.5f);
            Eular_LP_a_Y = new calLowPass(0.5f);
            Eular_HP_a_X = new highPass(0.1f);
            Eular_HP_v_X = new highPass(0.1f);
            Eular_HP_p_X = new highPass(0.1f);
            Eular_HP_a_Y = new highPass(0.1f);
            Eular_HP_v_Y = new highPass(0.1f);
            Eular_HP_p_Y = new highPass(0.1f);
            Eularkal_a = new KalmanFilter();
            Eularkal_v = new KalmanFilter();
            Eularkal_p = new KalmanFilter();
            mcalEular = new calEular();
            prevEular = sensordataIN.getData();
            Eular_sensordataOUT = new sensordata();
            Eularinit = true;
            Eular_staticOFFSET = 0.014f;
            int staticnum = 0;

            try {
                updateParas();
            } catch (Exception ex) {

            }
        }
        Eular_sensordataOUT = sensordataIN;
        //init
        if (mcalEular.position == null) {
            mcalEular.calc(Eular_sensordataOUT);
        }
        if (DemoDraw.drawing > 1) {
            for (int i = 0; i < mcalEular.position.length; i++) {
                //mcalEular.position[i] = 0;
            }
        }

        Log.d(TAG, "Euler " + String.valueOf(mcalEular.position[0]) + " " + Eular_sensordataOUT.getData()[0]);

        //lowpass
        float[] applylowpass = Eular_LP_a_X.lowPass(Eular_sensordataOUT.getData());
        Eular_sensordataOUT.setData(applylowpass);

        //moving average
        Eular_avg1.add((double) Eular_sensordataOUT.getData()[0]);
        Eular_avg2.add((double) Eular_sensordataOUT.getData()[1]);
        Eular_avg3.add((double) Eular_sensordataOUT.getData()[2]);
        Eular_sensordataOUT.setData(new float[]{(float) Eular_avg1.getAverage(), (float) Eular_avg2.getAverage(), (float) Eular_avg3.getAverage()});


        //set 0 if static
        if (Math.abs(Eular_sensordataOUT.getData()[0]) < Eular_staticOFFSET && Math.abs(Eular_sensordataOUT.getData()[1]) < Eular_staticOFFSET && staticnum > 50) {
            mcalEular.velocity[0] = 0;
            //mcalEular.position[0] = 0;
            mcalEular.velocity[1] = 0;
            //mcalEular.position[1] = 0;
        } else if (Math.abs(Eular_sensordataOUT.getData()[0]) < Eular_staticOFFSET && Math.abs(Eular_sensordataOUT.getData()[1]) < Eular_staticOFFSET) {
            staticnum++;
        } else {
            staticnum = 0;
        }

        //apply highpass
        mcalEular.velocity[0] = Eular_HP_v_X.calhighPass(mcalEular.velocity[0]);
        mcalEular.velocity[1] = Eular_HP_v_Y.calhighPass(mcalEular.velocity[1]);
        mcalEular.position[0] = Eular_HP_p_X.calhighPass(mcalEular.position[0]);
        mcalEular.position[1] = Eular_HP_p_X.calhighPass(mcalEular.position[1]);


        //KF
        //sensordata sensordataOUT_a = new sensordata(sensordataIN.getTime(), Eularkal_a.calKalman(sensordataIN.getData()));
        //sensordata sensordataOUT_v = new sensordata(sensordataIN.getTime(), Eularkal_v.calKalman(mcalEular.velocity));
        //Eular_sensordataOUT = new sensordata(Eular_sensordataOUT.getTime(), Eularkal_p.calKalman(mcalEular.position));

        //calculate
        mcalEular.calc(Eular_sensordataOUT);

        //getDelta
        //Eular_sensordataOUT = new sensordata(Eular_sensordataOUT.getTime(), new float[]{Eular_sensordataOUT.getData()[0] - prevEular[0], Eular_sensordataOUT.getData()[1] - prevEular[1]});
        prevEular = Eular_sensordataOUT.getData();

        new Thread(new Runnable() {
            @Override
            public void run() {
                //mtcpipdata.tcpipdatasend(mcalEular.position[0],mcalEular.position[1]);
            }
        }).start();


        Log.d(TAG, "forlogging " + String.valueOf(mcalEular.position[0]) + " " + String.valueOf(mcalEular.position[1]));
        if (DemoDraw.drawing < 2) {
            //LogCSV("Eular.csv", String.valueOf(Eular_sensordataOUT.getTime()), String.valueOf(mcalEular.position[0]), String.valueOf(mcalEular.position[1]), String.valueOf(mcalEular.velocity[0]), String.valueOf(mcalEular.velocity[0]), String.valueOf(mcalEular.velocity[1]));
        }
        //mtcpipdata.tcpipdatasend(mcalEular.position[0]);
        sensordata sensordataOUT = new sensordata(sensordataIN.getTime(), new float[]{mcalEular.position[0] - prevEular[0], mcalEular.position[1] - prevEular[1]});
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putFloatArray("Acce", sensordataOUT.getData());
        bundle.putFloatArray("Pos", mcalEular.position);
        bundle.putLong("Time", Eular_sensordataOUT.getTime());
        msg.setData(bundle);
        msg.arg1 = 2;
        //stabilize_v2.setcX(-10000);
        //stabilize_v2.setcY(-10000);
        //proDataFlow.AcceHandler.sendMessage(msg);
        stabilize_v2.getSensor.sendMessage(msg);


    }

    public void Calibration(sensordata sensordataIN) {
        if (Calibrationinit == false) {
            rawAcceAll = new ArrayList<sensordata>();
            AcceCircular = new ArrayList<sensordata>();
            HistoryLength = 100;
            Asamplenum = 50;
            AcceXsam = new float[Asamplenum];
            AcceYsam = new float[Asamplenum];
            AcceZsam = new float[Asamplenum];
            Calibrationinit = true;
            caliAccebuffer = new ArrayList<ArrayList<sensordata>>();
            long static_start_time = sensordataIN.getTime();
        } else {
            rawAcceAll.add(sensordataIN);
            Log.d(TAG, "TESTEWST");
            //(1)put into buffer
            if (AcceCircular.size() < 10) {
                AcceCircular.add(sensordataIN);
            } else {
                AcceCircular.remove(0);
                AcceCircular.add(sensordataIN);
            }
            //(2)put sample into buffer
            for (int j = 0; j < AcceCircular.size(); j++) {
                Aintsam = 0;
                if (Aintsam < Asamplenum) {
                    AcceXsam[Aintsam] = AcceCircular.get(AcceCircular.size() - j - 1).getData()[0];
                    AcceYsam[Aintsam] = AcceCircular.get(AcceCircular.size() - j - 1).getData()[1];
                    AcceZsam[Aintsam] = AcceCircular.get(AcceCircular.size() - j - 1).getData()[2];
                    Aintsam++;
                }
            }
            float AcceXvar = new Statistics(AcceXsam).getVariance();
            float AcceYvar = new Statistics(AcceYsam).getVariance();
            float AcceZvar = new Statistics(AcceZsam).getVariance();
            Aintsam = 0;

            //(3)put values into tmpbuffer when static, add to LM buffer if size>100
            precaliLogSTATUS = caliLogSTATUS;

            double tmpmagnitude = getVarianceMagnitude(AcceXvar, AcceYvar, AcceZvar);
            Log.d(TAG, "getvar: " + tmpmagnitude);
            float staticthreshold = 1.9f;
            //mdisplay.displaystatus1("num: " + String.valueOf(7 - caliAccebuffer.size()) + " buff: " + tmpcalibuffer.size() + " static: " + String.valueOf((1.865 - tmpmagnitude) * 100 / 1.865) + "%");


            //if (tmpmagnitude < 1.865) {
            //    caliLogSTATUS = true;
            //    if (tmpcalibuffer.size() < 100) {
            //        tmpcalibuffer.add(sensordataIN);
            //    }
            //} else {
            //    caliLogSTATUS = false;
            //}
            //LogCSV("screenup1",String.valueOf(tmpmagnitude),"","","","","");
            if (tmpmagnitude > staticthreshold) {
                caliLogSTATUS = false;
                static_start_time = System.currentTimeMillis();
                caliLogSTATUS = false;
                /*
                LogCSV(
                        "Magni",
                        String.valueOf(sensordataIN.getTime()),
                        String.valueOf(tmpmagnitude), String.valueOf(1),
                        String.valueOf(sensordataIN.getData()[0]),
                        String.valueOf(sensordataIN.getData()[1]),
                        String.valueOf(sensordataIN.getData()[2]));
                        */
                Log.d(TAG, "MOving");
            } else {
                if (System.currentTimeMillis() - static_start_time > 500) {
                    caliLogSTATUS = true;
                    if (tmpcalibuffer.size() < 100) {
                        tmpcalibuffer.add(sensordataIN);
                    }
                    /*
                    LogCSV(
                            "Magni",
                            String.valueOf(sensordataIN.getTime()),
                            String.valueOf(tmpmagnitude), String.valueOf(2),
                            String.valueOf(sensordataIN.getData()[0]),
                            String.valueOf(sensordataIN.getData()[1]),
                            String.valueOf(sensordataIN.getData()[2]));
                            */
                } else {
                    caliLogSTATUS = false;
                    /*
                    LogCSV(
                            "Magni",
                            String.valueOf(sensordataIN.getTime()),
                            String.valueOf(tmpmagnitude), String.valueOf(1),
                            String.valueOf(sensordataIN.getData()[0]),
                            String.valueOf(sensordataIN.getData()[1]),
                            String.valueOf(sensordataIN.getData()[2]));
                    Log.d(TAG, "MOving");
                    */
                }
            }
            //mdisplay.displaystatus1("num: " + String.valueOf(7 - caliAccebuffer.size()) + " buff: " + tmpcalibuffer.size() + " static: " + String.valueOf((staticthreshold - tmpmagnitude) * 100 / staticthreshold) + "%");
            mdisplay.displaystatus1("num: " + String.valueOf(7 - caliAccebuffer.size()) + " buff: " + tmpcalibuffer.size() + " static: " + String.valueOf(tmpmagnitude));
            //(4)if status change & num>1-- =>throw LM
            if (precaliLogSTATUS == true && caliLogSTATUS == false && tmpcalibuffer.size() > 90) {
                //caliAccebuffer.add(mkArrayAverage(tmpcalibuffer));
                caliAccebuffer.add(tmpcalibuffer);
                Log.d(TAG, "LM: cali " + caliAccebuffer.size() + " tmp:" + tmpcalibuffer.size());
                tmpcalibuffer = new ArrayList<>();
            }
            Log.d(TAG, "LM: cali caliLogSTATUS" + caliLogSTATUS);
            Log.d(TAG, "LM: cali " + caliAccebuffer.size() + " tmp:" + tmpcalibuffer.size());
            //(5)compute LM
            // if (caliAccebuffer.size() > 10 && Calibrated == false) {

            if (caliAccebuffer.size() > 6) {
                mdisplay.displaystatus1("LM: Start");
                //flatten all data
                ArrayList<sensordata> allcaliAccebuffer = new ArrayList<>();
                for (int j = 0; j < caliAccebuffer.size(); j++) {
                    for (int k = 0; k < caliAccebuffer.get(j).size(); k++) {
                        allcaliAccebuffer.add(caliAccebuffer.get(j).get(k));
                        Log.d(TAG, "LM: flattening " + String.valueOf(caliAccebuffer.get(j).get(k).getData()[0]) + " " + String.valueOf(caliAccebuffer.get(j).get(k).getData()[1]) + " " + String.valueOf(caliAccebuffer.get(j).get(k).getData()[2]));
                        mdisplay.displaystatus1("LM: flattening " + String.valueOf(caliAccebuffer.get(j).get(k).getData()[0]) + " " + String.valueOf(caliAccebuffer.get(j).get(k).getData()[1]) + " " + String.valueOf(caliAccebuffer.get(j).get(k).getData()[2]));
                    }
                }
                //cal LM
                LM(mLM, allcaliAccebuffer);
                //disp result

                Log.d(TAG, "LM: cost b/a: " + String.valueOf(mLM.getInitialCost()) + " " + mLM.getFinalCost());
                for (int l = 0; l < 6; l++) {
                    Log.d(TAG, "LM: parm " + String.valueOf(mLM.getParameters().get(l, 0)));
                }
                mdisplay.displaystatus1("LM: parm: " +
                                String.valueOf(Math.floor(mLM.getParameters().get(0, 0))) + " " +
                                String.valueOf(Math.floor(mLM.getParameters().get(1, 0))) + " " +
                                String.valueOf(Math.floor(mLM.getParameters().get(2, 0))) + " " +
                                String.valueOf(Math.floor(mLM.getParameters().get(3, 0))) + " " +
                                String.valueOf(Math.floor(mLM.getParameters().get(4, 0))) + " " +
                                String.valueOf(Math.floor(mLM.getParameters().get(5, 0))) + " "
                        //String.valueOf(Math.floor(mLM.getParameters().get(0, 0))) + " "
                );
                paramgot = mLM.getParameters();
                didCalibraiotn = true;
            }
        }
    }


    public boolean detectStatic(sensordata msensordata) {
        //(1)put into buffer
        if (lAcceCircular.size() < 10) {
            lAcceCircular.add(msensordata);
        } else {
            lAcceCircular.remove(0);
            lAcceCircular.add(msensordata);
        }

        //(2)put static into buffer
        for (int j = 0; j < lAcceCircular.size(); j++) {
            int lAintsam = 0;
            if (Aintsam < Asamplenum) {
                lAcceXsam[Aintsam] = lAcceCircular.get(lAcceCircular.size() - j - 1).getData()[0];
                lAcceYsam[Aintsam] = lAcceCircular.get(lAcceCircular.size() - j - 1).getData()[1];
                lAcceZsam[Aintsam] = lAcceCircular.get(lAcceCircular.size() - j - 1).getData()[2];
                lAintsam++;
            }
        }
        float lAcceXvar = new Statistics(AcceXsam).getVariance();
        float lAcceYvar = new Statistics(AcceYsam).getVariance();
        float lAcceZvar = new Statistics(AcceZsam).getVariance();
        Aintsam = 0;

        //(3)put values into tmpbuffer when static, add to LM buffer if size>100
        precaliLogSTATUS = caliLogSTATUS;
        getcaliLogSTATUS = getVarianceMagnitude(lAcceXvar, lAcceYvar, lAcceZvar) < 1.95;
        return getcaliLogSTATUS;
    }

    protected long getEventTimestampInMills(SensorEvent event) {
        long timestamp = event.timestamp / 1000;

        /**
         * work around the problem that in some devices event.timestamp is
         * actually returns nano seconds since last boot.
         */
        if (System.currentTimeMillis() - timestamp >
                TimeUnit.DAYS.toMillis(1) * 2) {
            /**
             * if we getting from the original event timestamp a value that does
             * not make sense(it is very very not unlikely that will be batched
             * events of two days..) then assume that the event time is actually
             * nano seconds since boot
             */
            timestamp = System.currentTimeMillis()
                    + (event.timestamp - System.nanoTime()) / 1000000L;
        }
        return timestamp;
    }

    private void LogEverything(ArrayList<sensordata> rawAcceAll, ArrayList<sensordata> caliAccebuffer) {

        for (sensordata rawdata : rawAcceAll) {
            Log.d(TAG, "LM:LoggingEverything");
            //LogCSV(String.valueOf(rawdata.getData()[0]), String.valueOf(rawdata.getData()[1]), String.valueOf(rawdata.getData()[2]), "", "", "");
        }

    }

    private ArrayList<sensordata> mkArrayAverage(ArrayList<sensordata> tmpcalibuffer) {

        float[] data = new float[3];
        for (sensordata iarray : tmpcalibuffer) {
            data[0] += iarray.getData()[0];
            data[1] += iarray.getData()[1];
            data[2] += iarray.getData()[2];
        }
        data[0] /= tmpcalibuffer.size();
        data[1] /= tmpcalibuffer.size();
        data[2] /= tmpcalibuffer.size();
        ArrayList<sensordata> avgArray = new ArrayList<sensordata>();
        avgArray.add(new sensordata(tmpcalibuffer.get(0).getTime(), data));
        return avgArray;
    }

    public void LogCSV(String Name, String a, String b, String c, String d, String g, String h) {
        //init CSV logging
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        //String fileName = csvName;
        String fileName = Name + ".csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer = null;
        // File exist
        if (f.exists() && !f.isDirectory()) {
            try {
                mFileWriter = new FileWriter(filePath, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer = new CSVWriter(mFileWriter);
        } else {
            try {
                writer = new CSVWriter(new FileWriter(filePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            String line = String.format("%s,%s,%s,%s,%s,%s\n", a, b, c, d, g, h);
            mFileWriter.write(line);
        } catch (Exception ex) {
        }
        /*
        catch (IOException e) {
            e.printStackTrace();
        }
        */

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double acceCali(DenseMatrix64F x, DenseMatrix64F param) {
        for (int i = 0; i < param.getNumElements(); i++) {
            Log.d(TAG, "testprint " + String.valueOf(param.get(i, 0)) + " " + param.getNumElements());
        }
        double returey1;
        /* with misalignment
        double[][] rawMatrix = {{x.get(0, 0), x.get(1, 0), x.get(2, 0)}};
        double[][] biasMatrix = {{param.get(6, 0), param.get(7, 0), param.get(8, 0)}};
        double[][] scaleMatrix =
                {{param.get(3, 0), 0, 0},
                        {0, param.get(4, 0), 0},
                        {0, 0, param.get(5, 0)}};
        double[][] nonorMatrix =
                {{1, -param.get(0, 0), param.get(1, 0)},
                        {0, 1, -param.get(2, 0)},
                        {0, 0, 1}};
        Matrix mrawMatrix = new Matrix(rawMatrix);
        Matrix mbiasMatrix = new Matrix(biasMatrix);
        Matrix mscaleMatrix = new Matrix(scaleMatrix);
        Matrix mnonorMatrix = new Matrix(nonorMatrix);

        Log.d(TAG, "getdimen " + mrawMatrix.getRowDimension() + "x" + mrawMatrix.getColumnDimension());  //1x3
        Log.d(TAG, "getdimen " + mbiasMatrix.getRowDimension() + "x" + mbiasMatrix.getColumnDimension());//1x3
        Log.d(TAG, "getdimen " + mscaleMatrix.getRowDimension() + "x" + mscaleMatrix.getColumnDimension());//3x3
        Log.d(TAG, "getdimen " + mnonorMatrix.getRowDimension() + "x" + mnonorMatrix.getRowDimension());//3x3

        Matrix k = mrawMatrix.plus(mbiasMatrix); //1x3
        Matrix m = mnonorMatrix.times(mscaleMatrix); //3x3
        Log.d(TAG, "getdimen " + k.getRowDimension() + "x" + k.getColumnDimension());
        Log.d(TAG, "getdimen " + m.getRowDimension() + "x" + m.getColumnDimension());
        Matrix result = m.times(k.transpose());
        Log.d(TAG, "getdimen " + result.getRowDimension() + "x" + result.getColumnDimension());
        //Log.d(TAG, "LM: getvarm " + result.get(0, 0) + " " + result.get(1, 0) + " " + result.get(2, 0));
        */
        double[][] rawMatrix = {{x.get(0, 0), x.get(1, 0), x.get(2, 0)}};
        double[][] biasMatrix = {{param.get(3, 0), param.get(4, 0), param.get(5, 0)}};
        double[][] scaleMatrix =
                {{param.get(0, 0), 0, 0},
                        {0, param.get(1, 0), 0},
                        {0, 0, param.get(2, 0)}};
        Matrix mrawMatrix = new Matrix(rawMatrix);
        Matrix mbiasMatrix = new Matrix(biasMatrix);
        Matrix mscaleMatrix = new Matrix(scaleMatrix);

        Log.d(TAG, "getdimen " + mrawMatrix.getRowDimension() + "x" + mrawMatrix.getColumnDimension());  //1x3
        Log.d(TAG, "getdimen " + mbiasMatrix.getRowDimension() + "x" + mbiasMatrix.getColumnDimension());//1x3
        Log.d(TAG, "getdimen " + mscaleMatrix.getRowDimension() + "x" + mscaleMatrix.getColumnDimension());//3x3

        Matrix k = mrawMatrix.plus(mbiasMatrix); //1x3
        Matrix m = mscaleMatrix; //3x3
        Log.d(TAG, "getdimen " + k.getRowDimension() + "x" + k.getColumnDimension());
        Log.d(TAG, "getdimen " + m.getRowDimension() + "x" + m.getColumnDimension());
        Matrix result = m.times(k.transpose());
        Log.d(TAG, "getdimen " + result.getRowDimension() + "x" + result.getColumnDimension());
        //Log.d(TAG, "LM: getvarm " + result.get(0, 0) + " " + result.get(1, 0) + " " + result.get(2, 0));
        returey1 = (double) getVarianceMagnitude((float) result.get(0, 0), (float) result.get(1, 0), (float) result.get(2, 0));
        Log.d(TAG, "returnoutput: " + returey1);
        return returey1;
    }

    public sensordata RawAcceCali(sensordata msensordata, DenseMatrix64F param) {
        sensordata calimsensordata = new sensordata();
        /*with misalignment
        double[][] rawMatrix = {{msensordata.getData()[0], msensordata.getData()[1], msensordata.getData()[2]}};
        double[][] biasMatrix = {{param.get(6, 0), param.get(7, 0), param.get(8, 0)}};
        double[][] scaleMatrix =
                {{param.get(3, 0), 0, 0},
                        {0, param.get(4, 0), 0},
                        {0, 0, param.get(5, 0)}};
        double[][] nonorMatrix =
                {{1, -param.get(0, 0), param.get(1, 0)},
                        {0, 1, -param.get(2, 0)},
                        {0, 0, 1}};
        Matrix mrawMatrix = new Matrix(rawMatrix);
        Matrix mbiasMatrix = new Matrix(biasMatrix);
        Matrix mscaleMatrix = new Matrix(scaleMatrix);
        Matrix mnonorMatrix = new Matrix(nonorMatrix);

        Log.d(TAG, "getdimen " + mrawMatrix.getRowDimension() + "x" + mrawMatrix.getColumnDimension());  //1x3
        Log.d(TAG, "getdimen " + mbiasMatrix.getRowDimension() + "x" + mbiasMatrix.getColumnDimension());//1x3
        Log.d(TAG, "getdimen " + mscaleMatrix.getRowDimension() + "x" + mscaleMatrix.getColumnDimension());//3x3
        Log.d(TAG, "getdimen " + mnonorMatrix.getRowDimension() + "x" + mnonorMatrix.getRowDimension());//3x3

        Matrix k = mrawMatrix.plus(mbiasMatrix); //1x3
        Matrix m = mnonorMatrix.times(mscaleMatrix); //3x3
        Log.d(TAG, "getdimen " + k.getRowDimension() + "x" + k.getColumnDimension());
        Log.d(TAG, "getdimen " + m.getRowDimension() + "x" + m.getColumnDimension());
        Matrix result = m.times(k.transpose());
        Log.d(TAG, "getdimen " + result.getRowDimension() + "x" + result.getColumnDimension());
        //Log.d(TAG, "LM: getvarm " + result.get(0, 0) + " " + result.get(1, 0) + " " + result.get(2, 0));
*/
        double[][] rawMatrix = {{msensordata.getData()[0], msensordata.getData()[1], msensordata.getData()[2]}};
        double[][] biasMatrix = {{param.get(3, 0), param.get(4, 0), param.get(5, 0)}};
        double[][] scaleMatrix =
                {{param.get(0, 0), 0, 0},
                        {0, param.get(1, 0), 0},
                        {0, 0, param.get(2, 0)}};
        Matrix mrawMatrix = new Matrix(rawMatrix);
        Matrix mbiasMatrix = new Matrix(biasMatrix);
        Matrix mscaleMatrix = new Matrix(scaleMatrix);

        Log.d(TAG, "getdimen " + mrawMatrix.getRowDimension() + "x" + mrawMatrix.getColumnDimension());  //1x3
        Log.d(TAG, "getdimen " + mbiasMatrix.getRowDimension() + "x" + mbiasMatrix.getColumnDimension());//1x3
        Log.d(TAG, "getdimen " + mscaleMatrix.getRowDimension() + "x" + mscaleMatrix.getColumnDimension());//3x3

        Matrix k = mrawMatrix.plus(mbiasMatrix); //1x3
        Matrix m = mscaleMatrix; //3x3
        Log.d(TAG, "getdimen " + k.getRowDimension() + "x" + k.getColumnDimension());
        Log.d(TAG, "getdimen " + m.getRowDimension() + "x" + m.getColumnDimension());
        Matrix result = m.times(k.transpose());
        Log.d(TAG, "getdimen " + result.getRowDimension() + "x" + result.getColumnDimension());
        //Log.d(TAG, "LM: getvarm " + result.get(0, 0) + " " + result.get(1, 0) + " " + result.get(2, 0));
        float[] data = new float[3];
        data[0] = (float) result.get(0, 0);
        data[1] = (float) result.get(1, 0);
        data[2] = (float) result.get(2, 0);

        calimsensordata.setTime(msensordata.Time);
        calimsensordata.setData(data);
        return calimsensordata;
    }

    public void LM(LevenbergMarquardt tLM, ArrayList<sensordata> allcaliAccebuffer) {
        //Calibration init
        ///理想error model值
        /*with misalignment
        DenseMatrix64F mpara = new DenseMatrix64F(9, 1);
        mpara.set(0, 0, 0);
        mpara.set(1, 0, 0);
        mpara.set(2, 0, 0);
        mpara.set(3, 0, 1);
        mpara.set(4, 0, 1);
        mpara.set(5, 0, 1);
        mpara.set(6, 0, 0);
        mpara.set(7, 0, 0);
        mpara.set(8, 0, 0);
        */
        DenseMatrix64F mpara = new DenseMatrix64F(6, 1);
        mpara.set(0, 0, 1);
        mpara.set(1, 0, 1);
        mpara.set(2, 0, 1);
        mpara.set(3, 0, 0);
        mpara.set(4, 0, 0);
        mpara.set(5, 0, 0);

        //隨便假設輸入data
        DenseMatrix64F mX = new DenseMatrix64F(allcaliAccebuffer.size(), 1);
        for (int i = 0; i < allcaliAccebuffer.size(); i++) {
            mX.set(i, 0, i);
        }
        //設定理想結果:重力加速度


        DenseMatrix64F mY = new DenseMatrix64F(allcaliAccebuffer.size(), 1);
        for (int i = 0; i < allcaliAccebuffer.size(); i++) {
            mY.set(i, 0, Math.pow(g, 2));
        }

        //optimize
        tLM.optimize(mpara, mX, mY, allcaliAccebuffer);
        //LogCSV("","","",String.valueOf(mLM.getParameters().get(6, 0)),String.valueOf(mLM.getParameters().get(7, 0)),String.valueOf(mLM.getParameters().get(8,0)));
    }

    //test
    public void TEST() {
        ArrayList<sensordata> csvdata = new ArrayList<sensordata>();
        try {
            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            String fileName = "perfectsensordata.csv";
            String filePath = baseDir + File.separator + fileName;
            CSVReader reader = new CSVReader(new FileReader(filePath), '\n');
            try {
                for (String[] k : reader.readAll()) {
                    for (String v : k) {
                        Log.d(TAG, "csv " + String.valueOf(v));
                        float[] gotdata = new float[3];
                        String[] parts = v.split(",");
                        gotdata[0] = Float.parseFloat(parts[0]);
                        gotdata[1] = Float.parseFloat(parts[1]);
                        gotdata[2] = Float.parseFloat(parts[2]);
                        csvdata.add(new sensordata(100, gotdata));
                        Log.d(TAG, "data " + gotdata[0] + " " + gotdata[1] + " " + gotdata[2]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

//Calibration init;
        ///理想error model值
        LevenbergMarquardt mLM = new LevenbergMarquardt(new LevenbergMarquardt.Function() {
            @Override
            public void compute(DenseMatrix64F param, DenseMatrix64F x, DenseMatrix64F y, ArrayList<proAcceGyroCali.sensordata> data) {
                for (int i = 0; i < x.getNumElements(); i++) {
                    DenseMatrix64F getxfromdata = new DenseMatrix64F(3, 1);
                    getxfromdata.set(0, 0, data.get(i).getData()[0]);
                    getxfromdata.set(1, 0, data.get(i).getData()[1]);
                    getxfromdata.set(2, 0, data.get(i).getData()[2]);
                    y.set(i, 0, acceCali(getxfromdata, param));

                }
            }
        });
        DenseMatrix64F mpara = new DenseMatrix64F(6, 1);
        mpara.set(0, 0, 0);
        mpara.set(1, 0, 0);
        mpara.set(2, 0, 0);
        mpara.set(3, 0, 1);
        mpara.set(4, 0, 1);
        mpara.set(5, 0, 1);


        //隨便假設輸入data

        DenseMatrix64F mX = new DenseMatrix64F(csvdata.size(), 1);
        for (int i = 0; i < csvdata.size(); i++) {
            mX.set(i, 0, i);
        }

        //設定理想結果:重力加速度
        DenseMatrix64F mY = new DenseMatrix64F(csvdata.size(), 1);
        for (int i = 0; i < csvdata.size(); i++) {
            mY.set(i, 0, Math.pow(g, 1));
        }
        //optimize
        mLM.optimize(mpara, mX, mY, csvdata);

        //LogCSV("","","",String.valueOf(mLM.getParameters().get(6, 0)),String.valueOf(mLM.getParameters().get(7, 0)),String.valueOf(mLM.getParameters().get(8,0)));
        Log.d(TAG, "LM: cost b/a: " + String.valueOf(mLM.getInitialCost()) + " " + mLM.getFinalCost());
        for (int l = 0; l < 6; l++) {
            Log.d(TAG, "LM: param " + String.valueOf(l) + " " + String.valueOf(mLM.getParameters().get(l, 0)));
        }
        Log.d(TAG, "LM: endded");

    }

    //convert List to double[]
    public double[] List2doublearr(List<Double> mList, int from, int to, int step) {
        double[] returndoublearr = new double[((to - from) / step) + 1];
        for (int i = from; i <= to; i = i + step) {
            returndoublearr[i] = mList.get(i);
        }
        return returndoublearr;
    }

    public float[] toFloatArray(double[] arr) {
        if (arr == null) return null;
        int n = arr.length;
        float[] ret = new float[n];
        for (int i = 0; i < n; i++) {
            ret[i] = (float) arr[i];
        }
        return ret;
    }

    private ArrayList<sensordata> getSumArray(ArrayList<sensordata> sensordatas) {
        ArrayList<sensordata> returnArray = new ArrayList<sensordata>();
        float[] tmp_data_sum = new float[sensordatas.get(0).getData().length];
        returnArray.add(new sensordata(sensordatas.get(0).getTime(), new float[sensordatas.get(0).getData().length]));
        for (int i = 0; i < sensordatas.size() - 1 + 1; i++) {
            for (int j = 0; j < tmp_data_sum.length; j++) {
                tmp_data_sum[j] += sensordatas.get(i).getData()[j];
            }
            returnArray.add(new sensordata(sensordatas.get(i).getTime(), tmp_data_sum));
            //LogCSV(String.valueOf(tmp_data_sum[0]), String.valueOf(tmp_data_sum[1]), "", "", "", "");
        }
        return returnArray;
    }

    public class gyrointegration {
        sensordata newsensordata;
        sensordata gyroDelta = new sensordata(0l, new float[]{0f, 0f, 0f});
        float samplerate = 0.01f;

        private void addgyro(sensordata ttmpgyrodata) {
            newsensordata = ttmpgyrodata;
            gyroDelta = new sensordata(
                    ttmpgyrodata.getTime(),
                    new float[]{
                            gyroDelta.getData()[0] + ttmpgyrodata.getData()[0] * samplerate,
                            gyroDelta.getData()[1] + ttmpgyrodata.getData()[1] * samplerate,
                            gyroDelta.getData()[2] + ttmpgyrodata.getData()[2] * samplerate,
                    }
            );
        }

        private sensordata getDelta() {
            return gyroDelta;
        }
    }

    public class rotateVector {
        float currXY = 10;
        long prevTimestamp = 0;

        public double setCurrXY(float currXY) {
            this.currXY = currXY * 0.01f;
            return currXY * 0.01;
        }

        public sensordata rotate(sensordata sensordataIN) {
            sensordata sensordataOUT = new sensordata();
            sensordataOUT.setTime(sensordataIN.getTime());
            double[][] rotationArray = {{Math.cos(currXY), -Math.sin(currXY)}, {Math.sin(currXY), Math.cos(currXY)}};
            double[][] input = {{sensordataIN.getData()[0]}, {sensordataIN.getData()[1]}};
            double[][] result = multiplyByMatrix(input, rotationArray);
            sensordataOUT.setData(new float[]{(float) result[0][0], (float) result[0][1]});
            return sensordataOUT;
        }
    }

    public class calLowPass {
        float v = 0.5f;

        public calLowPass(float v) {
            this.v = v;
        }

        private float[] lowPass(float[] input) {
            float[] output = new float[input.length];
            if (output == null) return input;

            for (int i = 0; i < input.length; i++) {
                output[i] = output[i] + v * (input[i] - output[i]);
            }
            return output;
        }
    }

    public class KalmanFilter {
        JKalman kalman = null;
        Matrix s, c, m;

        private float[] calKalman(float[] invalues) {
            if (kalman == null || m == null) {
                try {
                    //kalman = new JKalman(4, 2);
                    kalman = new JKalman(6, 3);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                double x = 0;
                double y = 0;
                double z = 0;

                // init

                //Matrix s = new Matrix(4, 1); // state [x, y, dx, dy, dxy]
                //Matrix c = new Matrix(4, 1); // corrected state [x, y, dx, dy, dxy]
                //Matrix m = new Matrix(2, 1); // measurement [x]
                Matrix s = new Matrix(6, 1); // state [x, y, z, dx, dy, dz]
                Matrix c = new Matrix(6, 1); // corrected state
                Matrix m = new Matrix(3, 1); // measurement [x, y, z]

                //m.set(0, 0, x);
                //m.set(1, 0, y);
                m.set(0, 0, x);
                m.set(1, 0, y);
                m.set(2, 0, z);

                // transitions for x, y, dx, dy
            /*
            double[][] tr =
                    {{1, 0, 1, 0},
                            {0, 1, 0, 1},
                            {0, 0, 1, 0},
                            {0, 0, 0, 1}};
            kalman.setTransition_matrix(new Matrix(tr));
            */
                double[][] tr = {{1, 0, 0, 1, 0, 0},
                        {0, 1, 0, 0, 1, 0},
                        {0, 0, 1, 0, 0, 1},
                        {0, 0, 0, 1, 0, 0},
                        {0, 0, 0, 0, 1, 0},
                        {0, 0, 0, 0, 0, 1}};
                kalman.setTransition_matrix(new Matrix(tr));
                // 1s somewhere?
                kalman.setError_cov_post(kalman.getError_cov_post().identity());
            }

            // check state before
            s = kalman.Predict();

            // function init :)
            // m.set(1, 0, rand.nextDouble());
            try {
                Matrix m = new Matrix(3, 1); // measurement [x]
                m.set(0, 0, (double) invalues[0]);
                m.set(1, 0, (double) invalues[1]);
                m.set(2, 0, (double) invalues[2]);
                // look better
                c = kalman.Correct(m);
                s = kalman.Predict();
                Log.d(TAG, "<Kalman> " + ";" + m.get(0, 0) + ";" + m.get(1, 0) + ";"
                        + s.get(0, 0) + ";" + s.get(1, 0) + ";" + s.get(2, 0) + ";" + s.get(3, 0) + ";"
                        + c.get(0, 0) + ";" + c.get(1, 0) + ";" + c.get(2, 0) + ";" + c.get(3, 0) + ";");
            } catch (Exception ex) {

            }
            return new float[]{(float) c.get(0, 0), (float) c.get(1, 0), (float) c.get(2, 0)};
        }

    }

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

        public long getTime() {
            return Time;
        }

        public void setTime(long time) {
            this.Time = time;
        }

        public float[] getData() {
            return Data;
        }

        public void setData(float[] data) {
            for (int i = 0; i < data.length; i++) {
                this.Data[i] = data[i];
            }
        }
    }

    public class Statistics {
        float[] data;
        int size;

        public Statistics(float[] data) {
            this.data = data;
            size = data.length;
        }

        float getMean() {
            float sum = (float) 0.0;
            for (float a : data)
                sum += a;
            return sum / size;
        }

        float getVariance() {
            float mean = getMean();
            float temp = 0;
            for (float a : data)
                temp += (mean - a) * (mean - a);
            return temp / size;
        }

        float getStdDev() {
            return (float) Math.sqrt(getVariance());
        }

        public float median() {
            Arrays.sort(data);

            if (data.length % 2 == 0) {
                return (float) ((data[(data.length / 2) - 1] + data[data.length / 2]) / 2.0);
            } else {
                return data[data.length / 2];
            }
        }
    }

    //high pass
    public class highPass {
        float alpha = 0.8f;
        //high pass
        float gravity = 0;

        public highPass() {
        }

        public highPass(float alpha) {
            this.alpha = alpha;
        }

        private float calhighPass(float input) {
            float linear_acceleration;
            gravity = alpha * gravity + (1 - alpha) * input;
            linear_acceleration = input - gravity;
            return linear_acceleration;
        }
    }

    //moving average filter
    public class Rolling {
        private int size;
        private double total = 0d;
        private int index = 0;
        private double samples[];

        public Rolling(int size) {
            this.size = size;
            samples = new double[size];
            for (int i = 0; i < size; i++) samples[i] = 0d;
        }

        public void add(double x) {
            total -= samples[index];
            samples[index] = x;
            total += x;
            if (++index == size) index = 0; // cheaper than modulus
        }

        public double getAverage() {
            return total / size;
        }
    }

    //Allan Variance
    public class calAllan {
        public sensordata getAvgAllan(List<sensordata> msensordatas) {
            List<sensordata> mcircular = new ArrayList<sensordata>();
            List<sensordata> mbincircular = new ArrayList<sensordata>();
            for (sensordata mmsensordata : msensordatas) {
                mcircular.add(mmsensordata);
                //if (mcircular.size() > 9) {
                float[] sumData = new float[mmsensordata.getData().length];
                for (int i = 0; i < mcircular.size(); i++) {
                    for (int j = 0; j < mmsensordata.getData().length; j++) {
                        sumData[j] += mcircular.get(i).getData()[j];
                    }
                }

                for (int j = 0; j < mmsensordata.getData().length; j++) {
                    sumData[j] /= mcircular.size();
                }
                sensordata tmpbin = new sensordata(mcircular.get(0).getTime(), sumData);
                mbincircular.add(tmpbin);
                mcircular = new ArrayList<sensordata>();
                //}
            }

            sensordata returnallan = new sensordata();
            float[] tmpsum = new float[mbincircular.get(0).getData().length];
            for (int i = 1; i < mbincircular.size(); i++) {
                for (int j = 0; j < mbincircular.get(i).getData().length; j++) {
                    tmpsum[j] += (float) Math.pow(mbincircular.get(i).getData()[j] - mbincircular.get(i - 1).getData()[j], 2);
                }
            }
            float[] returnallandata = new float[mbincircular.get(0).getData().length];
            for (int g = 0; g < mbincircular.get(0).getData().length; g++) {
                returnallandata[g] = tmpsum[g] / (2 * (mbincircular.size() - 1));
            }
            returnallan.setData(returnallandata);
            return returnallan;
        }
    }

    //Allan Variance2
    public class calAllan2 {
        private int n;
        private List<Double> tau = new ArrayList<Double>();
        private List<Double> D = new ArrayList<Double>();
        private List<Double> sig = new ArrayList<Double>();
        private List<Double> sig2 = new ArrayList<Double>();
        private List<Double> osig = new ArrayList<Double>();
        private List<Double> msig = new ArrayList<Double>();
        private List<Double> tsig = new ArrayList<Double>();
        private List<Double> u2 = new ArrayList<Double>();
        private List<Double> z1 = new ArrayList<Double>();
        private List<Double> z2 = new ArrayList<Double>();

        private double u, uu;

        public void calAllan(double[] y, int tau0) {
            n = y.length;
            int jj = (int) Math.floor(Math.log((n - 1) / 3) / Math.log(2));
            for (int j = 1; j <= jj; j++) {
                int m = (int) Math.pow(j, 2);

                tau.add((double) m * tau0);
                for (int k = 0; k <= n - m; k++) {
                    D.add(0d);
                }
                for (int i = 0; i <= n - m; i++) {
                    double tempd = 0d;
                    for (int t = i; t <= i + m - 1; t++) {
                        tempd += y[t];
                    }
                    D.set(i, tempd / m);
                }
                String v = String.valueOf(Math.pow(0.5 * mean(AVARfunc(D, 0, n - m, m)), 0.5));
                Log.d(TAG, "allll " + v);
                sig2.add(Math.pow(0.5 * mean(AVARfunc(D, 0, n - m, m)), 0.5));
                //AVAR
                sig2.set(j, Math.sqrt(0.5 * mean(AVARfunc(D, 0, n - m, m))));
                /*
                //N sample
                Statistics mst = new Statistics(toFloatArray(List2doublearr(D, 0, n - m , m)));

                sig.set(j , Double.valueOf(mst.getStdDev()));
                //AVAR
                sig2.set(j , Math.sqrt(0.5 * mean(AVARfunc(D, 0, n - m , m))));
                //OVERAVAR
                for (int g = m ; g <= n  - m; g++) {
                    z1.add(D.get(g));
                }
                for (int Eular_HP_a_X = 0; Eular_HP_a_X <= n - 2 * m; Eular_HP_a_X++) {
                    z2.add(D.get(Eular_HP_a_X));
                }
                u = 0;
                for (int f = 0; f < z1.size() - 1; f++) {
                    u += Math.pow(z1.get(f) - z2.get(f), 2);
                }
                osig.set(j , Math.sqrt(u / (n + 1 - 2 * m) / 2));

                //MVAR
                for (int o = 0; o <= n + 1 - 3 * m; o++) {
                    u2.set(o, 0d);
                }
                z1 = new ArrayList<Double>();
                z2 = new ArrayList<Double>();
                for (int L = 0; L <= n + 1 - 3 * m; L++) {
                    for (int u = 1 + L; u <= m + L; u++) {
                        z1.add(D.get(u));
                    }
                    for (int v =  m + L; v <= 2 * m + L-1; v++) {
                        z2.add(D.get(v));
                    }
                    double tmp = 0;
                    for (int f = 0; f < z1.size() - 1; f++) {
                        tmp += Math.pow(z2.get(f) - z1.get(f), 2);
                    }
                    u2.set(L , tmp);
                }


                uu = mean(u2);
                msig.set(j, Math.sqrt(uu / 2) / m);

                //TVAR
                tsig.set(j , tau.get(j + 1) * msig.get(j + 1) / Math.sqrt(3));
  */
            }

        }

        public List<Double> AVARfunc(List<Double> mList, int from, int to, int step) {
            List<Double> diffpow = new ArrayList<Double>();
            for (int i = from; i <= to - 1; i = i + step) {
                diffpow.add(Math.pow(mList.get(i + step) - mList.get(i), 2));
            }
            return diffpow;
        }

        public double mean(List<Double> m) {
            double sum = 0;
            for (int i = 0; i < m.size(); i++) {
                sum += m.get(i);
            }
            return sum / m.size();
        }

        public List<Double> getSig() {
            return sig;
        }

        public List<Double> getSig2() {
            return sig2;
        }

        public List<Double> getOsig() {
            return osig;
        }

        public List<Double> getMsig() {
            return msig;
        }

        public List<Double> getTsig() {
            return tsig;
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
        static final float NS2S = 1.0f / 1000000000.0f;
        long last_timestamp = 0;
        private double prevPos;
        private double deltaPos = 0;

        public RK4() {
        }

        public double getDeltaPos() {
            return deltaPos;
        }

        public Position integrate(Position position, long t, double dt, double acceleration) { //Heart of the RK4 integrator - I don't know what most of this is
            //save previous
            //double dt = (t - last_timestamp)/1000;
            last_timestamp = t;

            Derivative a = evaluate(position, t, 0, new Derivative(0, 0), acceleration);
            Derivative b = evaluate(position, t + dt * 0.5, dt * 0.5, a, acceleration);
            Derivative c = evaluate(position, t + dt * 0.5, dt * 0.5, b, acceleration);
            Derivative d = evaluate(position, t + dt, dt, c, acceleration);

            double dpdt = 1.0 / 6.0 * (a.dp + 2.0 * (b.dp + c.dp) + d.dp);
            double dvdt = 1.0 / 6.0 * (a.dv + 2.0 * (b.dv + c.dv) + d.dv);

            position.pos += dpdt * dt;
            position.v += dvdt * dt;

            deltaPos = position.pos - prevPos;
            prevPos = position.pos;
            Log.d(TAG, "forlogging2 " + String.valueOf(position.pos) + " " + String.valueOf(deltaPos));
            last_timestamp = t;
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

    //Eular
    public class calEular {
        static final float NS2S = 1.0f / 1000000000.0f;
        float[] last_values;
        float[] velocity = new float[]{0, 0, 0};
        float[] position = new float[]{0, 0, 0};
        long last_timestamp = 0;

        public void calc(sensordata msensordata) {
            if (last_values != null) {
                float dt = (msensordata.getTime() - last_timestamp) * NS2S;

                for (int index = 0; index < 3; ++index) {
                    velocity[index] += (msensordata.getData()[index] + last_values[index]) / 2 * dt;
                    position[index] += velocity[index] * dt;
                }
            } else {
                last_values = new float[3];
                velocity = new float[3];
                position = new float[3];
                velocity[0] = velocity[1] = velocity[2] = 0f;
                position[0] = position[1] = position[2] = 0f;
            }
            System.arraycopy(msensordata.getData(), 0, last_values, 0, 3);
            last_timestamp = msensordata.getTime();
            //LogCSV(String.valueOf(msensordata.getData()[0]),String .valueOf(msensordata.getData()[1]),String.valueOf(velocity[0]),String.valueOf(velocity[1]),String.valueOf(position[0]),String.valueOf(position[1]));
        }
    }

    public class display {
        public void displaystatus1(final String s) {
            DemoDrawUI.runOnUI(new Runnable() {
                @Override
                public void run() {
                    try {
                        DemoDrawUI.mlog_acce.setText(s);
                    } catch (Exception ex) {

                    }

                }
            });
        }

        public void displaystatus2(final String s) {
            DemoDrawUI.runOnUI(new Runnable() {
                @Override
                public void run() {
                    try {
                        DemoDrawUI.mlog_gyro.setText(s);
                    } catch (Exception ex) {

                    }

                }
            });
        }
    }

    public class tcpipdata {


        InetAddress serverAddr;
        Socket socket;

        public tcpipdata() {
          /*
            try {
                Log.d("TCP", "C: Connecting...");
                serverAddr = InetAddress.getByName("192.168.0.115");
                socket = new Socket(serverAddr, 4444);

            } catch (Exception e) {
                Log.e("TCP", "C: Error", e);
            }*/
        }

        public void tcpipdatasend(float f, float f2) {
            try {
                /*
                Log.d("TCP", "C: Sending: '" + f + "'");
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                //DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
                //DOS.writeFloat(f);
                out.println(String.valueOf(f));
                */

                /*
                PrintWriter out2 = new PrintWriter(socket.getOutputStream(), true);
                String line = null;
                line = String.valueOf(f);
                Log.d("TCP","Sending:" + line);
                out2.write(line +","+String.valueOf(f2)+ "\n");
                out2.flush();

                Log.d("TCP", "C: Sent. " + out2.toString());
                Log.d("TCP", "C: Done.");
                */

            } catch (Exception e) {
                Log.e("TCP", "S: Error", e);
            }
        }

        public void tcpipdatadisconnect() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    //get sample rate
    public class samplerate {
        long prevtime = System.currentTimeMillis();
        long currtime;

        public double getTimeDelta(sensordata currsensordata) {
            currtime = currsensordata.getTime();
            long delta = currtime - prevtime;
            prevtime = currtime;
            return delta / 1000;
        }
    }
}
