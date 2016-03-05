package com.project.nicki.displaystabilizer.dataprocessor;

import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.utils.MatMultiply;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat.Matrix3D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by nickisverygood on 3/6/2016.
 */
public class motion_Inertial {
    float[] initLocation;
    float[] initOrientation;
    //online
    List<SensorCollect.sensordata> ACCEstorage_online = new ArrayList<>();
    List<SensorCollect.sensordata> ORIENstorage_online = new ArrayList<>();
    List<SensorCollect.sensordata> locationList_online = new ArrayList<>();
    calEular mcalEular_online = new calEular();

    public motion_Inertial(float[] initLocation, float[] initOrientation) {
        this.initLocation = initLocation;
        this.initOrientation = initOrientation;
    }

    public List<SensorCollect.sensordata> createLocationListfromLists(List<SensorCollect.sensordata> ACCEList, List<SensorCollect.sensordata> ORIENList) {
        List<SensorCollect.sensordata> locationList = new ArrayList<>();
        return locationList;
    }

    public List<SensorCollect.sensordata> getLocationList_full(List<SensorCollect.sensordata> ACCEstorage, List<SensorCollect.sensordata> ORIENstorage) {
        List<SensorCollect.sensordata> locationList_full = createLocationListfromLists(ACCEstorage, ORIENstorage);
        return locationList_full;
    }

    public List<SensorCollect.sensordata> getLocationList_online() {
        return locationList_online;
    }

    public void update(SensorCollect.sensordata msensordata) {
        if (msensordata.getType() == SensorCollect.sensordata.TYPE.ACCE) {
            ACCEstorage_online.add(msensordata);
        }
        if (msensordata.getType() == SensorCollect.sensordata.TYPE.ORIEN) {
            ORIENstorage_online.add(msensordata);
        }
        if(ACCEstorage_online.size()>10&&ORIENstorage_online.size()>10){
            logLength("update_ACCEstorage_online",ACCEstorage_online);
            logLength("update_ORIENstorage_online",ORIENstorage_online);
            mcalEular_online.calcList(convertcorrPHN2WLD(ACCEstorage_online,ORIENstorage_online));
            locationList_online.add(new SensorCollect.sensordata(msensordata.getTime(), mcalEular_online.position, SensorCollect.sensordata.TYPE.LOCA));
        }

    }


    //UTILS
    public List<SensorCollect.sensordata> convertcorrPHN2WLD(List<SensorCollect.sensordata> msensordataACCEList_phone, List<SensorCollect.sensordata> msensordataORIENList) {
        logLength("convertcorrPHN2WLD_msensordataACCEList_phone",msensordataACCEList_phone);
        logLength("convertcorrPHN2WLD_msensordataORIENList",msensordataORIENList);
        List<SensorCollect.sensordata> msensordata_worldList = new ArrayList<>();
        alignListbyTime(msensordataACCEList_phone, msensordataORIENList);
        for (int i = 0; i < msensordataACCEList_phone.size(); i++) {
            if (i < msensordataORIENList.size()) {
                Matrix3D rotMatrix = new Matrix3D();
                rotMatrix.rotateX((double) (Math.toDegrees(msensordataORIENList.get(i).getData()[0] - initOrientation[0])));
                rotMatrix.rotateY((double) (Math.toDegrees(msensordataORIENList.get(i).getData()[0] - initOrientation[0])));
                rotMatrix.rotateZ((double) (Math.toDegrees(msensordataORIENList.get(i).getData()[0] - initOrientation[0])));
                double[][] rotMatrixArray = new double[4][4];
                for (int k = 0; k < 4; k++) {
                    for (int j = 0; j < 4; j++) {
                        rotMatrixArray[i][j] = (double) rotMatrix.get(k).get(j);
                    }
                }
                float[][] result = toFloatArray(MatMultiply.multiplyByMatrix(rotMatrixArray, new double[][]{
                        {(double) msensordataACCEList_phone.get(i).getData()[0], (double) msensordataACCEList_phone.get(i).getData()[0], (double) msensordataACCEList_phone.get(i).getData()[0], 1}
                }));
                float[] data = new float[]{result[0][0],result[1][0],result[2][0]};
                msensordata_worldList.add(new SensorCollect.sensordata(msensordataACCEList_phone.get(i).getTime(),data, SensorCollect.sensordata.TYPE.ACCE_world));
            }
        }
        return msensordata_worldList;
    }

