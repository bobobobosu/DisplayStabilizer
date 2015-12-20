package com.project.nicki.displaystabilizer.dataprocessor;


import android.content.Context;
import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.utils.Quaternion;
import com.project.nicki.displaystabilizer.dataprovider.getAccelerometer;
import com.project.nicki.displaystabilizer.dataprocessor.proAccelerometer;
import com.project.nicki.displaystabilizer.dataprocessor.proGyroscope;
import jkalman.JKalman;
import jama.Matrix;
import java.util.ArrayList;

/**
 * Created by nickisverygood on 12/16/2015.
 */
public class proDataProcess implements Runnable {
    private static final String TAG = "proDataProcess";
    int sensorByteLength = 21;
    public ArrayList<ArrayList<Integer>> data;
    public ArrayList<ArrayList<Integer>> dataRot;
    public double score = 0;
    protected int capturedDataPoints;
    protected int historyLength = 1000;
    protected int plotEveryNth = 10;//100;
    protected int plotCount = 0;
    protected int noChannels = 4;
    protected int[] zeros;
    protected int bufferIndex;
    protected long currentNanos;
    public ArrayList<ArrayList<Float>> buffer;
    public float bufferlength = 1000;
    public float[] accPos = new float[3];
    public float[] accVel = new float[3];
    float[] dataAcceGyro;
    float[] prevdataAcceGyro;
    public proDataProcess() {
        madgwickAHRS = new MadgwickAHRSIMU(0.1d, new double[]{1, 0, 0, 0}, 256d);
        noChannels = 9;
        valuesIn = new short[noChannels];
        batteryIn = new short[1];
        zAxis = new Quaternion(0, Z[0], Z[1], Z[2]);
        System.out.println("Start reserving memory for dataRot");

        /*Add array for rotated data*/
        dataRot = new ArrayList<ArrayList<Integer>>(3);
        for (int i = 0; i < 3; ++i) {
            dataRot.add(new ArrayList<Integer>(historyLength));
            for (int j = 0; j < historyLength; ++j) {
                dataRot.get(i).add(0);

            }
        }
        System.out.println("Reserved memory for dataRot " + dataRot.get(0).size());
    }

    /**
     * sensor buffer length
     */
    int batteryByteLength = 5;
    /**
     * battery byte length, to be ignored for now
     */
    int maxByteLength = 21;
    /**
     * Max buffer length
     */
    int minByteLength = 5;
    /**
     * Min buffer length
     */
    byte[] dataInBuffer = null;
    /**
     * Read data into temp buffer
     */
    int bufferPointer = 0;
    short[] valuesIn;
    short[] batteryIn;
    int channelsToVisualize = 6;
    private Quaternion quat = null;
    double[] Z = {0d, 0d, 1d};
    Quaternion zAxis = null;
    MadgwickAHRS madgwickAHRS = null;


    static final float ALPHA = 0.15f;
    protected float lowPass( float input, float output ) {
           output = output + ALPHA * (input - output);
        return output;
    }



    private double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private double mean(double[] a) {
        double b = 0;
        for (int i = 0; i < a.length; ++i) {
            b += a[i] / ((double) a.length);
        }
        return b;
    }

    private double[] normalize(double[] a) {
        double magnitude = norm(a);
        for (int i = 0; i < a.length; ++i) {
            a[i] = (a[i] / magnitude);
        }
        return a;
    }

    private double[] diff(double[] arrIn) {
        double[] arrOut = new double[arrIn.length - 1];
        for (int i = 0; i < arrIn.length - 1; ++i) {
            arrOut[i] = arrIn[i + 1] - arrIn[i];
        }
        return arrOut;
    }

    public byte checkByteSumValue(byte[] bufferIn) {
        byte checksum = 0;
        byte tempRxBufIndex = 0;
        while (tempRxBufIndex < bufferIn.length) {
            checksum ^= bufferIn[tempRxBufIndex];
            ++tempRxBufIndex;
        }
        return checksum;
    }

    private double[] cross(double[] a, double[] b) {
        double[] c = new double[3];
        c[0] = (a[1] * b[2] - a[2] * b[1]);
        c[1] = (a[2] * b[0] - a[0] * b[2]);
        c[2] = (a[0] * b[1] - a[1] * b[0]);
        return c;
    }

    private double norm(double[] a) {
        double b = 0;
        for (int i = 0; i < a.length; ++i) {
            b += a[i] * a[i];
        }
        return Math.sqrt(b);
    }

    private double norm(double a, double b, double c) {
        return Math.sqrt(a * a + b * b + c * c);
    }


