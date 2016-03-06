package com.project.nicki.displaystabilizer.dataprocessor.utils.Filters;

import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;

/**
 * Created by nickisverygood on 3/6/2016.
 */
public class filterSensorData {
    filterCollection mfilterCollection = new filterCollection();

    private float highpass_alpha;
    private float lowpass_alpha;
    private int moveingavg_sample;
    private boolean kalmanfilter_boo;

    public static class Builder {
        private float highpass_alpha = 1f;
        private float lowpass_alpha = 1f;
        private int moveingavg_sample = 1;
        private boolean kalmanfilter_boo = false;

        public Builder highpass_alpha(float mhighpass_alpha) {
            this.highpass_alpha = mhighpass_alpha;
            return this;
        }

        public Builder lowpass_alpha(float mlowpass_alpha) {
            lowpass_alpha = mlowpass_alpha;
            return this;
        }

        public Builder moveingavg_sample(int mmoveingavg_sample) {
            moveingavg_sample = mmoveingavg_sample;
            return this;
        }

        public Builder kalmanfilter_boo(boolean mkalmanfilter_boo) {
            kalmanfilter_boo = mkalmanfilter_boo;
            return this;
        }

        public filterSensorData build() {
            return new filterSensorData(this);
        }
    }

    private filterSensorData(Builder builder) {
        highpass_alpha = builder.highpass_alpha;
        lowpass_alpha = builder.lowpass_alpha;
        moveingavg_sample = builder.moveingavg_sample;
        kalmanfilter_boo = builder.kalmanfilter_boo;
    }

    public float[] filter(float[] data) {
        data = mfilterCollection.KalmanFilter(data, kalmanfilter_boo);
        data = mfilterCollection.MovingAvgFilter(data, moveingavg_sample);
        data = mfilterCollection.HighPassFilter(data, highpass_alpha);
        data = mfilterCollection.LowPassFilter(data, lowpass_alpha);
        return data;
    }
}
