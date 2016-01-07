package com.project.nicki.displaystabilizer.dataprocessor;

/**
 * Created by nickisverygood on 12/25/2015.
 */

import android.content.Context;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

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
import java.util.concurrent.TimeUnit;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import jama.Matrix;
import jkalman.JKalman;


public class proAcceGyroCali extends getAcceGyro {
    //low pass
    static final float ALPHA = 0.25f; // if ALPHA = 1 OR 0, no filter applies.
    public static boolean getcaliLogSTATUS = false;
    public static boolean Calibrated = false;
    //noshake
    private final int SENEOR_TYPE = Sensor.TYPE_LINEAR_ACCELERATION;
    private final int ACCELEROMOTER_FPS = SensorManager.SENSOR_DELAY_FASTEST;
    private final int BUFFER_SECOND = 4;
    private final int FPS = 60;
    private final int BUFFER_DATA_SIZE = BUFFER_SECOND * FPS;
    public int HistoryLength = 100;
    public ArrayList<sensordata> AcceCircular = new ArrayList<>();
    public ArrayList<sensordata> GyroCircular = new ArrayList<>();
    public ArrayList<sensordata> MagnCircular = new ArrayList<sensordata>();
    public int allanhistory = 0;
    public float[] tmpAcce = new float[3];
    public float[] avgAcce = new float[3];
    public float[] tmpGyro = new float[3];
    public float[] avgGyro = new float[3];
    public float Gallanvar;
    public long Ginittime;
    //for staticdetetc()
    public ArrayList<sensordata> lAcceCircular = new ArrayList<>();
    public boolean caliLogSTATUS = false;
    String TAG = "proAcceGyroCali";
    String csvName = "proAcceGyroCali.csv";
    ArrayList<sensordata> tmpcalibuffer = new ArrayList<>();
    //Kalman Filter
    JKalman kalman = null;
    Matrix s, c, m;
    double g = 9.806 - (1 / 2) * (9.832 - 9.780) * Math.cos(2 * 25.048212 * Math.PI / 180);
    //low pass
    float[] mSensorVals;
    //LogEverything
    ArrayList<sensordata> rawAcceAll = new ArrayList<sensordata>();
    //rk4
    Rolling avg = new Rolling(50);
    RK4 mrk4 = new RK4();
    Position initX = new Position(0, 0);
    highPass h = new highPass(0.99f);
    highPass hv = new highPass(0.99f);
    highPass hp = new highPass(0.99f);
    Rolling avg2 = new Rolling(50);
    RK4 mrk42 = new RK4();
    Position initX2 = new Position(0, 0);
    highPass h2 = new highPass(0.99f);
    highPass hv2 = new highPass(0.99f);
    highPass hp2 = new highPass(0.99f);
    LevenbergMarquardt mLM;
    DenseMatrix64F paramgot;
    FileWriter mFileWriter;
    private int statichistorylength = 1000;
    private int statichistory = 0;
    private int Asamplenum = 50;
    private float[] AcceXsam = new float[Asamplenum];
    private float[] AcceYsam = new float[Asamplenum];
    private float[] AcceZsam = new float[Asamplenum];
    private int Aintsam = 0;
    private int OFFSET_SCALE = 30;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private CircularBuffer mBufferX;
    private CircularBuffer mBufferY;
    private int mScreenHeight, mScreenWidth;
    private boolean noshakeinit = false;
    private float[] lAcceXsam = new float[Asamplenum];
    private float[] lAcceYsam = new float[Asamplenum];
    private float[] lAcceZsam = new float[Asamplenum];
    private int lAintsam = 0;
    private int Gsamplenum = 50;
    private float[] GyroXsam = new float[Asamplenum];
    private float[] GyroYsam = new float[Asamplenum];
    private float[] GyroZsam = new float[Asamplenum];
    private int Gintsam = 0;
    private float GallanMIN;
    private float GallanMINtmp = GallanMIN + 1000;
    private ArrayList<ArrayList<sensordata>> caliAccebuffer = new ArrayList<>();
    private int caliLogSTATUSnum = 0;
    private boolean precaliLogSTATUS = caliLogSTATUS;

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

    public void Controller(SensorEvent mSensorEvent) {
        RK4(mSensorEvent);
    }

