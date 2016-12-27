package com.project.nicki.displaystabilizer.stabilization;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.MotionEstimation3;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect.sensordata;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nickisverygood on 1/3/2016.
 */
public class stabilize_v3_1 {
    public static Handler getDraw;

    //constants
    private static float cX;
    private static float cY;
    //mods
    public boolean CalibrationMode = true;

    //buffers
    ArrayList<sensordata> strokebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> strokedeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posbuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posdeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> stastrokebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> stastrokedeltabuffer = new ArrayList<sensordata>();
    //tmps
    long prevTime = 0;
    float[] prevStroke = null;
    boolean drawSTATUS = false;
    boolean prevdrawSTATUS = false;
    boolean init_yesno = false;
    sensordata tmpaccesensordata;
    sensordata tmporiensensordata;
    float[] Pos = null;
    double[] prevQuaternion = null;
    double[] currQuaternion = null;
    double[][] currRot = null;
    int deltaingStatus = 0;
    sensordata tmp1accesensordata;
    //init_yesno specific
    float[] orieninit;


    public stabilize_v3_1() {
    }


    public void set_Sensor(final Bundle bundlegot) {
        try {
            Pos = bundlegot.getFloatArray("Pos");
            currQuaternion = bundlegot.getDoubleArray("Quaternion");

            tmpaccesensordata = new sensordata(bundlegot.getLong("Time"), bundlegot.getFloatArray("Pos"));
            tmporiensensordata = new sensordata(bundlegot.getLong("Time"), bundlegot.getFloatArray("Orien"));
            if (DemoDraw3.orienreset == false) {
                orieninit = tmporiensensordata.getData();
                DemoDraw3.orienreset = true;
            }

            prevdrawSTATUS = drawSTATUS;
            drawSTATUS = DemoDraw3.drawing < 2;
            if (tmp1accesensordata != null ) {
                float[] delta = new float[]{
                        tmpaccesensordata.getData()[0] - tmp1accesensordata.getData()[0],
                        tmpaccesensordata.getData()[1] - tmp1accesensordata.getData()[1]};
                if (currRot != null) {
                    SimpleMatrix delta_m = new SimpleMatrix(new double[][]{{(double) delta[0]}, {(double) delta[1]}, {0}});
                    SimpleMatrix rot_m = new SimpleMatrix(currRot);
                    delta_m.mult(rot_m);
                    delta = new float[]{(float)delta_m.getMatrix().get(0,0),(float)delta_m.getMatrix().get(1,0)};
                }


                tmpaccesensordata = new sensordata(tmp1accesensordata.getTime(), delta);
                tmp1accesensordata = tmpaccesensordata;
            }

            if (prevQuaternion != null) {
                Quaternion q_currQuaternion = new Quaternion(currQuaternion[0],currQuaternion[1],currQuaternion[2],currQuaternion[3]);
                Quaternion q_prevQuaternion = new Quaternion(prevQuaternion[0],prevQuaternion[1],prevQuaternion[2],prevQuaternion[3]);
                //Quaternion q_currQuaternion = new Quaternion(1,0,0,0);
                //Quaternion q_prevQuaternion = new Quaternion(0.7358881620051946,0.2889093508708513,0.45508140685231796,0.40975713921457785);
                Quaternion q_currminusprev = q_currQuaternion.multiply(q_prevQuaternion.getInverse());
                Rotation convert2rot = new Rotation(q_currminusprev.getQ0(), q_currminusprev.getQ1(), q_currminusprev.getQ2(), q_currminusprev.getQ3(), false);
                currRot = convert2rot.getMatrix();
                //currRot = MotionEstimation3.currRot;
            }
            prevQuaternion = currQuaternion.clone();


            if (prevdrawSTATUS == false && drawSTATUS == true || init_yesno == false) {
                orieninit = tmporiensensordata.getData();

                if (CalibrationMode == true && strokebuffer.size() > 1 && posbuffer.size() > 1) {
                    CalibrationMode = false;
                }
                strokebuffer = new ArrayList<>();
                strokedeltabuffer = new ArrayList<>();
                posbuffer = new ArrayList<>();
                posdeltabuffer = new ArrayList<>();
                stastrokebuffer = new ArrayList<>();
                stastrokedeltabuffer = new ArrayList<>();
                prevTime = 0;
                prevStroke = null;
                tmp1accesensordata = null;
                init_yesno = true;
                tmpaccesensordata = null;
                deltaingStatus = 0;
            }

        } catch (Exception ex) {
            //Log.e("set_Sensor",String.valueOf(ex));
        }

    }

