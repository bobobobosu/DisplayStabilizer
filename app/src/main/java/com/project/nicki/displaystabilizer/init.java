
package com.project.nicki.displaystabilizer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.project.nicki.displaystabilizer.UI.DemoDrawUI;
import com.project.nicki.displaystabilizer.dataprocessor.proAcceGyroCali;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v2;

import org.ejml.data.DenseMatrix64F;

import jama.Matrix;

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

        //clean csvs
        /*
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "stabilize_v2.csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        if(f.exists()){
            f.delete();
        }
        */
        //new Thread(new proDataFlow(getBaseContext())).start();
        //new Thread(new stabilize_v1(getBaseContext())).start();
        new Thread(new stabilize_v2(getBaseContext())).start();
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

    public class sensordata {
        private long Time;
        private float[] Data = new float[3];

        public sensordata() {
            this(0, new float[]{0, 0, 0});
        }

        public sensordata(long time, float[] data) {
            this.Time = time;
            this.Data[0] = data[0];
            this.Data[1] = data[1];
            this.Data[2] = data[2];
        }

        public void setsensordata(long time, float[] data) {
            this.Time = time;
            this.Data[0] = data[0];
            this.Data[1] = data[1];
            this.Data[2] = data[2];
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
            this.Data[0] = data[0];
            this.Data[1] = data[1];
            this.Data[2] = data[2];
        }
    }
    
}

