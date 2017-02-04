package com.project.nicki.displaystabilizer.dataprocessor;

import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Filters.filterSensorData;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;
import com.project.nicki.displaystabilizer.init;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nicki on 1/31/2017.
 */

public class calRk4_v4 {
    static final float NS2S = 1.0f / 1000000000.0f;
    public Position[] prevPosition = new Position[3];

    long last_timestamp = 0;
    private double prevPos;
    private double deltaPos = 0;


    public calRk4_v4() {
        for (int i = 0; i < prevPosition.length; i++) {
            prevPosition[i] = new Position(System.currentTimeMillis(),0, 0);
        }
    }

    public List<SensorCollect.sensordata> calcList(List<SensorCollect.sensordata> msensordataList) {
        List<SensorCollect.sensordata> toreturnsensordataList = new ArrayList<>();
        for (int i = 0; i < msensordataList.size(); i++) {
            toreturnsensordataList.add(calc(msensordataList.get(i)));
        }
        return toreturnsensordataList;
    }


    public SensorCollect.sensordata calc(final SensorCollect.sensordata msensordata) {
        double dt = (double) (( msensordata.getTime() - last_timestamp)*NS2S);
        last_timestamp = msensordata.getTime();

        final float[] toreurndata = new float[msensordata.getData().length];
        for (int i = 0; i < prevPosition.length; i++) {
            //Log.i("velo",String.valueOf(prevPosition[0].pos+" "+prevPosition[1].pos+ " "+prevPosition[2].pos));
            toreurndata[i] = (float) prevPosition[i].pos;
        }
        Log.d("Pos",String.valueOf(prevPosition[0].pos));
        SensorCollect.sensordata toreturnsensordata = new SensorCollect.sensordata(msensordata.getTime(), toreurndata, SensorCollect.sensordata.TYPE.LOCA);


        for (int i = 0; i < 3; i++) {
            integrate(prevPosition[i], msensordata.getTime(), dt, msensordata.getData()[i]);
        }
        return toreturnsensordata;
    }

    public double getDeltaPos() {
        return deltaPos;
    }

    public Position integrate(Position position, long t, double dt, double acceleration) { //Heart of the RK4 integrator - I don't know what most of this is
        //save previous
        //double dt = (t - last_timestamp)/1000;
        //last_timestamp = t;

        Derivative a = evaluate(position, t, 0, new Derivative(0, 0), acceleration);
        Derivative b = evaluate(position, t + dt * 0.5, dt * 0.5, a, acceleration);
        Derivative c = evaluate(position, t + dt * 0.5, dt * 0.5, b, acceleration);
        Derivative d = evaluate(position, t + dt, dt, c, acceleration);

        double dpdt = 1.0 / 6.0 * (a.dp + 2.0 * (b.dp + c.dp) + d.dp);
        double dvdt = 1.0 / 6.0 * (a.dv + 2.0 * (b.dv + c.dv) + d.dv);

        position.pos += dpdt * dt;
        position.v += dvdt * dt;

        deltaPos = position.pos - prevPos;
        prevPos = position.pos;
        //Log.d("debug_rk4",String.valueOf(t)+" "+String.valueOf(dt)+" "+ String.valueOf(prevPosition[0].pos) + " " + String.valueOf(prevPosition[1].pos) + " " + String.valueOf(prevPosition[1].pos));

        //last_timestamp = t;

        return position;
    }

    public double acceleration(Position position, double t) {        //Calculate all acceleration here - modify as needed
        float k = 10;
        float b = 1;
        return k * position.pos + b * position.v;
    }

    public Derivative evaluate(Position initial, double t, double dt, Derivative d, double acceleration) {   //Calculate new position based on change over time
        Position position = new Position((long) t,initial.pos + d.dp * dt, initial.v + d.dv * dt);       //New state influenced by derivatives of pos and v
        position.v = acceleration(position, t + dt);
        return new Derivative(position.v, acceleration);//acceleration(position, t));   //Calculate new derivative for new position
    }

    public class Position {
        public double pos = 0;      //position
        public double v = 0;        //velocity
        public double a = 0;        //acceleration
        public long t = System.currentTimeMillis();

        public Position(Position mposition) {
            this.pos = mposition.pos;
            this.v = mposition.v;
        }

        public Position(long t, double pos, double v) {
            this.pos = pos;
            this.v = v;
            a = 0;
        }
    }

    public class Derivative {
        public double dp;       //change in position
        public double dv;       //change in velocity

        public Derivative(double dp, double dv) {
            this.dp = dp;
            this.dv = dv;
        }
    }


}