    public SensorCollect.sensordata getElementByTime_interpolate(long Time, List<SensorCollect.sensordata> msensordataList) {
        logLength("getElementByTime_interpolate",msensordataList);
        SensorCollect.sensordata toreturn_sensordata = new SensorCollect.sensordata();
        if (Time < msensordataList.get(0).getTime()) {
            toreturn_sensordata = msensordataList.get(0);
        } else if (Time > msensordataList.get(0).getTime()) {
            toreturn_sensordata = msensordataList.get(msensordataList.size()-1);
        } else {
            //generate diff list
            List<Long> TimeDiff = new ArrayList<>();
            for (SensorCollect.sensordata msensordata : msensordataList) {
                TimeDiff.add(Math.abs(Time - msensordata.getTime()));
            }
            int minIndex = TimeDiff.indexOf(Collections.min(TimeDiff));
            //interpolate
            float[] interpolatedData = new float[msensordataList.get(0).getData().length];
            for (int i = 0; i < msensordataList.get(0).getData().length; i++) {
                if (Time > msensordataList.get(minIndex).getTime()) {
                    interpolatedData[i] =
                            (msensordataList.get(minIndex).getData()[0] * Math.abs(msensordataList.get(minIndex + 1).getTime() - Time) +
                                    msensordataList.get(minIndex + 1).getData()[0] * Math.abs(msensordataList.get(minIndex).getTime() - Time)) /
                                    Math.abs(msensordataList.get(minIndex + 1).getTime() - Math.abs(msensordataList.get(minIndex).getTime()));
                } else if (Time < msensordataList.get(minIndex).getTime()) {
                    interpolatedData[i] =
                            (msensordataList.get(minIndex).getData()[0] * Math.abs(msensordataList.get(minIndex - 1).getTime() - Time) +
                                    msensordataList.get(minIndex - 1).getData()[0] * Math.abs(msensordataList.get(minIndex).getTime() - Time)) /
                                    Math.abs(msensordataList.get(minIndex - 1).getTime() - Math.abs(msensordataList.get(minIndex).getTime()));
                } else {
                    interpolatedData[i] = msensordataList.get(minIndex).getData()[i];
                }
            }
            //prepare return
            toreturn_sensordata = new SensorCollect.sensordata(Time, interpolatedData, msensordataList.get(0).getType());
        }
        return toreturn_sensordata;
    }

    public void alignListbyTime(List<SensorCollect.sensordata> sensordataList, List<SensorCollect.sensordata> msensordataList_notaligned) {
        ArrayList<SensorCollect.sensordata> msensordataList_aligned = new ArrayList<>();
        for (SensorCollect.sensordata isensordataList : sensordataList) {
            msensordataList_aligned.add(getElementByTime_interpolate(isensordataList.getTime(), msensordataList_notaligned));
        }
        msensordataList_notaligned = msensordataList_aligned;
    }

    public float[][] toFloatArray(double[][] arr) {
        if (arr == null) return null;
        int m = arr[0].length;
        int n = arr.length;
        float[][] ret = new float[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                ret[i][j] = (float)arr[i][j];
            }
        }
        return ret;
    }

    public boolean checkList(List<SensorCollect.sensordata>... ListofList){
        boolean ok= true;
        for(List<SensorCollect.sensordata> List:ListofList){
            if (List.size() <1){
                ok=false;
            }
        }
        return ok;
    }

    public void logLength(String comment,List<SensorCollect.sensordata> List){
        Log.d("DEBUG : List : ",comment+" "+String.valueOf(List.size()));
    }

}
