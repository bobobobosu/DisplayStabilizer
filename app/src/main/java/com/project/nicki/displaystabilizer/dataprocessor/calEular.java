
package com.project.nicki.displaystabilizer.dataprocessor;

import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.utils.Filters.filterSensorData;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nickisverygood on 3/6/2016.
 */
public class calEular {
    static final float NS2S = 1.0f / 1000000000.0f;
    ;
    //init filters
    filterSensorData filtercalEular_ACCE = new filterSensorData(true, 100, 1, 1, getAcceGyro.isStatic);
    filterSensorData filtercalEular_VELO = new filterSensorData(true, 1, 1, 1, getAcceGyro.isStatic);
    filterSensorData filtercalEular_POSI = new filterSensorData(true, 100, 0.7f, 1, getAcceGyro.isStatic);
    float[] last_values;
    float[] velocity = new float[]{0, 0, 0};
    float[] position = new float[]{0, 0, 0};
    List<SensorCollect.sensordata> locationList = new ArrayList<>();
    long last_timestamp = 0;

    public List<SensorCollect.sensordata> calcList(List<SensorCollect.sensordata> msensordataList){
        List<SensorCollect.sensordata> LocationList = new ArrayList<>();
        for (int i =0;i<msensordataList.size();i++){
            LocationList.add(calc(msensordataList.get(i)));
        }
        return LocationList;
    }
    public SensorCollect.sensordata calc(SensorCollect.sensordata msensordata_world) {
        //Filter
        msensordata_world.setData(filtercalEular_ACCE.filter(msensordata_world.getData()));
        velocity = filtercalEular_VELO.filter(velocity);
        position = filtercalEular_POSI.filter(position);
        Log.d("calEuler", String.valueOf(locationList.size()));
        new LogCSV("calEular5","",new BigDecimal(last_timestamp).toPlainString(),
                position[0],
                position[1],
                position[2]);
        SensorCollect.sensordata toreturnfilteresList =new SensorCollect.sensordata(msensordata_world.getTime(),position, SensorCollect.sensordata.TYPE.LOCA);

        if (last_values != null) {
            float dt = (msensordata_world.getTime() - last_timestamp) * NS2S;

            for (int index = 0; index < 3; ++index) {
                velocity[index] += (msensordata_world.getData()[index] + last_values[index]) / 2 * dt;
                position[index] += velocity[index] * dt;
            }
        } else {
            last_values = new float[3];
            velocity = new float[3];
            position = new float[3];
            velocity[0] = velocity[1] = velocity[2] = 0f;
            position[0] = position[1] = position[2] = 0f;
        }
        System.arraycopy(msensordata_world.getData(), 0, last_values, 0, 3);
        last_timestamp = msensordata_world.getTime();
        locationList.add(new SensorCollect.sensordata(msensordata_world.getTime(),position, SensorCollect.sensordata.TYPE.LOCA));
        return toreturnfilteresList;
    }

}

