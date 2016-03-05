package com.project.nicki.displaystabilizer.dataprocessor;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nickisverygood on 3/5/2016.
 */
public class SensorCollect {
    private static final String TAG = "SensorCollect";
    List<sensordata> ACCEstorage = new ArrayList<>();
    List<sensordata> ORIENstorage = new ArrayList<>();
    List<sensordata> CAMEstorage = new ArrayList<>();
    float[] currLocation = new float[]{0,0,0};

    public void append(sensordata msensordata){
        if(msensordata.type == sensordata.TYPE.ACCE){
            ACCEstorage.add(msensordata);
        }
        if(msensordata.type == sensordata.TYPE.ORIEN){
            ORIENstorage.add(msensordata);
            Log.d(TAG,"orien: "+String.valueOf(msensordata.getData()[0]));
        }
        if(msensordata.type == sensordata.TYPE.CAME){
            CAMEstorage.add(msensordata);
        }
        Log.d(TAG,"amount: "+String.valueOf(ACCEstorage.size()));
    }

    //location control
    public void LOCATION_reset(){
        currLocation = new float[]{0,0,0};
    }

    public static class sensordata {
        private static final String TAG = "sensordata" ;
        sensordata.TYPE type;
        private long Time;
        private float[] Data = new float[3];

        public sensordata() {
            this(0, new float[]{0, 0, 0}, TYPE.UNDE);
        }

        public sensordata(sensordata msensordata) {
            setData(msensordata.getData());
            setTime(msensordata.getTime());
            setType(msensordata.getType());
        }

        public sensordata(long time, float[] data,sensordata.TYPE type) {
            this.type = type;
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

        public sensordata.TYPE getType(){
            return type;
        }

        public void setType(sensordata.TYPE type){
            this.type = type;
        }
        public enum TYPE {
            ACCE,
            ORIEN,
            CAME,
            UNDE;
            private TYPE() {
            }
        }
    }

    //data get
 



}