    public void decodeSensorPacket(float GyroX, float GyroY, float GyroZ, float AcceX, float AcceY, float AcceZ) {
        if(dataAcceGyro != null){
            float[] prevdataAcceGyro = new float[6];
            prevdataAcceGyro = dataAcceGyro;
        }
        float[] dataAcceGyro = new float[6];
        //apply low pass filter

        dataAcceGyro[0] = GyroX;
        dataAcceGyro[1] = GyroY;
        dataAcceGyro[2] = GyroZ;
        dataAcceGyro[3] = AcceX;
        dataAcceGyro[4] = AcceY;
        dataAcceGyro[5] = AcceZ;

        CircularBuffer(dataAcceGyro);

        AHRS(GyroX, GyroY, GyroZ, AcceX, AcceY, AcceZ);
        calPos(buffer);

    }

    public void CircularBuffer(float[] dataAcceGyro) {
        if (buffer == null) {
            buffer = new ArrayList<ArrayList<Float>>();
            buffer.add(new ArrayList<Float>());
            buffer.add(new ArrayList<Float>());
            buffer.add(new ArrayList<Float>());
            buffer.add(new ArrayList<Float>());
            buffer.add(new ArrayList<Float>());
            buffer.add(new ArrayList<Float>());
        }
        for (int i = 0; i < 6; ++i) {
            try{
                dataAcceGyro[i] = lowPass(dataAcceGyro[i],buffer.get(i).get(buffer.get(i).size()-2));
            }catch(Exception ex){
            }
            if (buffer.get(i).size() < bufferlength) {
                buffer.get(i).add(dataAcceGyro[i]);
            } else {
                buffer.get(i).add(dataAcceGyro[i]);
                buffer.get(i).remove(0);
            }

        }

    }


    public void calPos(ArrayList<ArrayList<Float>> buffer) {
        for (int j = 0; j < 3; j++) {
            accVel[j] = accVel[j] + buffer.get(j).get(buffer.get(j).size()-1)*50 ;
        }
        for (int j = 0; j < 3; j++) {
            accPos[j] = accPos[j] + accVel[j]*50 ;
        }
        Log.d(TAG,"Current Pos "+String.valueOf(accPos[0]));
    }

