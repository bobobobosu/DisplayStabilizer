
package com.project.nicki.displaystabilizer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.project.nicki.displaystabilizer.UI.DemoDrawUI;
import com.project.nicki.displaystabilizer.UI.data_visualize;
import com.project.nicki.displaystabilizer.dataprocessor.proAcceGyroCali;
import com.project.nicki.displaystabilizer.dataprocessor.proAccelerometer;
import com.project.nicki.displaystabilizer.dataprocessor.proDataFlow;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LM;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LMfunc;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LevenbergMarquardt;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;
import com.project.nicki.displaystabilizer.dataprovider.getAccelerometer;
import com.project.nicki.displaystabilizer.dataprovider.getGyroscope;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v1;

import org.ejml.data.DenseMatrix64F;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import au.com.bytecode.opencsv.CSVReader;
import jama.Matrix;

import java.util.Arrays;

import static java.lang.Math.cos;

public class init extends AppCompatActivity {
    String TAG = "init";
    double g = 9.806-(1/2)*(9.832-9.780)*Math.cos(2*25.048212*Math.PI/180);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //new Thread(new getAccelerometer(getBaseContext())).start();
        //new Thread(new getGyroscope(getBaseContext())).start();

        //new Thread(new proAccelerometer(getBaseContext())).start();
        //new Thread(new onlyAcceXY(getBaseContext())).start();
        //new Thread(new getFrontcam(getBaseContext())).start();
        //new Thread(new proCamera(getBaseContext())).start();

        //TEST();
        /////////////////////////////////////////////

         new Thread(new proDataFlow(getBaseContext())).start();
        new Thread(new stabilize_v1(getBaseContext())).start();
        new Thread(new getAcceGyro(getBaseContext())).start();
        Intent goto_DemoDrawUI = new Intent();
        overridePendingTransition(0, 0);
        goto_DemoDrawUI.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        goto_DemoDrawUI.setClass(init.this, DemoDrawUI.class);
        startActivity(goto_DemoDrawUI);


        /*
        Intent goto_DemoDrawUI = new Intent();
        overridePendingTransition(0, 0);
        goto_DemoDrawUI.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        goto_DemoDrawUI.setClass(init.this, data_visualize.class);
        startActivity(goto_DemoDrawUI);
*/





        /*
        Intent goto_DemoStabilizeOn = new Intent();
        goto_DemoStabilizeOn.setClass(init.this, DemoStabilizeOn.class);
        startActivity(goto_DemoStabilizeOn);
        */
        /*
        Intent goto_getFrontcam = new Intent();
        goto_getFrontcam.setClass(init.this, getFrontcam.class);
        startActivity(goto_getFrontcam);
        */
        /*
        Intent goto_getBackcam = new Intent();
        goto_getBackcam.setClass(init.this, getBackcam.class);
        startActivity(goto_getBackcam);
        */
        /*
        Intent goto_sensor_info = new Intent();
        goto_sensor_info.setClass(init.this, sensor_info.class);
        startActivity(goto_sensor_info);
        */


    }

    /*
    private void TEST() {
        ArrayList<proAcceGyroCali> csvdata = new ArrayList<proAcceGyroCali>();
        try {
            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            String fileName = "rAnalysisData.csv";
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
                        csvdata.add(new proAcceGyroCali(100, gotdata));
                        Log.d(TAG,"datd "+gotdata[0]+" "+gotdata[1]+" "+gotdata[2]);
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
            public void compute(DenseMatrix64F param, DenseMatrix64F x, DenseMatrix64F y, ArrayList<proAcceGyroCali> data) {
                for (int i = 0; i < x.getNumElements(); i++) {
                    DenseMatrix64F getxfromdata = new DenseMatrix64F(3, 1);
                    getxfromdata.set(0, 0, data.get(i).Data[0]);
                    getxfromdata.set(1, 0, data.get(i).Data[1]);
                    getxfromdata.set(2, 0, data.get(i).Data[2]);
                    y.set(i, 0, acceCali(getxfromdata, param));

                }
            }
        });
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
        for (int l = 0; l < 9; l++) {
            Log.d(TAG, "LM: param " + String.valueOf(l) + " " + String.valueOf(mLM.getParameters().get(l, 0)));
        }
        Log.d(TAG, "LM: endded");

    }

    public double acceCali(DenseMatrix64F x, DenseMatrix64F param) {
        double returey1 = 0;
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

        //Log.d(TAG, "getdimen " + mrawMatrix.getRowDimension() + "x" + mrawMatrix.getColumnDimension());  //1x3
        //Log.d(TAG, "getdimen " + mbiasMatrix.getRowDimension() + "x" + mbiasMatrix.getColumnDimension());//1x3
        //Log.d(TAG, "getdimen " + mscaleMatrix.getRowDimension() + "x" + mscaleMatrix.getColumnDimension());//3x3
        //Log.d(TAG, "getdimen " + mnonorMatrix.getRowDimension() + "x" + mnonorMatrix.getRowDimension());//3x3

        Matrix k = mrawMatrix.plus(mbiasMatrix); //1x3
        Matrix m = mnonorMatrix.times(mscaleMatrix); //3x3
        Matrix result = m.times(k.transpose());
        //Log.d(TAG, "LM: getvarm " + result.get(0, 0) + " " + result.get(1, 0) + " " + result.get(2, 0));
        try {
            returey1 = (double) proAcceGyroCali.getVarianceMagnitude((float) result.get(0, 0), (float) result.get(1, 0), (float) result.get(2, 0));

        } catch (Exception ex) {
            Log.d(TAG, "sssss");
        }
        Log.d(TAG, "returnoutput: " + returey1);
        return returey1;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    */
}

