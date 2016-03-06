package com.project.nicki.displaystabilizer.dataprocessor;

import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nickisverygood on 3/5/2016.
 */
public class SensorCollect {
    //todel
    long initTime = System.currentTimeMillis();
    boolean done = false;

    private static final String TAG = "SensorCollect";
    List<sensordata> ACCEstorage = new ArrayList<>();
    List<sensordata> ORIENstorage = new ArrayList<>();
    List<sensordata> CAMEstorage = new ArrayList<>();
    float[] initLocation = new float[]{0, 0, 0};
    float[] initOrientation = new float[]{0, 0, 0};
    motion_Inertial motion_online = new motion_Inertial(initLocation, initOrientation);

    public void append(sensordata msensordata) {
        Log.d(TAG, "count: " + String.valueOf(ACCEstorage.size()) + " " + String.valueOf(ORIENstorage.size()) + " " + String.valueOf(CAMEstorage.size()));
        if (msensordata.type == sensordata.TYPE.ACCE) {
            ACCEstorage.add(msensordata);
        }
        if (msensordata.type == sensordata.TYPE.ORIEN) {
            ORIENstorage.add(msensordata);
            Log.d(TAG, "orien: " + String.valueOf(msensordata.getData()[0]));
        }
        if (msensordata.type == sensordata.TYPE.CAME) {
            CAMEstorage.add(msensordata);
        }
        motion_online.update(msensordata);
        Log.d(TAG, "amount: " + String.valueOf(ACCEstorage.size()));


        try {
            if (done == false) {
                LogCSV.LogCSV("debug20",
                        getInertialLocationList_online().get(getInertialLocationList_online().size() - 1).getData()[0],
                        getInertialLocationList_online().get(getInertialLocationList_online().size() - 1).getData()[1],
                        getInertialLocationList_online().get(getInertialLocationList_online().size() - 1).getData()[2]);
            }

        } catch (Exception ex) {

        }

        if (System.currentTimeMillis() - initTime > 5000 && done == false) {
            done = true;
            for (sensordata isensordata : getInertialLocationList_offline()) {
                LogCSV.LogCSV("debug9",
                        isensordata.getData()[0],
                        isensordata.getData()[1],
                        isensordata.getData()[2]);
            }
        }


    }

    //reset when start drawing
    public void reset() {
        initLocation = new float[]{0, 0, 0};
        if (ORIENstorage.size() == 0) {
            reset();
        } else {
            initOrientation = ORIENstorage.get(0).getData();
        }
    }

    /////location inertia
    //get full
    public List<sensordata> getInertialLocationList_offline() {
        return new motion_Inertial(initLocation, initOrientation).getLocationList_full(ACCEstorage, ORIENstorage);
    }

    //get realtime online
    public List<sensordata> getInertialLocationList_online() {
        return motion_online.getLocationList_online();
    }

    public static class sensordata {
        private static final String TAG = "sensordata";
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

        public sensordata(long time, float[] data, sensordata.TYPE type) {
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

        public sensordata.TYPE getType() {
            return type;
        }

        public void setType(sensordata.TYPE type) {
            this.type = type;
        }

        public enum TYPE {
            ACCE,
            ORIEN,
            CAME,
            UNDE,
            LOCA,
            ACCE_world;

            private TYPE() {
            }
        }
    }


}
