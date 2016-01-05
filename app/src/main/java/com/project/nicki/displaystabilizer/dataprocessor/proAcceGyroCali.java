package com.project.nicki.displaystabilizer.dataprocessor;

/**
 * Created by nickisverygood on 12/25/2015.
 */

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.utils.Filter;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LevenbergMarquardt;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;
import com.project.nicki.displaystabilizer.dataprovider.getAccelerometer;
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
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import jama.Matrix;
import jkalman.JKalman;


public class proAcceGyroCali extends getAcceGyro {
    String TAG = "proAcceGyroCali";
    String csvName = "proAcceGyroCali.csv";
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

    private int statichistorylength = 1000;
    private int statichistory = 0;

    private int Asamplenum = 50;
    private float[] AcceXsam = new float[Asamplenum];
    private float[] AcceYsam = new float[Asamplenum];
    private float[] AcceZsam = new float[Asamplenum];
    private int Aintsam = 0;

    //for staticdetetc()
    public ArrayList<sensordata> lAcceCircular = new ArrayList<>();
    private float[] lAcceXsam = new float[Asamplenum];
    private float[] lAcceYsam = new float[Asamplenum];
    private float[] lAcceZsam = new float[Asamplenum];
    private int lAintsam = 0;
    public static boolean getcaliLogSTATUS = false;

    private int Gsamplenum = 50;
    private float[] GyroXsam = new float[Asamplenum];
    private float[] GyroYsam = new float[Asamplenum];
    private float[] GyroZsam = new float[Asamplenum];
    private int Gintsam = 0;
    private float GallanMIN;
    private float GallanMINtmp = GallanMIN + 1000;

    public static boolean Calibrated = false;
    private ArrayList<ArrayList<sensordata>> caliAccebuffer = new ArrayList<>();
    public boolean caliLogSTATUS = false;
    private int caliLogSTATUSnum = 0;
    private boolean precaliLogSTATUS = caliLogSTATUS;
    ArrayList<sensordata> tmpcalibuffer = new ArrayList<>();

    //Kalman Filter
    JKalman kalman =null;
    Matrix s, c, m;

    double g = 9.806-(1/2)*(9.832-9.780)*Math.cos(2*25.048212*Math.PI/180);

    //low pass
    float[] mSensorVals;

    //LogEverything
    ArrayList<sensordata> rawAcceAll = new ArrayList<sensordata>();

    LevenbergMarquardt mLM;
    DenseMatrix64F paramgot;
    FileWriter mFileWriter;

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


