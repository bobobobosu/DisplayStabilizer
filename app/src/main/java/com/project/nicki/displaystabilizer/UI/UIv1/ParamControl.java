package com.project.nicki.displaystabilizer.UI.UIv1;

import android.provider.Settings;
import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.init;

/**
 * Created by nicki on 2/1/2017.
 */

public class ParamControl implements Runnable {
    public ParamControl(){

    }
    @Override
    public void run() {

        //manual control
        //params: highpass,lowpass,movingavg    target: acce, velocity, pos
        // sources: pen, ME
        //pen:
        for (float i = 0; i < 1; i += 0.5) {
            for (float j = 0; j < 1; j += 0.5) {
                for (float k = 0; k < 1; k += 0.5) {
                    long initTime = System.currentTimeMillis();
                    init.initglobalvariable.sAccelerometerLinearVal.setFilterParam(new String[]{
                            "sAccelerometerLinearVal","1",
                            String.valueOf(i),
                            String.valueOf(j),
                            String.valueOf(k),
                            "0","10000000"
                    });
                    Log.d("test","test");
                    while (System.currentTimeMillis() - initTime < 5000){

                        try {
                            new LogCSV("sAccelerometerLinearVal" + "_" +
                                    "1" + "_" +
                                    String.valueOf(i) + "_" +
                                    String.valueOf(j) + "_" +
                                    String.valueOf(k) + "_" +
                                    "0" + "_" + "10000000",
                                    String.valueOf(System.currentTimeMillis()),
                                    "",
                                    init.initglobalvariable.TouchVal[0],
                                    init.initglobalvariable.TouchVal[1],
                                    init.initglobalvariable.sAccelerometerLinearVal.getLatestData().getValues()[0],
                                    init.initglobalvariable.sAccelerometerLinearVal.getLatestData().getValues()[1],
                                    init.initglobalvariable.sAccelerometerLinearVal.getLatestData().getValues()[2]);

                        }catch (Exception ex){

                        }
                    }
                }
            }
        }

    }
}
