package com.project.nicki.displaystabilizer.dataprocessor;

import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.dataprocessor.utils.MatMultiply;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat.Matrix3D;
//import flanagan.interpolation.*;


import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.util.MultidimensionalCounter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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
    calRk4 mcalRk4_online = new calRk4();
    calSpring mcalSpring_online = new calSpring();

    public motion_Inertial() {
    }

    public motion_Inertial(float[] initLocation, float[] initOrientation) {
        this.initLocation = initLocation;
        this.initOrientation = initOrientation;
    }


    public List<SensorCollect.sensordata> getLocationList_full(List<SensorCollect.sensordata> ACCEstorage, List<SensorCollect.sensordata> ORIENstorage) {
        Log.d("getLocationList_full","erfvwer");
        List<SensorCollect.sensordata> locationList_full = createLocationListfromLists(ACCEstorage, ORIENstorage);
        for(int i=0;i<locationList_full.size();i++){
            new LogCSV("locationList_full7","",new BigDecimal(locationList_full.get(i).getTime()).toPlainString(),
                    locationList_full.get(i).getData()[0],
                    locationList_full.get(i).getData()[1],
                    locationList_full.get(i).getData()[2]);
        }
        return locationList_full;
    }

    public List<SensorCollect.sensordata> getLocationList_online() {
        return locationList_online;
    }

    public void update(SensorCollect.sensordata msensordata) {
        if (msensordata.getType() == SensorCollect.sensordata.TYPE.ACCE) {
            if (ACCEstorage_online.size() > 1) {
                ACCEstorage_online.remove(0);
            }
            ACCEstorage_online.add(msensordata);

        }
        if (msensordata.getType() == SensorCollect.sensordata.TYPE.ORIEN_radian) {
            if (ORIENstorage_online.size() > 1) {
                ORIENstorage_online.remove(0);
            }
            ORIENstorage_online.add(msensordata);
        }
        if (ACCEstorage_online.size() > 1 && ORIENstorage_online.size() > 1) {
            //mcalEular_online.calcList(convertcorrPHN2WLD(ACCEstorage_online, ORIENstorage_online));
            //mcalRk4_online.calcList(convertcorrPHN2WLD(ACCEstorage_online, ORIENstorage_online));
            //mcalSpring_online.calcList(convertcorrPHN2WLD(ACCEstorage_online, ORIENstorage_online));
            locationList_online.add(mcalRk4_online.calc(convertcorrPHN2WLD(ACCEstorage_online, ORIENstorage_online).get(0)));
        }
        try {
            //Log.d("TESTING",String.valueOf(ACCEstorage_online.size()+" "+ORIENstorage_online.size()));
            //Log.d("TESTING","YA");
            //Log.d("TESTING", String.valueOf(locationList_online.get(0).getData()[0]));
        }catch (Exception ex){
            //Log.d("TESTING","ERROR");
            //Log.d("TESTING", String.valueOf(ex.toString()));
        }
    }


    //UTILS
    public List<SensorCollect.sensordata> createLocationListfromLists(List<SensorCollect.sensordata> ACCEList, List<SensorCollect.sensordata> ORIENList) {
        calEular mcalEular_offline = new calEular();
        calRk4 mcalRk4_offline = new calRk4();
        return mcalRk4_offline.calcList(convertcorrPHN2WLD(ACCEList, ORIENList));
    }

    public List<SensorCollect.sensordata> convertcorrPHN2WLD(List<SensorCollect.sensordata> sensordataACCEList_phone, List<SensorCollect.sensordata> msensordataORIENList) {
        List<SensorCollect.sensordata> msensordata_worldList = new ArrayList<>();
        List<SensorCollect.sensordata> msensordataACCEList_phone = new ArrayList<>(sensordataACCEList_phone);
        List<SensorCollect.sensordata> msensordataORIENList_algnd = new ArrayList<>(alignListbyTime(msensordataACCEList_phone, msensordataORIENList));
        Log.d("convertcorrPHN2WLD",msensordataACCEList_phone.size()+" "+msensordataORIENList_algnd.size());
        for (int i = 0; i < msensordataACCEList_phone.size() && i < msensordataACCEList_phone.size(); i++) {
            if (i < msensordataORIENList.size()) {
                Matrix3D rotMatrix = new Matrix3D();
                rotMatrix.rotateX((double) (Math.toDegrees(msensordataORIENList_algnd.get(i).getData()[0] - initOrientation[0])));
                rotMatrix.rotateY((double) (Math.toDegrees(msensordataORIENList_algnd.get(i).getData()[1] - initOrientation[1])));
                rotMatrix.rotateZ((double) (Math.toDegrees(msensordataORIENList_algnd.get(i).getData()[2] - initOrientation[2])));
                double[][] rotMatrixArray = new double[4][4];
                for (int k = 0; k < 4; k++) {
                    for (int j = 0; j < 4; j++) {
                        rotMatrixArray[k][j] = (double) rotMatrix.get(k).get(j);
                    }
                }

                float[][] result = toFloatArray(MatMultiply.multiplyByMatrix(rotMatrixArray, new double[][]{
                        {(double) msensordataACCEList_phone.get(i).getData()[0]},
                        {(double) msensordataACCEList_phone.get(i).getData()[1]},
                        {(double) msensordataACCEList_phone.get(i).getData()[2]},
                        {1}
                }));
                float[] data = new float[]{result[0][0], result[1][0], result[2][0]};
                msensordata_worldList.add(new SensorCollect.sensordata(msensordataACCEList_phone.get(i).getTime(), data, SensorCollect.sensordata.TYPE.ACCE_world));
                new LogCSV("phn2world1","",
                        new BigDecimal(msensordata_worldList.get(i).getTime()).toPlainString(),
                        msensordata_worldList.get(i).getData()[0],
                        msensordata_worldList.get(i).getData()[1],
                        msensordata_worldList.get(i).getData()[2]);
            }
        }
        return msensordata_worldList;
    }

    public SensorCollect.sensordata getbyTime(long Time, List<SensorCollect.sensordata> msensordataList) {
        List<SensorCollect.sensordata> msensordataListCLN = new ArrayList(msensordataList);
        List<SensorCollect.sensordata> thismsensordataList = new ArrayList<>();
        for (int i=0;i<msensordataListCLN.size();i++) {
            boolean dup = false;
            for (int j=0;j<thismsensordataList.size();j++) {
                if (thismsensordataList.get(j).getTime() == msensordataListCLN.get(i).getTime()) {
                    dup = true;
                }
            }
            if (!dup) {
                thismsensordataList.add(msensordataListCLN.get(i));
            }
        }

        //return if num==0 or 1
        if (thismsensordataList.size() == 0) {
            return new SensorCollect.sensordata(Time, new float[]{0, 0, 0}, SensorCollect.sensordata.TYPE.TOUCH);
        } else if (thismsensordataList.size() == 1) {
            return thismsensordataList.get(0);
        }else if(Time< thismsensordataList.get(0).getTime()){
            return thismsensordataList.get(0);
        }else if (Time> thismsensordataList.get(thismsensordataList.size()-1).getTime()){
            return thismsensordataList.get(thismsensordataList.size()-1);
        }



        //generate cost
        List<Long> costList = new ArrayList<>();
        for (int i = 0; i < thismsensordataList.size(); i++) {
            costList.add(Math.abs(thismsensordataList.get(i).getTime() - Time));
        }

        //sort,convert array to list, long to double
        sortbytime msortbytime = new sortbytime();
        Collections.sort(thismsensordataList,msortbytime);


        double[] TimeArray = new double[thismsensordataList.size()];
        double[][] DataArray = new double[thismsensordataList.get(0).getData().length][thismsensordataList.size()];
        double Time_smallest = new BigDecimal(thismsensordataList.get(0).getTime()).doubleValue();
        double Timed = new BigDecimal(Time).doubleValue()-Time_smallest;
        for (int i = 0; i < thismsensordataList.size(); i++) {
            TimeArray[i] = new BigDecimal(thismsensordataList.get(i).getTime()).doubleValue()-Time_smallest;
            for (int j = 0; j < thismsensordataList.get(0).getData().length; j++) {
                DataArray[j][i] = new BigDecimal(thismsensordataList.get(i).getData()[j]).doubleValue();
            }
        }

        //test
        //interpolate
        SensorCollect.sensordata msensordata_return = new SensorCollect.sensordata();
        msensordata_return.setTime(Time);
        float[] data = new float[thismsensordataList.get(0).getData().length];
        for (int j = 0; j < thismsensordataList.get(0).getData().length; j++) {
            if(thismsensordataList.size()>2){
                PolynomialSplineFunction mPS = new SplineInterpolator().interpolate(TimeArray, DataArray[j]);
                data[j] = new BigDecimal(mPS.value(Timed)).floatValue();
            }else {
                PolynomialSplineFunction mPS = new LinearInterpolator().interpolate(TimeArray, DataArray[j]);
                data[j] = new BigDecimal(mPS.value(Timed)).floatValue();
            }

        }
        msensordata_return.setData(data);

        Log.d("value", String.valueOf(data[0]));
        return msensordata_return;
    }
    class sortbytime implements Comparator<SensorCollect.sensordata> {
        @Override
        public int compare(SensorCollect.sensordata A, SensorCollect.sensordata B) {
            return new BigDecimal(A.getTime()-B.getTime()).intValue();
        }
    }
    public SensorCollect.sensordata getElementByTime_interpolate(long Time, List<SensorCollect.sensordata> msensordataList) {
        SensorCollect.sensordata toreturn_sensordata = new SensorCollect.sensordata();
        if (Time < msensordataList.get(0).getTime()) {
            toreturn_sensordata = msensordataList.get(0);
        } else if (Time > msensordataList.get(0).getTime()) {
            toreturn_sensordata = msensordataList.get(msensordataList.size() - 1);
        } else {
            //generate diff list
            List<Long> TimeDiff = new ArrayList<>();
            for (SensorCollect.sensordata msensordata : msensordataList) {
                TimeDiff.add((long) Math.abs(Time - msensordata.getTime()));
            }
            int minIndex = TimeDiff.indexOf(Collections.min(TimeDiff));
            //interpolate
            float[] interpolatedData = new float[msensordataList.get(0).getData().length];
            for (int i = 0; i < msensordataList.get(0).getData().length; i++) {
                float A, At;
                if (Time > msensordataList.get(minIndex).getTime()) {
                    interpolatedData[i] =
                            (msensordataList.get(minIndex).getData()[i] * Math.abs(msensordataList.get(minIndex + 1).getTime() - Time) +
                                    msensordataList.get(minIndex + 1).getData()[i] * Math.abs(msensordataList.get(minIndex).getTime() - Time)) /
                                    Math.abs(msensordataList.get(minIndex + 1).getTime() - Math.abs(msensordataList.get(minIndex).getTime()));
                } else if (Time < msensordataList.get(minIndex).getTime()) {
                    interpolatedData[i] =
                            (msensordataList.get(minIndex).getData()[i] * Math.abs(msensordataList.get(minIndex - 1).getTime() - Time) +
                                    msensordataList.get(minIndex - 1).getData()[i] * Math.abs(msensordataList.get(minIndex).getTime() - Time)) /
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

    public ArrayList<SensorCollect.sensordata> alignListbyTime(List<SensorCollect.sensordata> sensordataList, List<SensorCollect.sensordata> msensordataList_notaligned) {
        ArrayList<SensorCollect.sensordata> msensordataList_aligned = new ArrayList<>();
        for (int i=0;i<sensordataList.size();i++) {
            msensordataList_aligned.add(new SensorCollect.sensordata(getbyTime(sensordataList.get(i).getTime(), msensordataList_notaligned)));
        }

        return msensordataList_aligned;
    }

    public float[][] toFloatArray(double[][] arr) {
        if (arr == null) return null;
        int m = arr[0].length;
        int n = arr.length;
        float[][] ret = new float[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                ret[i][j] = (float) arr[i][j];
            }
        }
        return ret;
    }

    public boolean checkList(List<SensorCollect.sensordata>... ListofList) {
        boolean ok = true;
        for (List<SensorCollect.sensordata> List : ListofList) {
            if (List.size() < 5) {
                ok = false;
                return ok;
            }
        }
        return ok;
    }

    public void logLength(String comment, List<SensorCollect.sensordata> List) {
        Log.d("DEBUG : List : ", comment + " " + String.valueOf(List.size()));
    }


}
