package com.project.nicki.displaystabilizer.dataprocessor;

import android.content.Context;

import com.project.nicki.displaystabilizer.dataprovider.getAccelerometer;

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


    static final float NS2S = 1.0f / 1000000000.0f;
    float[] last_values = null;
    float[] velocity = null;
    float[] position = null;
    float[] acceleration = null;
    long last_timestamp = 0;




    @Override
    public void run() {
        long currTime = System.currentTimeMillis();
        if (init == true) {
            for (int t = 0; t < 1000; t++) {
                proGravX = proGravX + getAccelerometer.AcceX;
                proGravY = proGravX + getAccelerometer.AcceY;
                proGravZ = proGravX + getAccelerometer.AcceZ;
            }
            proGravX = proGravX / 1000;
            proGravY = proGravY / 1000;
            proGravZ = proGravZ / 1000;
            init = false;
        }

        proAcceX = getAccelerometer.AcceX - proGravX;
        proAcceY = getAccelerometer.AcceY - proGravY;
        proAcceZ = getAccelerometer.AcceZ - proGravZ;



/*

        if(last_values != null){
            if(DemoDraw.drawing == false){
                Arrays.fill(position,0);
            }
            if( Math.abs(acceleration[0])<0.5 &&  Math.abs(acceleration[1])<0.5 && Math.abs(acceleration[2])<0.5){
                Arrays.fill(velocity,0);
            }
            float dt = (getAccelerometer.AcceTime - last_timestamp) * NS2S;

            acceleration[0]=(float) proAcceX - (float) 0.0188;
            acceleration[1]=(float) proAcceY - (float) 0.00217;
            acceleration[2]=(float) proAcceZ + (float) 0.01857;

            for(int index = 0; index < 3;++index){
                velocity[index] += (acceleration[index] + last_values[index])/2 * dt;
                position[index] += velocity[index] * dt;
            }
        }
        else{
            last_values = new float[3];
            acceleration = new float[3];
            velocity = new float[3];
            position = new float[3];
            velocity[0] = velocity[1] = velocity[2] = 0f;
            position[0] = position[1] = position[2] = 0f;
        }
        System.arraycopy(acceleration, 0, last_values, 0, 3);
        last_timestamp = getAccelerometer.AcceTime;

        proAcceX = position[0];
        proAcceY = position[1];
        proAcceZ = position[2];


        if(proAcceX>0){
            Log.d(TAG,"GGGGGGG      XX+");
        }else {
            Log.d(TAG,"GGGGGGG      XXXX+");
        }
        if(proAcceY>0){
            Log.d(TAG,"GGGGGGG      YY+");
        }else {
            Log.d(TAG,"GGGGGGG      YYYY+");
        }
        if(proAcceZ>0){
            Log.d(TAG,"GGGGGGG      ZZ+");
        }else{
            Log.d(TAG,"GGGGGGG      ZZZZ+");
        }
*/





        /*
        data = new float[3];
        data[0] = proAcceX;
        data[1] = proAcceY;
        data[2] = proAcceZ;
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putFloatArray("Acce",data);
        bundle.putLong("Time",getAccelerometer.AcceTime);
        msg.setData(bundle);
        proDataFlow.AcceHandler.sendMessage(msg);

        Log.d(TAG, String.valueOf(proAcceX) + " " + String.valueOf(proAcceY) + " " + String.valueOf(proAcceZ));
        */

    }




}