    public void AHRS(float GyroX, float GyroY, float GyroZ, float AcceX, float AcceY, float AcceZ) {

        //Check sampling machine time
        int noChannels = 9;
        int historyLength = 1000;
        /*
        valuesIn = new short[noChannels];
        long currentNanos = System.nanoTime();
        for (int i = 0;i<noChannels;++i){
            valuesIn[i] =(short)((((short) (bufferIn[2*i+1] & 0xff))<< 8) | (bufferIn[2*i+2] & 0xff));
        }
        byte currentCount = bufferIn[2*noChannels+1];
        */
        float[] valuesIn = new float[6];
        /*
        valuesIn[0] = (short) GyroX;
        valuesIn[1] = (short) GyroY;
        valuesIn[2] = (short) GyroZ;
        valuesIn[3] = (short) AcceX;
        valuesIn[4] = (short) AcceY;
        valuesIn[5] = (short) AcceZ;
*/

        ArrayList<ArrayList<Integer>> data = new ArrayList<ArrayList<Integer>>(noChannels);
        int[] zeros = new int[noChannels];
        for (int i = 0; i < noChannels; ++i) {
            zeros[i] = (int) (Math.pow(2.0, 10.0) - 1);
        }
        for (int i = 0; i < noChannels; ++i) {
            data.add(new ArrayList<Integer>(historyLength));
            for (int j = 0; j < historyLength; ++j) {
                data.get(i).add(0);
            }
        }
        int capturedDataPoints = 0;
        int plotCount = 0;


		/*Calculate and set quaternion!!*/
        /*Scale the values*/
        double[] imuData = new double[6];
        imuData[0] = ((double) AcceX) / 1000d;
        imuData[1] = ((double) AcceY) / 1000d;
        imuData[2] = ((double) AcceZ) / 1000d;
        imuData[3] = ((double) GyroX) / (10d * 180d) * Math.PI;
        imuData[4] = ((double) GyroY) / (10d * 180d) * Math.PI;
        imuData[5] = ((double) GyroZ) / (10d * 180d) * Math.PI;

        double initTheta;
        double[] rotAxis;
        /*The initial round*/
        if (quat == null) {
            //Set the initial orientation according to first sample of accelerometry
            System.out.println("X " + Double.toString(imuData[0]) + " Y " + Double.toString(imuData[1]) + " Z " + Double.toString(imuData[2]));
            initTheta = Math.acos(dot(normalize(new double[]{imuData[0], imuData[1], imuData[2]}), Z));
            rotAxis = cross(new double[]{imuData[0], imuData[1], imuData[2]}, Z);
            Log.d(TAG, "X " + Double.toString(rotAxis[0]) + " Y " + Double.toString(rotAxis[1]) + " Z " + Double.toString(rotAxis[2]) + " norm " + Double.toString(norm(rotAxis)) + " cos " + Double.toString(Math.cos(initTheta / 2d)) + " " + Double.toString(initTheta));
            if (norm(rotAxis) != 0) {
                rotAxis = normalize(rotAxis);
                //quat = new Quaternion(Math.cos(initTheta/2d),-Math.sin(initTheta/2d)*rotAxis[0],-Math.sin(initTheta/2d)*rotAxis[1],-Math.sin(initTheta/2d)*rotAxis[2]);
                quat = new Quaternion(Math.cos(initTheta / 2d), Math.sin(initTheta / 2d) * rotAxis[0], Math.sin(initTheta / 2d) * rotAxis[1], Math.sin(initTheta / 2d) * rotAxis[2]);
            } else {
                quat = new Quaternion(1d, 0d, 0d, 0d);
            }
            madgwickAHRS.setOrientationQuaternion(quat.getDouble());
            //System.out.println(Double.toString(initTheta) +" "+Double.toString(Math.cos(initTheta/2d))+" "+Double.toString(-Math.sin(initTheta/2d)*rotAxis[0])+" "+Double.toString(-Math.sin(initTheta/2d)*rotAxis[1])+" "+Double.toString(-Math.sin(initTheta/2d)*rotAxis[2]) );
            System.out.println(quat.toString());
            Log.d(TAG, quat.toString());
        } else {
            /*Use Madgwick AHRS IMU algorithm*/
            madgwickAHRS.AHRSUpdate(new double[]{imuData[3], imuData[4], imuData[5], imuData[0], imuData[1], imuData[2]});
            double[] tempQ = madgwickAHRS.getOrientationQuaternion();
            quat = new Quaternion(tempQ[0], tempQ[1], tempQ[2], tempQ[3]);
            Log.d(TAG, "Q = " + String.valueOf(tempQ[0]) + String.valueOf(tempQ[1]) + String.valueOf(tempQ[2]) + String.valueOf(tempQ[3]));
        }

        if (quat != null) {
            //visualizeWindow.setRotationQuaternion(quat.getFloat());
            //Calculated rotated values

            Quaternion grf = new Quaternion(0d, imuData[0], imuData[1], imuData[2]);
            //Quaternion rotatedQ = ((quat.conjugate()).times(grf)).times(quat);
            Quaternion rotatedQ = (quat.times(grf)).times(quat.conjugate());
            double[] rotatedVals = rotatedQ.getAxis();
            Log.d(TAG, "Got to rotating data X " + rotatedVals[0] + " Y " + rotatedVals[1] + " Z " + rotatedVals[2]);
            //System.out.println("Got to rotating data X "+rotatedVals[0]+" Y "+rotatedVals[1]+" Z "+rotatedVals[2]);
			/*Assign the values to ArrayLists*/
            for (int i = 0; i < 3; ++i) {
                if (capturedDataPoints < historyLength) {
                    dataRot.get(i).set(capturedDataPoints, (int) rotatedVals[i]);
                } else {
                    dataRot.get(i).add((int) (rotatedVals[i] * 1000.0d + 500.0));
                    dataRot.get(i).remove(0);    //Remove the oldest value
                }
            }
        }

        /*
		//Assign the values to ArrayLists
        for (int i = 0; i < noChannels; ++i) {
            if (capturedDataPoints < historyLength) {
                data.get(i).set(capturedDataPoints, (int) valuesIn[i]);
            } else {
                data.get(i).add((int) valuesIn[i]);
                data.get(i).remove(0);    //Remove the oldest value
            }
        }

        if (capturedDataPoints < historyLength) {
            ++capturedDataPoints;
        }
*/

    }


    @Override
    public void run() {

        while (true) {
            Log.d(TAG, String.valueOf(proGyroscope.proGyroX) + String.valueOf(proGyroscope.proGyroY) + String.valueOf(proGyroscope.proGyroZ) + String.valueOf(proAccelerometer.proAcceX) + String.valueOf(proAccelerometer.proAcceY) + String.valueOf(proAccelerometer.proAcceZ));
            if (proGyroscope.proGyroX != 0 && proGyroscope.proGyroX != 0 && proGyroscope.proGyroX != 0 && proAccelerometer.proAcceX != 0 && proAccelerometer.proAcceY != 0 && proAccelerometer.proAcceZ != 0) {
                decodeSensorPacket(proGyroscope.proGyroX, proGyroscope.proGyroY, proGyroscope.proGyroZ, proAccelerometer.proAcceX, proAccelerometer.proAcceY, proAccelerometer.proAcceZ);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
