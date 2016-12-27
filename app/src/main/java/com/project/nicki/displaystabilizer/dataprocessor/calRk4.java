package com.project.nicki.displaystabilizer.dataprocessor;

import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw2;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Filters.filterSensorData;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.dataprovider.getAcceGyro;
import com.project.nicki.displaystabilizer.init;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by nickisverygood on 3/6/2016.
 */
public class calRk4 {
    static final float NS2S = 1.0f / 1000000000.0f;
    public Position[] prevPosition = new Position[3];
    //init filters
    filterSensorData filtercalRk4_ACCE = new filterSensorData(true, 10, 1, 1, init.initglobalvariable.StaticVal, Float.MAX_VALUE);
    filterSensorData filtercalRk4_VELO = new filterSensorData(true, 1, 0.7f, 1, init.initglobalvariable.StaticVal, Float.MAX_VALUE);
    filterSensorData filtercalRk4_POSI = new filterSensorData(true, 10, 0.7f, 1, init.initglobalvariable.StaticVal,0.00004f);

    long last_timestamp = 0;
    private double prevPos;
    private double deltaPos = 0;


    public calRk4() {
        for (int i = 0; i < prevPosition.length; i++) {
            prevPosition[i] = new Position(0, 0);
        }
    }

    public List<SensorCollect.sensordata> calcList(List<SensorCollect.sensordata> msensordataList) {
        List<SensorCollect.sensordata> toreturnsensordataList = new ArrayList<>();
        for (int i=0;i<msensordataList.size();i++) {
            toreturnsensordataList.add(calc(msensordataList.get(i)));
        }
        return toreturnsensordataList;
    }


    public SensorCollect.sensordata calc(final SensorCollect.sensordata msensordata) {

        //Log.d("static?", String.valueOf(getAcceGyro.isStatic + " " + String.valueOf(DemoDraw2.drawing == 0)));
        //filter update

        filtercalRk4_ACCE.paramUpdate(true, 10, 1, 1,init.initglobalvariable.StaticVal || DemoDraw3.resetted == false,1f);
        filtercalRk4_VELO.paramUpdate(true, 1, 1f, 1,init.initglobalvariable.StaticVal || DemoDraw3.resetted == false, 0.5f);
        filtercalRk4_POSI.paramUpdate(true, 10, 0.1f, 1,init.initglobalvariable.StaticVal ||DemoDraw3.resetted == false, 0.00003f);
        if (DemoDraw3.resetted == false) {
            //Log.d("reset", String.valueOf(DemoDraw3.resetted));
            for (int i = 0; i < prevPosition.length; i++) {
                prevPosition[i].v = 0;
                //prevPosition[i].pos = 0;
            }
            DemoDraw3.resetted = true;
        }

        //filter
        msensordata.setData(filtercalRk4_ACCE.filter(msensordata.getData()));

        final float[] toreurndata =new float[msensordata.getData().length];
        for (int i = 0; i < prevPosition.length; i++) {
            prevPosition[i].pos = filtercalRk4_POSI.filter(new float[]{(float) prevPosition[0].pos, (float) prevPosition[1].pos, (float) prevPosition[2].pos})[i];
            prevPosition[i].v = filtercalRk4_VELO.filter(new float[]{(float) prevPosition[0].v, (float) prevPosition[1].v, (float) prevPosition[2].v})[i];
            //Log.i("velo",String.valueOf(prevPosition[0].pos+" "+prevPosition[1].pos+ " "+prevPosition[2].pos));
            toreurndata[i] = (float)prevPosition[i].pos;
            //Log.d("stopdetect", String.valueOf(getAcceGyro.mstopdetector.getStopped(0)+" "+getAcceGyro.mstopdetector.error_threshold[0]+" "+getAcceGyro.mstopdetector.getstack()[0]+" "+getAcceGyro.mstopdetector.getstack()[1]+" "+getAcceGyro.mstopdetector.getstack()[2]));

            if(getAcceGyro.mstopdetector.getStopped(i)==true){
                prevPosition[i].v=0;
            }
        }
        SensorCollect.sensordata toreturnsensordata = new SensorCollect.sensordata(msensordata.getTime(), toreurndata, SensorCollect.sensordata.TYPE.LOCA);




/*
        new Thread(new Runnable() {
            @Override
            public void run() {
                new LogCSV(init.rk4_Log+"calRk4", String.valueOf(getAcceGyro.mstopdetector.getStopped(0)),
                        new BigDecimal(msensordata.getTime()).toPlainString(),
                        msensordata.getData()[0],
                        msensordata.getData()[1],
                        msensordata.getData()[2],
                        (float) prevPosition[0].v,
                        (float) prevPosition[1].v,
                        (float) prevPosition[2].v,
                        toreurndata[0],
                        toreurndata[1],
                        toreurndata[2]
                );
            }
        }).start();



        Log.d("RK4",String.valueOf(
                new BigDecimal(msensordata.getTime()).toPlainString()+" "+
                msensordata.getData()[0]+" "+
                msensordata.getData()[1]+" "+
                msensordata.getData()[2]+" "+
                (float) prevPosition[0].v+" "+
                (float) prevPosition[1].v+" "+
                (float) prevPosition[2].v+" "+
                toreurndata[0]+" "+
                toreurndata[1]+" "+
                toreurndata[2]));
*/

        for (int i = 0; i < 3; i++) {
            integrate(prevPosition[i], msensordata.getTime(), 0.01, msensordata.getData()[i]);
        }
        return toreturnsensordata;
    }

    public double getDeltaPos() {
        return deltaPos;
    }

    public Position integrate(Position position, long t, double dt, double acceleration) { //Heart of the RK4 integrator - I don't know what most of this is
        //save previous
        //double dt = (t - last_timestamp)/1000;
        last_timestamp = t;

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

        last_timestamp = t;
        return position;
    }

    public double acceleration(Position position, double t) {        //Calculate all acceleration here - modify as needed
        float k = 10;
        float b = 1;
        return k * position.pos +b*position.v;
    }

    public Derivative evaluate(Position initial, double t, double dt, Derivative d, double acceleration) {   //Calculate new position based on change over time
        Position position = new Position(initial.pos + d.dp * dt, initial.v + d.dv * dt);       //New state influenced by derivatives of pos and v
        position.v = acceleration(position,t+dt);
        return new Derivative(position.v, acceleration);//acceleration(position, t));   //Calculate new derivative for new position
    }
    public class Position {
        public double pos = 6;      //position
        public double v = 6;        //velocity
        public double a = 6;        //acceleration

        public Position(Position mposition) {
            this.pos = mposition.pos;
            this.v = mposition.v;
        }

        public Position(double pos, double v) {
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
