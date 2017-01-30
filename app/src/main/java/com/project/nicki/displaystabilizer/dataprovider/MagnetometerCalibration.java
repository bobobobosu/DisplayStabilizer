package com.project.nicki.displaystabilizer.dataprovider;

import android.util.Log;

import com.project.nicki.displaystabilizer.init;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nicki on 1/25/2017.
 */

public class MagnetometerCalibration implements Runnable {
    @Override
    public void run() {

        final List<float[]> mag_origdaat = new ArrayList<>();
        float[] dest1 = new float[3];
        float[] dest2 = new float[3];

        int ii = 0, sample_count = 0;
        float[] mag_bias = new float[]{0, 0, 0}, mag_scale = new float[]{0, 0, 0};
        float[] mag_max = new float[]{0x8000, 0x8000, 0x8000}, mag_min = {0x7FFF, 0x7FFF, 0x7FFF}, mag_temp = {0, 0, 0};

        Log.d("MAGCALI", "Mag Calibration: Wave device in a figure eight until done!");


        final int finalSample_count = 128;
        int i = 0;
        while (i < finalSample_count) {
            if (init.initglobalvariable.MagnBuffer.hasnew) {
                mag_origdaat.add(new float[]{
                        init.initglobalvariable.MagnBuffer.data.get(init.initglobalvariable.MagnBuffer.data.size() - 1)[0],
                        init.initglobalvariable.MagnBuffer.data.get(init.initglobalvariable.MagnBuffer.data.size() - 1)[1],
                        init.initglobalvariable.MagnBuffer.data.get(init.initglobalvariable.MagnBuffer.data.size() - 1)[2]
                });
            }
            i++;
        }


        for (ii = 0; ii < sample_count; ii++) {
            //MPU9250readMagData(mag_temp);  // Read the mag buffer
            mag_temp = mag_origdaat.get(ii);
            for (int jj = 0; jj < 3; jj++) {
                if (mag_temp[jj] > mag_max[jj]) mag_max[jj] = mag_temp[jj];
                if (mag_temp[jj] < mag_min[jj]) mag_min[jj] = mag_temp[jj];
            }
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get hard iron correction
        mag_bias[0] = (mag_max[0] + mag_min[0]) / 2;  // get average x mag bias in counts
        mag_bias[1] = (mag_max[1] + mag_min[1]) / 2;  // get average y mag bias in counts
        mag_bias[2] = (mag_max[2] + mag_min[2]) / 2;  // get average z mag bias in counts

        //dest1[0] = (float) mag_bias[0]*MPU9250mRes*MPU9250magCalibration[0];  // save mag biases in G for main program
        //dest1[1] = (float) mag_bias[1]*MPU9250mRes*MPU9250magCalibration[1];
        //dest1[2] = (float) mag_bias[2]*MPU9250mRes*MPU9250magCalibration[2];

        // Get soft iron correction estimate
        mag_scale[0] = (mag_max[0] - mag_min[0]) / 2;  // get average x axis max chord length in counts
        mag_scale[1] = (mag_max[1] - mag_min[1]) / 2;  // get average y axis max chord length in counts
        mag_scale[2] = (mag_max[2] - mag_min[2]) / 2;  // get average z axis max chord length in counts

        float avg_rad = mag_scale[0] + mag_scale[1] + mag_scale[2];
        avg_rad /= 3.0;

        dest2[0] = avg_rad / ((float) mag_scale[0]);
        dest2[1] = avg_rad / ((float) mag_scale[1]);
        dest2[2] = avg_rad / ((float) mag_scale[2]);


    }

    private float calculateAverage(List <Integer> marks) {
        Integer sum = 0;
        if(!marks.isEmpty()) {
            for (Integer mark : marks) {
                sum += mark;
            }
            return sum.floatValue() / marks.size();
        }
        return sum;
    }
}
