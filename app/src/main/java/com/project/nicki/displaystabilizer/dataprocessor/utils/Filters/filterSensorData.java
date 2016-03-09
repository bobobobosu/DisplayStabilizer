package com.project.nicki.displaystabilizer.dataprocessor.utils.Filters;

/**
 * Created by nickisverygood on 3/6/2016.
 */
public class filterSensorData {
    filterCollection mfilterCollection = new filterCollection();
    boolean kalmanfilter_boo;
    int moveingavg_sample;
    float highpass_alpha;
    float lowpass_alpha;
    boolean static_boo;

    public filterSensorData(
            boolean kalmanfilter_boo,
            int moveingavg_sample,
            float highpass_alpha,
            float lowpass_alpha,
            boolean static_boo) {
        this.kalmanfilter_boo = kalmanfilter_boo;
        this.highpass_alpha = highpass_alpha;
        this.lowpass_alpha = lowpass_alpha;
        this.moveingavg_sample = moveingavg_sample;
        this.static_boo = static_boo;
    }

    public float[] filter(float[] data) {
        data = mfilterCollection.KalmanFilter(data, kalmanfilter_boo);
        data = mfilterCollection.MovingAvgFilter(data, moveingavg_sample);
        data = mfilterCollection.HighPassFilter(data, highpass_alpha);
        //data = mfilterCollection.LowPassFilter(data, lowpass_alpha);
        data = mfilterCollection.StaticFilter(data, static_boo);
        return data;
    }
    /*
    filterCollection mfilterCollection = new filterCollection();

    private float highpass_alpha;
    private float lowpass_alpha;
    private int moveingavg_sample;
    private boolean kalmanfilter_boo;

    private boolean static_boo;


    public static class Builder {
        private float highpass_alpha ;
        private float lowpass_alpha ;
        private int moveingavg_sample ;
        private boolean kalmanfilter_boo ;
        private boolean static_boo ;

        public Builder highpass_alpha(float mhighpass_alpha) {
            this.highpass_alpha = mhighpass_alpha;
            return this;
        }
        public Builder StaticFilter(boolean static_boo) {
            this.static_boo = static_boo;
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
        static_boo = builder.static_boo;
    }

    public float[] filter(float[] data) {
        data = mfilterCollection.KalmanFilter(data, kalmanfilter_boo);
        data = mfilterCollection.MovingAvgFilter(data, moveingavg_sample);
        data = mfilterCollection.HighPassFilter(data, highpass_alpha);
        data = mfilterCollection.LowPassFilter(data, lowpass_alpha);
        data = mfilterCollection.StaticFilter(data,static_boo);
        return data;
    }
    */
}