    public ArrayList<sensordata> gen_Draw(final Bundle bundlegot) {


        //try {
        if (DemoDraw3.drawing == 0) {
            orieninit = tmporiensensordata.getData();
        }
        if (tmpaccesensordata == null || Pos == null) {
            return null;
        }
        prevdrawSTATUS = drawSTATUS;
        drawSTATUS = DemoDraw3.drawing < 2;

        if (prevdrawSTATUS == false && drawSTATUS == true || init_yesno == false) {


            if (CalibrationMode == true && strokebuffer.size() > 1 && posbuffer.size() > 1) {
                CalibrationMode = false;
            }
            strokebuffer = new ArrayList<>();
            strokedeltabuffer = new ArrayList<>();
            posbuffer = new ArrayList<>();
            posdeltabuffer = new ArrayList<>();
            stastrokebuffer = new ArrayList<>();
            stastrokedeltabuffer = new ArrayList<>();
            prevTime = 0;
            prevStroke = null;

            init_yesno = true;
            tmpaccesensordata = null;


        }


        //manual control
        cX = -150f;
        cY = 150f;


        //buffer Draw
        strokebuffer.add(bundle2sensordata(bundlegot));
        if (strokebuffer.size() > 1) {
            strokedeltabuffer.add(getlatestdelta(strokebuffer));
        }


        if (tmpaccesensordata != null) {
            //rotate position vector
            Log.i("touch",String.valueOf(strokebuffer.get(strokebuffer.size()-1).getData()[0]+" "+strokebuffer.get(strokebuffer.size()-1).getData()[1]));
            if(strokebuffer.get(strokebuffer.size()-1)!=null && tmpaccesensordata.getData()[0]!=0){
                posdeltabuffer.add(multiply_multiplier_caused_by_rotation(strokebuffer.get(strokebuffer.size()-1).getData(),tmpaccesensordata));
            }else {
                posdeltabuffer.add(tmpaccesensordata);
            }
           // posdeltabuffer.add(tmpaccesensordata);

            tmpaccesensordata = null;
        }

        //get stabilized  result
        if (strokedeltabuffer.size() > 0 && posdeltabuffer.size() > 0) {
            //generate stabilize vector
            sensordata stastrokedelta = new sensordata();
            stastrokedelta.setTime(strokedeltabuffer.get(strokedeltabuffer.size() - 1).getTime());
            stastrokedelta.setData(new float[]{
                    strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[0] - posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0] * cX / (float) com.project.nicki.displaystabilizer.init.pix2m,
                    strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[1] - posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[1] * cY / (float) com.project.nicki.displaystabilizer.init.pix2m});
            stastrokedeltabuffer.add(stastrokedelta);

            if (prevStroke != null) {
                //generate stabilized point
                float[] delta = new float[]{
                        stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[0],
                        stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[1]};
                if (init.initglobalvariable.RotationVal != null ) {
                    SimpleMatrix delta_m = new SimpleMatrix(new double[][]{{(double) delta[0]}, {(double) delta[1]}, {0}});
                    SimpleMatrix rot_m = new SimpleMatrix(init.initglobalvariable.RotationVal).invert();
                    //rot_m = new SimpleMatrix(new double[][]{{0.866,-0.5,0},{0.5,0.866,0},{0,0,1}});
                    SimpleMatrix fin = rot_m.mult(delta_m);
                    delta = new float[]{(float)fin.getMatrix().get(0,0),(float)fin.getMatrix().get(1,0)};
                    /*
                    float[] deltae = new float[]{(float)fine.getMatrix().get(0,0),(float)fine.getMatrix().get(1,0)};
                    new LogCSV(init.rk4_Log + " stade", String.valueOf(getAcceGyro.mstopdetector.getStopped(0)),
                            String.valueOf(System.currentTimeMillis()),
                            deltae[0],
                            deltae[1]
                    );*/
                }
                prevStroke[0] += delta[0];
                prevStroke[1] += delta[1];
                stastrokebuffer.add(new sensordata(stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getTime(), prevStroke));


                init.initTouchCollection.sta_Online_raw.add(new sensordata(stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getTime(), prevStroke));

                //stick to finger_array
                float tofinger[] = new float[]{strokebuffer.get(strokebuffer.size() - 1).getData()[0] - prevStroke[0],
                        strokebuffer.get(strokebuffer.size() - 1).getData()[1] - prevStroke[1]};
                ArrayList<sensordata> r_stastrokebuffer = new ArrayList<>();

                //init.initTouchCollection.sta_Online_todraw_stroke = new ArrayList<>();


                List<stabilize_v3.Point> tofingerList = new ArrayList<>();
                for (int i = 0; i < stastrokebuffer.size(); i++) {
                    sensordata tmp = new sensordata(strokebuffer.get(i));
                    tmp.setData(new float[]{
                            stastrokebuffer.get(i).getData()[0] + tofinger[0],
                            stastrokebuffer.get(i).getData()[1] + tofinger[1]
                    });
                    tmp.setTime(stastrokebuffer.get(i).getTime());

                    stabilize_v3.Point todraw = new stabilize_v3.Point();
                    todraw.setX(stastrokebuffer.get(i).getData()[0] + tofinger[0]);
                    todraw.setY(stastrokebuffer.get(i).getData()[1] + tofinger[1]);
                    tofingerList.add(todraw);

                    r_stastrokebuffer.add(tmp);

                }

                // DemoDraw3.pending_to_draw_direct = tofingerList;

                return r_stastrokebuffer;


            } else {
                ArrayList<sensordata> r_stastrokebuffer = new ArrayList<>();
                prevStroke = new float[]{
                        strokebuffer.get(0).getData()[0],
                        strokebuffer.get(0).getData()[1]};
                prevStroke = new float[]{0, 0};
                r_stastrokebuffer.add(new sensordata(strokebuffer.get(0).getTime(), prevStroke));
                return r_stastrokebuffer;

            }


        }