    public void CircularBuffer(SensorEvent mSensorEvent) {
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && Calibrated == false) {
            //LogCSV(String.valueOf(mSensorEvent.values[0]), String.valueOf(mSensorEvent.values[1]), String.valueOf(mSensorEvent.values[2]),"","","");
            rawAcceAll.add(new sensordata(mSensorEvent.timestamp,mSensorEvent.values));

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
                if(tmpcalibuffer.size() < 100){
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
                        Log.d(TAG,"LM: flattening "+String.valueOf(caliAccebuffer.get(j).get(k).getData()[0])+" "+String.valueOf(caliAccebuffer.get(j).get(k).getData()[1])+" "+String.valueOf(caliAccebuffer.get(j).get(k).getData()[2]));
                    }
                }
                //cal LM
                Log.d(TAG, "LM: LM start");
                Calibrated = true;
                LM(mLM, allcaliAccebuffer);
                //disp result
                Log.d(TAG, "LM: cost b/a: " + String.valueOf(mLM.getInitialCost()) + " " + mLM.getFinalCost());
                for (int l = 0; l <6; l++) {
                    Log.d(TAG, "LM: parm " + String.valueOf(l) + " " + String.valueOf(mLM.getParameters().get(l, 0)));
                }
                paramgot = mLM.getParameters();
            }

            //OLD
            /*
            //Calculate bias
            if (statichistory < statichistorylength) {
                for (int k = 0; k < 3; k++) {
                    avgAcce[k] = avgAcce[k] + mSensorEvent.values[k];
                }
                statichistory++;
            } else if (statichistory == statichistorylength) {
                for (int k = 0; k < 3; k++) {
                    //LogCSV("avg_ACCE"+k, String.valueOf(avgAcce[k]), "diff_ACCE" + k, String.valueOf(mSensorEvent.values[k] - avgAcce[k]));
                }
            } else {
                for (int k = 0; k < 3; k++) {
                    mSensorEvent.values[k] = mSensorEvent.values[k] - avgAcce[k];
                }
            }
            */
        }

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
                proDataFlow.AcceHandler.sendMessage(msg);
                //Log.d(TAG, "LM: STATUS CALI true");
                //LogCSV(String.valueOf(mSensorEvent.values[0]),String.valueOf(mSensorEvent.values[1]),String.valueOf(mSensorEvent.values[2]),String.valueOf(msensordataCALI.getData()[0]),String.valueOf(msensordataCALI.getData()[1]),String.valueOf(msensordataCALI.getData()[2]));
            }else {

                //mSensorVals = lowPass(mSensorEvent.values.clone(), mSensorVals);
                float[] kalmaned = KalmanFilter(mSensorEvent.values);
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
                LogCSV(String.valueOf(mSensorEvent.values[0]),
                        String.valueOf(mSensorEvent.values[1]),
                        String.valueOf(mSensorEvent.values[2]),
                        String.valueOf(msensordata.getData()[0]),
                        String.valueOf(msensordata.getData()[1]),
                        String.valueOf(msensordata.getData()[2]));

                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putFloatArray("Acce",msensordata.getData());
                //bundle.putLong("Time", (new Date()).getTime()
                //        + (mSensorEvent.timestamp - System.nanoTime()) / 1000000L);
                bundle.putLong("Time",System.currentTimeMillis());
                msg.setData(bundle);
                proDataFlow.AcceHandler.sendMessage(msg);

            }

        }
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            //pass para
            float[] data = new float[3];
            data[0] = mSensorEvent.values[0];
            data[1] = mSensorEvent.values[1];
            data[2] = mSensorEvent.values[2];
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putFloatArray("Gyro", data);
            bundle.putLong("Time", getEventTimestampInMills(mSensorEvent));
            msg.setData(bundle);
            //proDataFlow.GyroHandler.sendMessage(msg);
            Log.d(TAG, "GYROTIME " + String.valueOf(data[0]) + "  " + String.valueOf(System.currentTimeMillis() + "  " + String.valueOf((int) System.currentTimeMillis())));


            //put into buffer
            if (GyroCircular.size() < HistoryLength) {
                GyroCircular.add(new sensordata(mSensorEvent.timestamp, mSensorEvent.values));
            } else {
                GyroCircular.remove(0);
                GyroCircular.add(new sensordata(mSensorEvent.timestamp, mSensorEvent.values));
            }

            //cal allan variance
            if (allanhistory == 0) {
                Ginittime = mSensorEvent.timestamp;
            }
            allanhistory = allanhistory + 1;
            Gallanvar = (float) (Gallanvar + Math.pow(mSensorEvent.values[0] - tmpGyro[0], 2));
            Gallanvar = Gallanvar * (1 / (2 * (float) allanhistory));
            //LogCSV(String.valueOf(Gallanvar), "", "", "", " ", " ");
            GallanMIN = Gallanvar;
            if (GallanMIN < GallanMINtmp && GallanMINtmp != 0) {
                GallanMINtmp = GallanMIN;
                Log.d(TAG, "GallanMIN " + String.valueOf(GallanMINtmp) + " time" + String.valueOf(mSensorEvent.timestamp - Ginittime));
            }

            //Calculate abias
            if (statichistory < statichistorylength) {
                for (int k = 0; k < 3; k++) {
                    avgGyro[k] = avgGyro[k] + mSensorEvent.values[k];
                }
                statichistory++;
            } else if (statichistory == statichistorylength) {
                for (int k = 0; k < 3; k++) {
                    //LogCSV("avg_GYRO"+k, String.valueOf(avgGyro[k]), "diff_GYRO" + k, String.valueOf(mSensorEvent.values[k] - avgGyro[k]));
                }

            } else {
                for (int k = 0; k < 3; k++) {
                    mSensorEvent.values[k] = mSensorEvent.values[k] - avgGyro[k];
                }
            }


            if (GyroCircular.size() < HistoryLength) {
                GyroCircular.add(new sensordata(mSensorEvent.timestamp, mSensorEvent.values));
            } else {
                GyroCircular.remove(0);
                GyroCircular.add(new sensordata(mSensorEvent.timestamp, mSensorEvent.values));
            }
        }
    }



    public class sensordata {
        private long Time;
        private float[] Data = new float[3];
        public sensordata(){
            this(0, new float[]{0, 0, 0});
        }
        public sensordata(long time,float[] data){
            this.Time = time;
            this.Data[0] = data[0];
            this.Data[1] = data[1];
            this.Data[2] = data[2];
        }
        public void setsensordata(long time,float[] data){
            this.Time = time;
            this.Data[0] = data[0];
            this.Data[1] = data[1];
            this.Data[2] = data[2];
        }
        public void setTime(long time){
            this.Time = time;
        }
        public void setData(float[] data){
            this.Data[0] = data[0];
            this.Data[1] = data[1];
            this.Data[2] = data[2];
        }
        public long getTime(){
            return Time;
        }
        public float[] getData(){
            return Data;
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
        if (getVarianceMagnitude(lAcceXvar, lAcceYvar, lAcceZvar) < 1.95) {
            getcaliLogSTATUS = true;
        } else {
            getcaliLogSTATUS = false;
        }
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

    public float[] KalmanFilter(float[] invalues){
        if (kalman == null || m == null) {
            try {
                //kalman = new JKalman(4, 2);
                kalman = new JKalman(6, 3);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Random rand = new Random(System.currentTimeMillis() % 2011);
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
            double[][] tr = { {1, 0, 0, 1, 0, 0},
                    {0, 1, 0, 0, 1, 0},
                    {0, 0, 1, 0, 0, 1},
                    {0, 0, 0, 1, 0, 0},
                    {0, 0, 0, 0, 1, 0},
                    {0, 0, 0, 0, 0, 1} };
            kalman.setTransition_matrix(new Matrix(tr));
            // 1s somewhere?
            kalman.setError_cov_post(kalman.getError_cov_post().identity());
        }

        // check state before
        s = kalman.Predict();

        // function init :)
        // m.set(1, 0, rand.nextDouble());
        try{
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
        }catch(Exception ex){

        }
        return new float[]{(float)c.get(0,0),(float)c.get(1,0),(float)c.get(2,0)};
    }

    //low pass
    static final float ALPHA = 0.25f; // if ALPHA = 1 OR 0, no filter applies.
    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    //high pass
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
                        Log.d(TAG,"data "+gotdata[0]+" "+gotdata[1]+" "+gotdata[2]);
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
}