    public void NoShake(final SensorEvent mSensorEvent) {
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION  ) {
            Calibrated = false;
            if(Calibrated == true && paramgot!=null){
                sensordata msensordata = new sensordata(mSensorEvent.timestamp,mSensorEvent.values);
                sensordata msensordataCALI = RawAcceCali(msensordata,paramgot);
                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putFloatArray("Acce", msensordataCALI.getData());
                bundle.putLong("Time", System.currentTimeMillis());
                msg.setData(bundle);
                msg.arg1 = 2;
                //proDataFlow.AcceHandler.sendMessage(msg);
                stabilize_v2.getSensor.sendMessage(msg);
                //Log.d(TAG, "LM: STATUS CALI true");
                //LogCSV(String.valueOf(mSensorEvent.values[0]),String.valueOf(mSensorEvent.values[1]),String.valueOf(mSensorEvent.values[2]),String.valueOf(msensordataCALI.getData()[0]),String.valueOf(msensordataCALI.getData()[1]),String.valueOf(msensordataCALI.getData()[2]));
            }else {
                //mSensorVals = lowPass(mSensorEvent.values.clone(), mSensorVals);
                float[] kalmaned = KalmanFilter(mSensorEvent.values);
                //noshake
                final float x = kalmaned[0];
                final float y = kalmaned[1];
                final float z = mSensorEvent.values[2];
                new Thread(new Runnable() {
                    public void run() {
                        Log.d("HELLO","HELLO 3 ");
                        mBufferX.insert(x);
                        mBufferY.insert(y);
                        final float dx = -mBufferX.convolveWithH() * OFFSET_SCALE;
                        final float dy = -mBufferY.convolveWithH() * OFFSET_SCALE;
                        LogCSV(String.valueOf(dx),String.valueOf(dy),"","","","");
                        Message msg = new Message();
                        Bundle bundle = new Bundle();
                        msg.arg1 = 2;
                        bundle.putFloatArray("Acce", new float[]{dx, dy, 0});
                        bundle.putLong("Time", System.currentTimeMillis());
                        msg.setData(bundle);
                        msg.arg1 = 2;
                        //proDataFlow.AcceHandler.sendMessage(msg);
                        stabilize_v2.getSensor.sendMessage(msg);


                    }
                }).start();
                sensordata msensordata = new sensordata(System.currentTimeMillis(),kalmaned);
                DenseMatrix64F mpara = new DenseMatrix64F(6, 1);
                mpara.set(0, 0, 1.0090734231345496);
                mpara.set(1, 0, 0.914821159289414);
                mpara.set(2, 0, 0.9766550810797668);
                mpara.set(3, 0, 0.7745442786490917);
                mpara.set(4, 0, 0.900701438639133);
                mpara.set(5, 0, 0.7656634224611445);
                sensordata msensordataCALI = RawAcceCali(msensordata,mpara);
                if(getcaliLogSTATUS == true){
                    msensordataCALI.setData(new float[]{0,0,0});
                }
            }

        }
    }

    public void RK4(final SensorEvent mSensorEvent) {
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            if (DemoDraw.drawing > 1) {
                Rolling avg = new Rolling(50);
                RK4 mrk4 = new RK4();
                Position initX = new Position(0, 0);
                highPass h = new highPass(0.99f);
                highPass hv = new highPass(0.99f);
                highPass hp = new highPass(0.99f);
                Rolling avg2 = new Rolling(50);
                RK4 mrk42 = new RK4();
                Position initX2 = new Position(0, 0);
                highPass h2 = new highPass(0.99f);
                highPass hv2 = new highPass(0.99f);
                highPass hp2 = new highPass(0.99f);
            }
            avg.add(mSensorEvent.values[0]);
            float highpassed = h.calhighPass((float) avg.getAverage());
            if (Math.abs(highpassed) < 0.014) {
                initX.v = 0;
            }
            initX.v = hv.calhighPass((float) initX.v);
            //initX.pos = hp.calhighPass((float)initX.pos);
            mrk4.integrate(initX, 0, 0.01, highpassed);

            avg2.add(mSensorEvent.values[1]);
            float highpassed2 = h2.calhighPass((float) avg2.getAverage());

            if (Math.abs(highpassed2) < 0.014) {
                initX2.v = 0;
            }
            initX2.v = hv2.calhighPass((float) initX2.v);
            //initX.pos = hp.calhighPass((float)initX.pos);
            mrk42.integrate(initX2, 0, 0.01, highpassed2);
            LogCSV(String.valueOf(avg2.getAverage()), String.valueOf(avg.getAverage()), String.valueOf(initX.pos), String.valueOf(initX2.pos), "", "");
            Log.d(TAG, "forlogging " + String.valueOf(initX.pos) + " " + String.valueOf(initX2.pos));
            sensordata msensordata = new sensordata(mSensorEvent.timestamp, new float[]{(float) initX.pos, (float) initX2.pos});
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putFloatArray("Acce", msensordata.getData());
            bundle.putLong("Time", System.currentTimeMillis());
            msg.setData(bundle);
            msg.arg1 = 2;
            stabilize_v2.setcX(-3000);
            stabilize_v2.setcY(-3000);
            //proDataFlow.AcceHandler.sendMessage(msg);
            stabilize_v2.getSensor.sendMessage(msg);

        }
    }

    public void Calibration(final SensorEvent mSensorEvent) {
        if (noshakeinit == false) {
            //noshake
            //noshake
            //Display display = mContext.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            //display.getSize(size);
            mScreenWidth = 1440;
            mScreenHeight = 2560;
            mBufferX = new CircularBuffer(BUFFER_DATA_SIZE, BUFFER_SECOND);
            mBufferY = new CircularBuffer(BUFFER_DATA_SIZE, BUFFER_SECOND);
            noshakeinit = true;
        }

        if (mSensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && Calibrated == false) {
            //LogCSV(String.valueOf(mSensorEvent.values[0]), String.valueOf(mSensorEvent.values[1]), String.valueOf(mSensorEvent.values[2]),"","","");
            rawAcceAll.add(new sensordata(mSensorEvent.timestamp, mSensorEvent.values));

            //(1)put into buffer
            if (AcceCircular.size() < HistoryLength) {
                AcceCircular.add(new sensordata(mSensorEvent.timestamp, mSensorEvent.values));
            } else {
                AcceCircular.remove(0);
                AcceCircular.add(new sensordata(mSensorEvent.timestamp, mSensorEvent.values));
            }

            //(2)put static into buffer
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
            if (getVarianceMagnitude(AcceXvar, AcceYvar, AcceZvar) < 1.865) {
                caliLogSTATUS = true;
                sensordata q = new sensordata(mSensorEvent.timestamp, mSensorEvent.values);
                if (tmpcalibuffer.size() < 100) {
                    tmpcalibuffer.add(q);
                }
            } else {
                caliLogSTATUS = false;
            }
            //(4)if status change & num>1-- =>throw LM
            if (precaliLogSTATUS == true && caliLogSTATUS == false && tmpcalibuffer.size() > 90) {
                //caliAccebuffer.add(mkArrayAverage(tmpcalibuffer));
                caliAccebuffer.add(tmpcalibuffer);
                Log.d(TAG, "LM: cali " + caliAccebuffer.size() + " tmp:" + tmpcalibuffer.size() + " " + Calibrated);
                tmpcalibuffer = new ArrayList<>();
            } else if (precaliLogSTATUS == true && caliLogSTATUS == false && tmpcalibuffer.size() < 90) {
                tmpcalibuffer = new ArrayList<>();
            }


            //(5)compute LM
            if (caliAccebuffer.size() > 10 && Calibrated == false) {
                //flatten all data
                ArrayList<sensordata> allcaliAccebuffer = new ArrayList<>();
                for (int j = 0; j < caliAccebuffer.size(); j++) {
                    for (int k = 0; k < caliAccebuffer.get(j).size(); k++) {
                        allcaliAccebuffer.add(caliAccebuffer.get(j).get(k));
                        //LogCSV(String.valueOf(caliAccebuffer.get(j).get(k).getData()[0]), String.valueOf(caliAccebuffer.get(j).get(k).getData()[1]), String.valueOf(caliAccebuffer.get(j).get(k).getData()[2]), "", "", "");
                        Log.d(TAG, "LM: flattening " + String.valueOf(caliAccebuffer.get(j).get(k).getData()[0]) + " " + String.valueOf(caliAccebuffer.get(j).get(k).getData()[1]) + " " + String.valueOf(caliAccebuffer.get(j).get(k).getData()[2]));
                    }
                }
                //cal LM
                Log.d(TAG, "LM: LM start");
                Calibrated = true;
                LM(mLM, allcaliAccebuffer);
                //disp result
                Log.d(TAG, "LM: cost b/a: " + String.valueOf(mLM.getInitialCost()) + " " + mLM.getFinalCost());
                for (int l = 0; l < 6; l++) {
                    Log.d(TAG, "LM: parm " + String.valueOf(l) + " " + String.valueOf(mLM.getParameters().get(l, 0)));
                }
                paramgot = mLM.getParameters();
            }

        }
    }

    public boolean detectStatic(sensordata msensordata){
        //(1)put into buffer
        if (lAcceCircular.size() < HistoryLength) {
            lAcceCircular.add(msensordata);
        } else {
            lAcceCircular.remove(0);
            lAcceCircular.add(msensordata);
        }

        //(2)put static into buffer
        for (int j = 0; j < lAcceCircular.size(); j++) {
            lAintsam = 0;
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

    public sensordata RawAcceCali(sensordata msensordata,DenseMatrix64F param){
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
        data[0] = (float)result.get(0,0);
        data[1] = (float)result.get(1,0);
        data[2] = (float)result.get(2,0);

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
            mY.set(i, 0, Math.pow(9.8, 2));
        }

        //optimize
        tLM.optimize(mpara, mX, mY, allcaliAccebuffer);
        //LogCSV("","","",String.valueOf(mLM.getParameters().get(6, 0)),String.valueOf(mLM.getParameters().get(7, 0)),String.valueOf(mLM.getParameters().get(8,0)));
    }

    public float[] KalmanFilter(float[] invalues) {
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

    protected float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
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

    //rk4
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
