package com.project.nicki.displaystabilizer.dataprocessor;

/**
 * Created by nickisverygood on 3/9/2016.
 */

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw2;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LevenbergMarquardt;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v2_1;

import org.ejml.data.DenseMatrix64F;

import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jama.Matrix;
import jkalman.JKalman;

public class proAcceGyroCali2 {
    public static boolean getcaliLogSTATUS = false;
    public static int selectedMethod;
    //handle setText update
    public static boolean pendingUpdate = false;
    public static Handler applypara = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d("TAG", "Parameters");
            pendingUpdate = true;
        }
    };
    /////////////////////////////////////;
    public static sensordata tmpgyrodata;
    public static int nowparam = 1;
    public static float nowmultip = 1;
    //noshake
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
    public int modauto = -1;
    public boolean drawStarted = false;
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
    calRk4 mRk4 = new calRk4();
    calRk4 mcalRk4 = new calRk4();
    float[] mGravity = new float[3];
    float[] mGeomagnetic = new float[3];
    SensorCollect.sensordata thissensordata = null;
    float orientation[] = null;
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

    public proAcceGyroCali2(Context context) {
        mContext = context;
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

    public void Controller(SensorEvent mSensorEvent) {
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            thissensordata = new SensorCollect.sensordata(System.currentTimeMillis(), mSensorEvent.values, SensorCollect.sensordata.TYPE.ACCE);
            thissensordata.setData(new float[]{
                    mSensorEvent.values[0],
                    mSensorEvent.values[1] - 0.452898f,
                    mSensorEvent.values[2]
            });
        }

        if (mSensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = mSensorEvent.values;
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            orientation = mSensorEvent.values.clone();
            new LogCSV("ori2", "", "",
                    orientation[0],
                    orientation[1],
                    orientation[2]);
        }
        /*
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = mSensorEvent.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
            }
        }
*/
        if (thissensordata != null && orientation != null) {
            new LogCSV("acce1", "", "",
                    mSensorEvent.values[0],
                    mSensorEvent.values[1],
                    mSensorEvent.values[2]);
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putFloatArray("Pos", mcalRk4.calc(thissensordata).getData());
            bundle.putFloatArray("Orien", new float[]{orientation[0], orientation[1], orientation[2]});
            bundle.putLong("Time", thissensordata.getTime());
            msg.setData(bundle);
            stabilize_v2_1.getSensor.sendMessage(msg);

            thissensordata = null;
            orientation = null;
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

    public void RK4(sensordata sensordataIN) {
        if (RK4init == false || DemoDraw2.drawing == 0) {
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

        }

        //manual control
        RK4_LP_a_X = new calLowPass(0.9f);
        RK4_LP_a_Y = new calLowPass(0.9f);
        RK4_HP_a_X = new highPass(0.9f);
        RK4_HP_v_X = new highPass(0.9f);
        RK4_HP_p_X = new highPass(0.9f);
        RK4_HP_a_Y = new highPass(0.9f);
        RK4_HP_v_Y = new highPass(0.9f);
        RK4_HP_p_Y = new highPass(0.9f);
        RK4_staticOFFSET = 0.0017f;


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
            //RK4_initY.v = 0;
            //RK4_initX.v = 0;
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
        if (DemoDraw2.drawing < 2) {
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
        //("RK4_pos",String.valueOf(RK4_initX.pos),String.valueOf(RK4_initY.pos),"","","","");
        Log.d(TAG, "thisone p " + RK4_initX.pos);
        bundle.putLong("Time", sensordataOUT_p.getTime());
        msg.setData(bundle);
        msg.arg1 = 2;

        //proDataFlow.AcceHandler.sendMessage(msg);
        stabilize_v2_1.getSensor.sendMessage(msg);
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
        return getcaliLogSTATUS;
    }

    //test

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

        public sensordata(sensordata msensordata) {
            setData(msensordata.getData());
            setTime(msensordata.getTime());
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
