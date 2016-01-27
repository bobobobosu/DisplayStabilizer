package com.project.nicki.displaystabilizer.dataprocessor;

import android.util.Log;

/**
 * Created by fan on 11/1/15.
 */
public class CircularBuffer {
    public static int CONST_K = 1;
    private final String TAG = "Calibration";
    private int maxSize;
    private float mDuration;
    private int aaaaa = 0;
    private int front = 0;
    private int rear = 0;
    private int bufLen = 0;
    private float[] mBuffer;

    public CircularBuffer(int size, float duration) {
        this.maxSize = size;
        this.mDuration = duration;
        this.mBuffer = new float[maxSize];
    }

    public boolean isEmpty() {
        return bufLen == 0;
    }

    public boolean isFull() {
        return bufLen == maxSize;
    }

    public void insert(float d) {
        if (!isFull()) {
            aaaaa = aaaaa + 1;
            bufLen++;
            rear = (rear + 1) % maxSize;

            mBuffer[rear] = d;
        } else {
            aaaaa = 0;
            delete();
            insert(d);
        }
    }

    public float delete() {
        if (!isEmpty()) {
            bufLen--;
            front = (front + 1) % maxSize;
            return mBuffer[front];
        } else {
            Log.e(TAG, "underflow");
            return -1;
        }
    }


    public float convolveWithH() {

        float sum = 0;
        /*
        if (bufLen != maxSize) {
            return -1;
        }
        */

        for (int i = 0; i < maxSize; i++) {
            int idx = (front + i) % maxSize;//front is the oldest one

            double t = mDuration - ((float) i) / maxSize * mDuration;
            double impulseResponse = t * Math.exp(-t * Math.sqrt(CONST_K));

            sum += (mBuffer[idx] * impulseResponse);

        }

        return sum;
    }

    public void display() {
        for (int i = 0; i < maxSize; i++) {
            int idx = (front + i) % maxSize;//the oldest one
            double t = mDuration - ((float) i) / maxSize * mDuration;
            double impulseResponse = t * Math.exp(-t * Math.sqrt(CONST_K));
            Log.d(TAG, idx + " " + mBuffer[idx]);
        }
    }

    public void setK(int k) {
        CONST_K = k;
        return;
    }
}
