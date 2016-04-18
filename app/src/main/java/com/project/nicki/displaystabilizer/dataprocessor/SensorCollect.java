package com.project.nicki.displaystabilizer.dataprocessor;

import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;

import java.math.BigDecimal;
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
    List<sensordata> reORIENstorage_degree = new ArrayList<>();
    List<sensordata> CAMEstorage = new ArrayList<>();
    public float[] initLocation = new float[]{0, 0, 0};
    public float[] initOrientation = new float[]{0, 0, 0};
    motion_Inertial motion_online = new motion_Inertial(initLocation, initOrientation);

    public void append(sensordata msensordata) {
        Log.d(TAG, "count: " + String.valueOf(ACCEstorage.size()) + " " + String.valueOf(ORIENstorage.size()) + " " + String.valueOf(CAMEstorage.size()));
        if (msensordata.type == sensordata.TYPE.ACCE) {
            ACCEstorage.add(msensordata);

            new LogCSV("1append2","",
                    new BigDecimal(ACCEstorage.get(ACCEstorage.size()-1).getTime()).toPlainString(),
                    ACCEstorage.get(ACCEstorage.size()-1).getData()[0],
                    ACCEstorage.get(ACCEstorage.size()-1).getData()[1],
                    ACCEstorage.get(ACCEstorage.size()-1).getData()[2]);
        }
        if (msensordata.type == sensordata.TYPE.ORIEN_radian) {
            ORIENstorage.add(msensordata);
            reORIENstorage_degree.add(new sensordata(msensordata.getTime(),
                    new float[]{
                            msensordata.getData()[0]-initOrientation[0],
                            msensordata.getData()[1]-initOrientation[1],
                            msensordata.getData()[2]-initOrientation[2]
            }, sensordata.TYPE.relORIEN_degree));
            reset();
        }
        if (msensordata.type == sensordata.TYPE.CAME) {
            CAMEstorage.add(msensordata);
        }
        Log.d("TESTING", String.valueOf(msensordata.getTime()));
        motion_online.update(msensordata);



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

    /////location inertial
    //get full
    public List<sensordata> getInertialLocationList_offline() {
        Log.d("getLocationList", String.valueOf(ACCEstorage.size()));
        for(int i=0;i<ACCEstorage.size();i++){
            new LogCSV("getInertialLocationList_offline","",new BigDecimal(ACCEstorage.get(i).getTime()).toPlainString(),
                    ACCEstorage.get(i).getData()[0],ACCEstorage.get(i).getData()[1],ACCEstorage.get(i).getData()[2]
                    );
            Log.d("getLocationList", String.valueOf(i));
        }

        return new motion_Inertial(initLocation, initOrientation).getLocationList_full(ACCEstorage, ORIENstorage);
    }
    //get realtime online
    public List<sensordata> getInertialLocationList_online() {
        return motion_online.getLocationList_online();
    }
    /////Rotation inertial
    //getfull
    public List<sensordata> getInertialOrientationList_offline() {
        return reORIENstorage_degree;
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
            ACCE_world,
            ORIEN_radian,
            relORIEN_degree,
            CAME,
            UNDE,
            LOCA,
            TOUCH;

            private TYPE() {
            }
        }
    }


}
