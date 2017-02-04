package com.project.nicki.displaystabilizer.dataprocessor;

import android.util.Log;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by nicki on 2/1/2017.
 */

public class Interpolation {
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
}
