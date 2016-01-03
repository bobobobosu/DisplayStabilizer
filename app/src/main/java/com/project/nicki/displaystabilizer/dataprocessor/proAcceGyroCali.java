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

import com.project.nicki.displaystabilizer.dataprocessor.utils.LevenbergMarquardt;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;

import org.ejml.data.DenseMatrix64F;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;

import au.com.bytecode.opencsv.CSVWriter;
import jama.Matrix;


public class proAcceGyroCali extends getAcceGyro {
    String TAG = "proAcceGyroCali";
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

    //teset
    int test = 0;
    //LogEverything
    ArrayList<sensordata> rawAcceAll = new ArrayList<sensordata>();

    LevenbergMarquardt mLM;
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

    public void CircularBuffer(SensorEvent mSensorEvent) {
        if (mSensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER && Calibrated == false) {
            rawAcceAll.add(new sensordata(mSensorEvent.timestamp,mSensorEvent.values));

            //pass para
            float[] data = new float[3];
            data[0] = mSensorEvent.values[0];
            data[1] = mSensorEvent.values[1];
            data[2] = mSensorEvent.values[2];
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putFloatArray("Acce", data);
            bundle.putLong("Time", System.currentTimeMillis());
            msg.setData(bundle);
            proDataFlow.AcceHandler.sendMessage(msg);

            //put into buffer
            if (AcceCircular.size() < HistoryLength) {
                AcceCircular.add(new sensordata(mSensorEvent.timestamp, mSensorEvent.values));
            } else {
                AcceCircular.remove(0);
                AcceCircular.add(new sensordata(mSensorEvent.timestamp, mSensorEvent.values));
            }

            //put static into buffer
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

            //logging
            precaliLogSTATUS = caliLogSTATUS;
            if (getVarianceMagnitude(AcceXvar, AcceYvar, AcceZvar) < 1.865) {
                caliLogSTATUS = true;
                sensordata q = new sensordata(mSensorEvent.timestamp, mSensorEvent.values);
                tmpcalibuffer.add(q);
            } else {
                caliLogSTATUS = false;
            }
            //if status change & num>1-- =>throw LM
            if (precaliLogSTATUS == true && caliLogSTATUS == false && tmpcalibuffer.size() > 200) {
                //caliAccebuffer.add(mkArrayAverage(tmpcalibuffer));
                caliAccebuffer.add(tmpcalibuffer);
                Log.d(TAG, "LM: cali " + caliAccebuffer.size() + " tmp:" + tmpcalibuffer.size() + " " + Calibrated);
                tmpcalibuffer = new ArrayList<>();
            } else if (precaliLogSTATUS == true && caliLogSTATUS == false && tmpcalibuffer.size() < 200) {
                tmpcalibuffer = new ArrayList<>();
            }


            //compute LM
            if (caliAccebuffer.size() > 10 && Calibrated == false) {
                //flatten all data
                ArrayList<sensordata> allcaliAccebuffer = new ArrayList<>();
                for (int j = 0; j < caliAccebuffer.size(); j++) {
                    for (int k = 0; k < caliAccebuffer.get(j).size(); k++) {
                        allcaliAccebuffer.add(caliAccebuffer.get(j).get(k));
                        LogCSV(String.valueOf(caliAccebuffer.get(j).get(k).getData()[0]), String.valueOf(caliAccebuffer.get(j).get(k).getData()[1]), String.valueOf(caliAccebuffer.get(j).get(k).getData()[2]), "", "", "");
                        Log.d(TAG,"LM: flattening "+String.valueOf(caliAccebuffer.get(j).get(k).getData()[0])+" "+String.valueOf(caliAccebuffer.get(j).get(k).getData()[1])+" "+String.valueOf(caliAccebuffer.get(j).get(k).getData()[2]));
                    }
                }


                try {
                    //cal LM
                    Log.d(TAG, "LM: LM start");
                    Calibrated = true;
                    LM(mLM, allcaliAccebuffer);
                    //disp result
                    Log.d(TAG, "LM: cost b/a: " + String.valueOf(mLM.getInitialCost()) + " " + mLM.getInitialCost());
                    for (int l = 0; l < 9; l++) {
                        Log.d(TAG, "LM: parm " + String.valueOf(l) + " " + String.valueOf(mLM.getParameters().get(l, 0)));
                    }
                    DenseMatrix64F d = mLM.getParameters();
                } catch (Exception ex) {
                    Calibrated = false;
                    //clean reset
                    caliAccebuffer = new ArrayList<ArrayList<sensordata>>();
                    boolean Calibrated = false;
                    ArrayList<ArrayList<proAcceGyroCali>> caliAccebuffer = new ArrayList<ArrayList<proAcceGyroCali>>();
                    caliLogSTATUS = false;
                    caliLogSTATUSnum = 0;
                    precaliLogSTATUS = caliLogSTATUS = false;
                    ArrayList<proAcceGyroCali> tmpcalibuffer = new ArrayList<proAcceGyroCali>();
                    mLM = new LevenbergMarquardt(new LevenbergMarquardt.Function() {
                        @Override
                        public void compute(DenseMatrix64F param, DenseMatrix64F x, DenseMatrix64F y, ArrayList<sensordata> data) {
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
            }


/*
            //Log Everything
            rawAcceAll.add(new proAcceGyroCali(mSensorEvent.timestamp,mSensorEvent.values));
            ArrayList<proAcceGyroCali> alllogcaliAccebuffer = new ArrayList<proAcceGyroCali>();
            if (Calibrated == true) {
                for (int j = 0; j < caliAccebuffer.size(); j++) {
                    for (int k = 0; k < caliAccebuffer.get(j).size(); k++) {
                        Log.d(TAG, "LM: Logging");
                        alllogcaliAccebuffer.add(caliAccebuffer.get(j).get(k));
                    }
                }
                LogEverything(rawAcceAll, alllogcaliAccebuffer);
            }
*/

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
            bundle.putLong("Time", System.currentTimeMillis());
            msg.setData(bundle);
            proDataFlow.GyroHandler.sendMessage(msg);
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

    private void LogEverything(ArrayList<sensordata> rawAcceAll, ArrayList<sensordata> caliAccebuffer) {

        for (sensordata rawdata : rawAcceAll) {
            Log.d(TAG, "LM:LoggingEverything");
            LogCSV(String.valueOf(rawdata.getData()[0]), String.valueOf(rawdata.getData()[1]), String.valueOf(rawdata.getData()[2]), "", "", "");
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
        String fileName = "AnalysisData.csv";
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
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        returey1 = (double) getVarianceMagnitude((float) result.get(0, 0), (float) result.get(1, 0), (float) result.get(2, 0));
        Log.d(TAG, "returnoutput: " + returey1);
        return returey1;
    }

    public void LM(LevenbergMarquardt tLM, ArrayList<sensordata> allcaliAccebuffer) {
        //Calibration init
        ///理想error model值
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
}