        //}catch (Exception ex){
        //    ex.printStackTrace();
        //    Log.e("gen_Draw",String.valueOf(ex));
        //}

        return null;
    }


    private sensordata bundle2sensordata(Bundle bundle) {
        sensordata returesensordata = new sensordata();
        for (String keys : bundle.keySet()) {
            if (keys == "Time") {
                returesensordata.setTime(bundle.getLong(keys));
            } else {
                returesensordata.setData(bundle.getFloatArray(keys));
            }
        }
        return returesensordata;
    }

    private sensordata getlatestdelta(ArrayList<sensordata> strokebuffer) {
        sensordata msensordata = new sensordata();
        //compute delta
        float[] deltaFloat = new float[strokebuffer.get(0).getData().length];
        for (int i = 0; i < strokebuffer.get(0).getData().length; i++) {
            deltaFloat[i] = (strokebuffer.get(strokebuffer.size() - 1).getData()[i] - strokebuffer.get(strokebuffer.size() - 2).getData()[i]);
        }
        msensordata.setTime(strokebuffer.get(strokebuffer.size() - 1).getTime());
        msensordata.setData(deltaFloat);
        return msensordata;
    }


    //Process delta:　multiplier caused by rotation
    private sensordata multiply_multiplier_caused_by_rotation(float[] prevStroke, sensordata acce){
        sensordata toreturn = new sensordata(acce);

        //returnvec = [accevector] + [Rotationvector][touch relative to center] -  [touch relative to center]
        float[] accevector = acce.getData();
        float[] touch_relative_to_center = new float[]{prevStroke[0]-691,prevStroke[1]-905};
        //[Rotationvector][touch relative to center]
        SimpleMatrix Rot_mult_rev = new SimpleMatrix(currRot).mult(new SimpleMatrix(new double[][]{{touch_relative_to_center[0]},{touch_relative_to_center[1]},{0}}));
        //Rot_mult_rev = new SimpleMatrix(new double[][]{{0.866,-0.5,0},{0.5,0.866,0},{0,0,1}}).mult(new SimpleMatrix(new double[][]{{touch_relative_to_center[0]},{touch_relative_to_center[1]},{0}}));
        //Rot_mult_rev = new SimpleMatrix(new double[][]{{1d,0d,0d},{0d,1d,0d},{0d,0d,1d}}).mult(new SimpleMatrix(new double[][]{{touch_relative_to_center[0]},{touch_relative_to_center[1]},{0}}));
        toreturn.setData(new float[]{
                (float) (accevector[0]+((float) Rot_mult_rev.get(0,0)-touch_relative_to_center[0])*0.0000001),
                (float) (accevector[1]+((float) Rot_mult_rev.get(1,0)-touch_relative_to_center[1])*0.0000001),
        });


        /*
        SimpleMatrix Rot_mult_rev = new SimpleMatrix(currRot).mult(new SimpleMatrix(new double[][]{{acce.getData()[0]},{acce.getData()[1]},{0}}));
        toreturn.setData(new float[]{
                (float) Rot_mult_rev.get(0,0),
                (float) Rot_mult_rev.get(1,0),
        });
        */
        return toreturn;
    }


}
