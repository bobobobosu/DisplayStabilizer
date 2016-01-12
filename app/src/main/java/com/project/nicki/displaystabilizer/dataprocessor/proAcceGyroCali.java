package com.project.nicki.displaystabilizer.dataprocessor;

/**
 * Created by nickisverygood on 12/25/2015.
 */

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

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
    public Rolling avg1;
    public Rolling avg2;
    public Rolling avg3;
    highPass RK4_HP_a_X = new highPass(0.99f);
    highPass RK4_HP_v_Y = new highPass(0.99f);
    highPass RK4_HP_p_X = new highPass(0.99f);
    highPass Eular_HP_a_Y = new highPass(0.99f);
    highPass Eular_HP_v_Y = new highPass(0.99f);
    highPass Eular_HP_p_Y = new highPass(0.99f);
    String TAG = "proAcceGyroCali";
    String csvName = "proAcceGyroCali.csv";
    ArrayList<sensordata> tmpcalibuffer = new ArrayList<>();
    ArrayList<ArrayList<sensordata>> caliAccebuffer;

    double g = 9.806 - (1 / 2) * (9.832 - 9.780) * Math.cos(2 * 25.048212 * Math.PI / 180);
    //LogEverything
    ArrayList<sensordata> rawAcceAll = new ArrayList<sensordata>();
    //rk4
    Rolling avg = new Rolling(50);
    RK4 mrk4_X = new RK4();
    Position RK4_initX = new Position(0, 0);
    highPass Eular_HP_a_X = new highPass(0.99f);
    highPass Eular_HP_v_X = new highPass(0.99f);
    highPass Eular_HP_p_X = new highPass(0.99f);
    RK4 mrk4_Y = new RK4();
    Position RK4_initY = new Position(0, 0);
    highPass RK4_HP_a_Y = new highPass(0.99f);
    highPass RK4_HP_v_X = new highPass(0.99f);
    highPass RK4_HP_p_Y = new highPass(0.99f);
    //eular
    calEular mcalEular = new calEular();
    LevenbergMarquardt mLM;
    DenseMatrix64F paramgot;
    FileWriter mFileWriter;
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
    private KalmanFilter NoShakekal = new KalmanFilter();
    private boolean precaliLogSTATUS = caliLogSTATUS;
    private display mdisplay = new display();

    public proAcceGyroCali(Context context) {
        super(context);
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

    public void Controller(SensorEvent mSensorEvent) {
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            //////////////////////////////init///////////////////////
            if (controllerinit == false) {
                controllerinit = true;
                avg1 = new Rolling(50);
                avg2 = new Rolling(50);
                avg3 = new Rolling(50);
                cirbuff = new ArrayList<sensordata>();
                mdisplay.displaystatus2("Initialized");
            } else {
                sensordata thissensordata = new sensordata(mSensorEvent.timestamp, mSensorEvent.values);
                //lowpass
                float[] applylowpass = new float[thissensordata.getData().length];
                applylowpass = lowPass(thissensordata.getData(), applylowpass, 0.25f);
                thissensordata.setData(applylowpass);
                //moving average
                avg1.add((double) thissensordata.getData()[0]);
                avg2.add((double) thissensordata.getData()[1]);
                avg3.add((double) thissensordata.getData()[2]);
                thissensordata.setData(new float[]{(float) avg1.getAverage(), (float) avg2.getAverage(), (float) avg3.getAverage()});

                //Allan
                if (cirbuff.size() < 100) {
                    cirbuff.add(thissensordata);
                } else {
                    cirbuff.add(thissensordata);
                    cirbuff.remove(0);
                    calAllan mcalAllan = new calAllan();
                    sensordata Allan = mcalAllan.getAvgAllan(cirbuff);
                }
                //NoShake(thissensordata);
                mdisplay.displaystatus2("Method: " + "Eular");
                Eular(thissensordata);

            }
        }
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && didCalibraiotn == false) {
            if (ctrlerCalibrationinit == false) {
                avg1 = new Rolling(50);
                avg2 = new Rolling(50);
                avg3 = new Rolling(50);
                ctrlerCalibrationinit = true;
                mdisplay.displaystatus1("Calibrating...");
            } else {
                sensordata thissensordata = new sensordata(mSensorEvent.timestamp, mSensorEvent.values);
                //lowpass
                float[] applylowpass = new float[thissensordata.getData().length];
                applylowpass = lowPass(thissensordata.getData(), applylowpass, 0.25f);
                thissensordata.setData(applylowpass);
                //moving average
                avg1.add((double) thissensordata.getData()[0]);
                avg2.add((double) thissensordata.getData()[1]);
                avg3.add((double) thissensordata.getData()[2]);
                //MODE: Calibration
                Calibration(thissensordata);
            }
        }
    }


    public void Controller(SensorEvent mSensorEvent) {
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            //////////////////////////////init///////////////////////
            if (controllerinit == false) {
                controllerinit = true;
                avg1 = new Rolling(50);
                avg2 = new Rolling(50);
                avg3 = new Rolling(50);
                cirbuff = new ArrayList<sensordata>();
                mdisplay.displaystatus2("Initialized");
            } else {
                sensordata thissensordata = new sensordata(mSensorEvent.timestamp, mSensorEvent.values);
                //lowpass
                float[] applylowpass = new float[thissensordata.getData().length];
                applylowpass = lowPass(thissensordata.getData(), applylowpass, 0.25f);
                thissensordata.setData(applylowpass);
                //moving average
                avg1.add((double) thissensordata.getData()[0]);
                avg2.add((double) thissensordata.getData()[1]);
                avg3.add((double) thissensordata.getData()[2]);
                thissensordata.setData(new float[]{(float) avg1.getAverage(), (float) avg2.getAverage(), (float) avg3.getAverage()});

                //Allan
                if (cirbuff.size() < 100) {
                    cirbuff.add(thissensordata);
                } else {
                    cirbuff.add(thissensordata);
                    cirbuff.remove(0);
                    calAllan mcalAllan = new calAllan();
                    sensordata Allan = mcalAllan.getAvgAllan(cirbuff);
                }
                //NoShake(thissensordata);
                mdisplay.displaystatus2("Method: " + "Eular");
                Eular(thissensordata);

            }
        }
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && didCalibraiotn == false) {
            if (ctrlerCalibrationinit == false) {
                avg1 = new Rolling(50);
                avg2 = new Rolling(50);
                avg3 = new Rolling(50);
                ctrlerCalibrationinit = true;
                mdisplay.displaystatus1("Calibrating...");
            } else {
                sensordata thissensordata = new sensordata(mSensorEvent.timestamp, mSensorEvent.values);
                //lowpass
                float[] applylowpass = new float[thissensordata.getData().length];
                applylowpass = lowPass(thissensordata.getData(), applylowpass, 0.25f);
                thissensordata.setData(applylowpass);
                //moving average
                avg1.add((double) thissensordata.getData()[0]);
                avg2.add((double) thissensordata.getData()[1]);
                avg3.add((double) thissensordata.getData()[2]);
                //MODE: Calibration
                Calibration(thissensordata);
            }
        }
    }

    public void NoShake(sensordata sensordataIN) {
        sensordata msensordataCALI;
        if (NoShakeinit == false) {
            NoShakekal = new KalmanFilter();
            NoShakeinit = true;
            OFFSET_SCALE = 30;
        } else {
            sensordataIN.setData(NoShakekal.calKalman(sensordataIN.getData()));
            mBufferX.insert(sensordataIN.getData()[0]);
            mBufferY.insert(sensordataIN.getData()[1]);
            final float dx = -mBufferX.convolveWithH() * OFFSET_SCALE;
            final float dy = -mBufferY.convolveWithH() * OFFSET_SCALE;
            Message msg = new Message();
            Bundle bundle = new Bundle();
            msg.arg1 = 2;
            bundle.putFloatArray("Acce", new float[]{dx, dy, 0});
            bundle.putLong("Time", sensordataIN.getTime());
            msg.setData(bundle);
            stabilize_v2.getSensor.sendMessage(msg);
        }


    }

    public void RK4(sensordata sensordataIN) {
        if (DemoDraw.drawing > 1) {
            mrk4_X = new RK4();
            RK4_initX = new Position(0, 0);
            RK4_HP_a_X = new highPass(0.99f);
            RK4_HP_v_X = new highPass(0.99f);
            RK4_HP_p_X = new highPass(0.99f);
            mrk4_Y = new RK4();
            RK4_initY = new Position(0, 0);
            RK4_HP_a_Y = new highPass(0.99f);
            RK4_HP_v_Y = new highPass(0.99f);
            RK4_HP_p_Y = new highPass(0.99f);
        }
        //high pass
        //X
        float highpassed = RK4_HP_a_X.calhighPass(sensordataIN.getData()[0]);
        if (Math.abs(highpassed) < 0.014) {
            RK4_initX.v = 0;
        }
        RK4_initX.v = RK4_HP_v_X.calhighPass((float) RK4_initX.v);
        mrk4_X.integrate(RK4_initX, 0, 0.01, highpassed);
        //Y
        float highpassed2 = RK4_HP_a_Y.calhighPass(sensordataIN.getData()[1]);
        if (Math.abs(highpassed2) < 0.014) {
            RK4_initY.v = 0;
        }
        RK4_initY.v = RK4_HP_v_X.calhighPass((float) RK4_initY.v);
        mrk4_Y.integrate(RK4_initY, 0, 0.01, highpassed2);

        //KF
        sensordata sensordataOUT_a = new sensordata(sensordataIN.getTime(), RK4kal_a.calKalman(new float[]{(float) RK4_initX.a, (float) RK4_initY.a}));
        sensordata sensordataOUT_v = new sensordata(sensordataIN.getTime(), RK4kal_v.calKalman(new float[]{(float) RK4_initX.v, (float) RK4_initY.v}));
        sensordata sensordataOUT_p = new sensordata(sensordataIN.getTime(), RK4kal_p.calKalman(new float[]{(float) RK4_initX.pos, (float) RK4_initY.pos}));


        Log.d(TAG, "forlogging " + String.valueOf(RK4_initX.pos) + " " + String.valueOf(RK4_initY.pos));
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putFloatArray("Acce", sensordataOUT_p.getData());
        bundle.putLong("Time", sensordataOUT_p.getTime());
        msg.setData(bundle);
        msg.arg1 = 2;
        //stabilize_v2.setcX(-10000);
        //stabilize_v2.setcY(-10000);
        //proDataFlow.AcceHandler.sendMessage(msg);
        stabilize_v2.getSensor.sendMessage(msg);
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

    public void Eular(sensordata sensordataIN) {
        if (DemoDraw.drawing > 1) {
            Eular_HP_a_X = new highPass(0.99f);
            Eular_HP_v_X = new highPass(0.99f);
            Eular_HP_p_X = new highPass(0.99f);
            Eular_HP_a_Y = new highPass(0.99f);
            Eular_HP_v_Y = new highPass(0.99f);
            Eular_HP_p_Y = new highPass(0.99f);
            Eularkal_a = new KalmanFilter();
            Eularkal_v = new KalmanFilter();
            Eularkal_p = new KalmanFilter();
            mcalEular = new calEular();
        }

        ///////HP
        if (mcalEular.position == null) {
            mcalEular.calc(sensordataIN);
        }
        //X
        float highpassedX = Eular_HP_a_X.calhighPass(sensordataIN.getData()[0]);
        if (Math.abs(highpassedX) < 0.014) {
            mcalEular.velocity[0] = 0;
        }
        mcalEular.velocity[0] = Eular_HP_v_X.calhighPass(mcalEular.velocity[0]);
        //Y
        float highpassedY = Eular_HP_a_Y.calhighPass(sensordataIN.getData()[1]);
        if (Math.abs(highpassedY) < 0.014) {
            mcalEular.velocity[1] = 0;
        }
        mcalEular.velocity[1] = RK4_HP_v_X.calhighPass(mcalEular.velocity[1]);
        sensordataIN.setData(new float[]{highpassedX, highpassedY, sensordataIN.getData()[2]});


        /////////KF
        sensordata sensordataOUT_a = new sensordata(sensordataIN.getTime(), Eularkal_a.calKalman(sensordataIN.getData()));
        sensordata sensordataOUT_v = new sensordata(sensordataIN.getTime(), Eularkal_v.calKalman(mcalEular.velocity));
        sensordata sensordataOUT_p = new sensordata(sensordataIN.getTime(), Eularkal_p.calKalman(mcalEular.position));

        mcalEular.calc(sensordataIN);

        Log.d(TAG, "forlogging " + String.valueOf(mcalEular.position[0]) + " " + String.valueOf(mcalEular.position[1]));
        sensordata sensordataOUT = new sensordata(sensordataIN.getTime(), new float[]{mcalEular.position[0], mcalEular.position[1]});
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putFloatArray("Acce", sensordataOUT_p.getData());
        bundle.putLong("Time", sensordataOUT_p.getTime());
        msg.setData(bundle);
        msg.arg1 = 2;
        //stabilize_v2.setcX(-10000);
        //stabilize_v2.setcY(-10000);
        //proDataFlow.AcceHandler.sendMessage(msg);
        stabilize_v2.getSensor.sendMessage(msg);


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
        } else {
            rawAcceAll.add(sensordataIN);
            //(1)put into buffer
            if (AcceCircular.size() < HistoryLength) {
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
            mdisplay.displaystatus1("num: " + String.valueOf(4 - caliAccebuffer.size()) + " buff: " + tmpcalibuffer.size() + " static: " + Math.floor((1.865 - tmpmagnitude) * 100 / 1.865) + "%");
            if (tmpmagnitude < 1.865) {
                caliLogSTATUS = true;
                if (tmpcalibuffer.size() < 100) {
                    tmpcalibuffer.add(sensordataIN);
                }
            } else {
                caliLogSTATUS = false;
            }
            //(4)if status change & num>1-- =>throw LM
            if (precaliLogSTATUS == true && caliLogSTATUS == false && tmpcalibuffer.size() > 90) {
                //caliAccebuffer.add(mkArrayAverage(tmpcalibuffer));
                caliAccebuffer.add(tmpcalibuffer);
                Log.d(TAG, "LM: cali " + caliAccebuffer.size() + " tmp:" + tmpcalibuffer.size());
                tmpcalibuffer = new ArrayList<>();
            }
            Log.d(TAG, "hi");
            Log.d(TAG, "LM: cali " + caliAccebuffer.size() + " tmp:" + tmpcalibuffer.size());
            //(5)compute LM
            // if (caliAccebuffer.size() > 10 && Calibrated == false) {

            if (caliAccebuffer.size() > 3) {
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
                                String.valueOf(Math.floor(mLM.getParameters().get(0, 0))) + " " +
                                String.valueOf(Math.floor(mLM.getParameters().get(0, 0))) + " " +
                                String.valueOf(Math.floor(mLM.getParameters().get(0, 0))) + " " +
                                String.valueOf(Math.floor(mLM.getParameters().get(0, 0))) + " " +
                                String.valueOf(Math.floor(mLM.getParameters().get(0, 0))) + " " +
                                String.valueOf(Math.floor(mLM.getParameters().get(0, 0))) + " "
                );
                paramgot = mLM.getParameters();
                didCalibraiotn = true;
            }
        }
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

    public boolean detectStatic(sensordata msensordata) {
        //(1)put into buffer
        if (lAcceCircular.size() < HistoryLength) {
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
        long timestamp = event.timestamp / 1000 / 1000;

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

    protected float[] lowPass(float[] input, float[] output, float v) {
        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + v * (input[i] - output[i]);
        }
        return output;
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
                if (mcircular.size() > 9) {
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
                }
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
                LogCSV(v, "", "", "", "", "");
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
        float[] last_values = null;
        float[] velocity = null;
        float[] position = null;
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
}
