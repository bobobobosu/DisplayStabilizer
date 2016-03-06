
package com.project.nicki.displaystabilizer.dataprocessor;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.utils.Filters.filterSensorData;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v2;

import java.nio.Buffer;
import java.util.List;

/**
 * Created by nickisverygood on 3/6/2016.
 */
public class calSpring {
    //init filters
    filterSensorData filtercalSpring_ACCE = new filterSensorData.Builder().build();
    filterSensorData filtercalSpring_dPOS = new filterSensorData.Builder().build();
    filterSensorData filtercalSpring_POSI = new filterSensorData.Builder().build();

    private CircularBuffer[] mBuffer = new CircularBuffer[3];
    float[] pos = new float[]{0, 0,0};
    float[] dpos = new float[]{0,0,0};
    public calSpring(){
        for(int i=0;i<mBuffer.length;i++){
            mBuffer[i] = new CircularBuffer(50,10);
        }
    }


    calEular mcalEular = new calEular();

    public void calcList(List<SensorCollect.sensordata> mList) {
        for (SensorCollect.sensordata isensordata : mList) {
            calc(isensordata);
        }
    }

    public void calc(SensorCollect.sensordata sensordataIN) {
        //filter
        sensordataIN.setData(filtercalSpring_ACCE.filter(sensordataIN.getData()));
        dpos = filtercalSpring_dPOS.filter(dpos);
        pos = filtercalSpring_POSI.filter(pos);

        LogCSV.LogCSV("debug17",dpos[0],dpos[1],dpos[2],pos[0],pos[1],pos[2]);

        for(int i=0;i<mBuffer.length;i++){
            mBuffer[i].insert(sensordataIN.getData()[0]);
            dpos[i] = mBuffer[i].convolveWithH();
            pos[i] += mBuffer[i].convolveWithH();
        }

    }
}
