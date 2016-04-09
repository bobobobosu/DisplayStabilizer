
package com.project.nicki.displaystabilizer.dataprocessor;

import com.project.nicki.displaystabilizer.dataprocessor.utils.Filters.filterSensorData;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;

import java.util.List;

/**
 * Created by nickisverygood on 3/6/2016.
 */
public class calSpring {
    //init filters
    filterSensorData filtercalSpring_ACCE = new filterSensorData(true, 100, 1, 1, getAcceGyro.isStatic, Float.MAX_VALUE);
    filterSensorData filtercalSpring_dPOS = new filterSensorData(true, 1, 1, 1, getAcceGyro.isStatic, Float.MAX_VALUE);
    filterSensorData filtercalSpring_POSI = new filterSensorData(true, 100, 0.7f, 1, false, Float.MAX_VALUE);
    float[] pos = new float[]{0, 0, 0};
    float[] dpos = new float[]{0, 0, 0};
    calEular mcalEular = new calEular();
    private CircularBuffer[] mBuffer = new CircularBuffer[3];


    public calSpring(){
        for(int i=0;i<mBuffer.length;i++){
            mBuffer[i] = new CircularBuffer(50,10);
        }
    }

    public void calcList(List<SensorCollect.sensordata> mList) {
        for (int i=0;i< mList.size();i++) {
            calc(mList.get(i));
        }
    }

    public void calc(SensorCollect.sensordata sensordataIN) {
        //filter
        sensordataIN.setData(filtercalSpring_ACCE.filter(sensordataIN.getData()));
        dpos = filtercalSpring_dPOS.filter(dpos);
        pos = filtercalSpring_POSI.filter(pos);


        for(int i=0;i<mBuffer.length;i++){
            mBuffer[i].insert(sensordataIN.getData()[0]);
            dpos[i] = mBuffer[i].convolveWithH();
            pos[i] += mBuffer[i].convolveWithH();
        }

    }
}
