package com.project.nicki.displaystabilizer.dataprocessor;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;
import com.project.nicki.displaystabilizer.dataprovider.getAccelerometer;

import java.util.Arrays;
import java.util.Random;

import jkalman.JKalman;
import jama.Matrix;

/**
 * Created by nicki on 10/27/2015.
 */
public class proAccelerometer implements Runnable {
    public static float proAcceX, proAcceY, proAcceZ;
    public static float proGravX = 0, proGravY = 0, proGravZ = 0;
    private float[] data;
    public static long proTime = 1;
    public static float proPosX = 0, proPosY = 0, proPosZ = 0;
    private static boolean init = true;
    private String TAG = "proAccelerometer";
    private Context mContext;


    public proAccelerometer(Context context) {
        mContext = context;
    }

    private int initnum = 1;

    static final float NS2S = 1.0f / 1000000000.0f;
    float[] last_values = null;
    float[] velocity = null;
    float[] position = null;
    float[] acceleration = null;
    long last_timestamp = 0;

    JKalman kalman = null;
    Matrix s, c, m;

    public proAccelerometer() {

    }

    @Override
    public void run() {
        long currTime = System.currentTimeMillis();

        if (kalman == null || m == null) {
            try {
                kalman = new JKalman(4, 2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Random rand = new Random(System.currentTimeMillis() % 2011);
            double x = 0;
            double y = 0;
            // constant velocity
            double dx = rand.nextDouble();
            double dy = rand.nextDouble();
            // init
            Matrix s = new Matrix(4, 1); // state [x, y, dx, dy, dxy]
            Matrix c = new Matrix(4, 1); // corrected state [x, y, dx, dy, dxy]
            Matrix m = new Matrix(2, 1); // measurement [x]
            m.set(0, 0, x);
            m.set(1, 0, y);
            // transitions for x, y, dx, dy
            double[][] tr = {{1, 0, 1, 0},
                    {0, 1, 0, 1},
                    {0, 0, 1, 0},
                    {0, 0, 0, 1}};
            kalman.setTransition_matrix(new Matrix(tr));
            // 1s somewhere?
            kalman.setError_cov_post(kalman.getError_cov_post().identity());
        }

// check state before
        s = kalman.Predict();

        // function init :)
        // m.set(1, 0, rand.nextDouble());
        try{
            Matrix m = new Matrix(2, 1); // measurement [x]
            m.set(0, 0, (double) getAccelerometer.AcceX);
            m.set(1, 0, (double) getAccelerometer.AcceY);
            // look better
            c = kalman.Correct(m);
            s = kalman.Predict();
            Log.d(TAG, "<Kalman> " + ";" + m.get(0, 0) + ";" + m.get(1, 0) + ";"
                    + s.get(0, 0) + ";" + s.get(1, 0) + ";" + s.get(2, 0) + ";" + s.get(3, 0) + ";"
                    + c.get(0, 0) + ";" + c.get(1, 0) + ";" + c.get(2, 0) + ";" + c.get(3, 0) + ";");
        }catch(Exception ex){

        }

        data = new float[3];
        data[0] = proAcceX;
        data[1] = proAcceY;
        data[2] = proAcceZ;
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putFloatArray("Acce", data);
        bundle.putLong("Time", getAccelerometer.AcceTime);
        msg.setData(bundle);
        proDataFlow.AcceHandler.sendMessage(msg);
        Log.d(TAG, String.valueOf(proAcceX) + " " + String.valueOf(proAcceY) + " " + String.valueOf(proAcceZ));


    }


}
