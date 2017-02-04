package com.project.nicki.displaystabilizer.stabilization;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.dataprovider.representation.MatrixF4x4;
import com.project.nicki.displaystabilizer.dataprovider.representation.Vector3f;
import com.project.nicki.displaystabilizer.globalvariable;
import com.project.nicki.displaystabilizer.init;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nicki on 1/31/2017.
 */

public class stabilize_v3_1_func {
    public static Handler getDraw;

    //// # For Translation Only
    public TranslationProvider mTP = new TranslationProvider();

    //constants
    private static float cX;
    private static float cY;
    //mods
    public boolean CalibrationMode = true;

    //buffers
    ArrayList<SensorCollect.sensordata> strokebuffer = new ArrayList<SensorCollect.sensordata>();
    ArrayList<SensorCollect.sensordata> strokedeltabuffer = new ArrayList<SensorCollect.sensordata>();
    ArrayList<SensorCollect.sensordata> posbuffer = new ArrayList<SensorCollect.sensordata>();
    ArrayList<SensorCollect.sensordata> posdeltabuffer = new ArrayList<SensorCollect.sensordata>();
    ArrayList<SensorCollect.sensordata> stastrokebuffer = new ArrayList<SensorCollect.sensordata>();
    ArrayList<SensorCollect.sensordata> stastrokedeltabuffer = new ArrayList<SensorCollect.sensordata>();
    //tmps
    long prevTime = 0;
    public float[] prevStroke = null;
    boolean drawSTATUS = false;
    boolean prevdrawSTATUS = false;
    boolean init_yesno = false;
    SensorCollect.sensordata tmpaccesensordata;
    SensorCollect.sensordata tmporiensensordata;
    float[] Pos = null;
    SimpleMatrix initQua_m = new SimpleMatrix(3, 3);
    Quaternion q_initQua;
    float[] prevQuaternion = init.initglobalvariable.sQuaternionVal.getLatestData().getValues();
    float[] currQuaternion = init.initglobalvariable.sQuaternionVal.getLatestData().getValues();
    ;
    double[][] currRot = null;
    int deltaingStatus = 0;
    SensorCollect.sensordata tmp1accesensordata;
    //init_yesno specific
    float[] orieninit;


    public stabilize_v3_1_func() {
    }


    public void set_Sensor(final Bundle bundlegot) {
        try {
            Pos = bundlegot.getFloatArray("Pos");
            //currQuaternion = bundlegot.getDoubleArray("Quaternion");

            tmpaccesensordata = new SensorCollect.sensordata(bundlegot.getLong("Time"), bundlegot.getFloatArray("Pos"));
            tmporiensensordata = new SensorCollect.sensordata(bundlegot.getLong("Time"), bundlegot.getFloatArray("Orien"));
            if (DemoDraw3.orienreset == false) {
                orieninit = tmporiensensordata.getData();
                DemoDraw3.orienreset = true;
            }

            prevdrawSTATUS = drawSTATUS;
            drawSTATUS = DemoDraw3.drawing < 2;
            if (tmp1accesensordata != null) {
                float[] delta = new float[]{
                        tmpaccesensordata.getData()[0] - tmp1accesensordata.getData()[0],
                        tmpaccesensordata.getData()[1] - tmp1accesensordata.getData()[1]};
                if (currRot != null) {
                    SimpleMatrix delta_m = new SimpleMatrix(new double[][]{{(double) delta[0]}, {(double) delta[1]}, {0}});
                    SimpleMatrix rot_m = new SimpleMatrix(currRot);
                    delta_m.mult(rot_m);
                    delta = new float[]{(float) delta_m.getMatrix().get(0, 0), (float) delta_m.getMatrix().get(1, 0)};
                }


                tmpaccesensordata = new SensorCollect.sensordata(tmp1accesensordata.getTime(), delta);
                tmp1accesensordata = tmpaccesensordata;
            }

            /////////////////////////////////RESET/////////////////////////////////
            if (prevdrawSTATUS == false && drawSTATUS == true || init_yesno == false) {
                //init.initglobalvariable.sQuaternionVal.resetinit();
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
            //////////////////////////////////////////////////////////////////

        } catch (Exception ex) {
            //Log.e("set_Sensor",String.valueOf(ex));
        }

    }

    public ArrayList<SensorCollect.sensordata> gen_Draw(final Bundle bundlegot) {

        /////////////////////////////////RESET/////////////////////////////////
        //try {
        if (DemoDraw3.drawing == 0) {
            orieninit = tmporiensensordata.getData();
        }
        if (tmpaccesensordata == null || Pos == null) {
            return null;
        }
        prevdrawSTATUS = drawSTATUS;
        drawSTATUS = DemoDraw3.drawing < 2;
        if (prevdrawSTATUS == false & drawSTATUS == true || init_yesno == false) {
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
        //////////////////////////////////////////////////////////////////


        //manual control
        cX = -150f;
        cY = 150f;


        //buffer Draw
        strokebuffer.add(centerlizeTouch(bundle2sensordata(bundlegot)));
        if (strokebuffer.size() > 1) {
            strokedeltabuffer.add(getlatestdelta(strokebuffer));
        }


        if (tmpaccesensordata != null) {
            /*\
            //rotate position vector
            Log.i("touch", String.valueOf(strokebuffer.get(strokebuffer.size() - 1).getData()[0] + " " + strokebuffer.get(strokebuffer.size() - 1).getData()[1]));
            if (strokebuffer.get(strokebuffer.size() - 1) != null && tmpaccesensordata.getData()[0] != 0) {
                posdeltabuffer.add(multiply_multiplier_caused_by_rotation(strokebuffer.get(strokebuffer.size() - 1).getData(), tmpaccesensordata));
            } else {
                posdeltabuffer.add(tmpaccesensordata);
            }
            */
            posdeltabuffer.add(tmpaccesensordata);
            tmpaccesensordata = null;


        }

        ////cal strokedelta
        //generate delta rotation
        if (strokebuffer.size() > 1 && prevQuaternion != null) {
            currQuaternion = init.initglobalvariable.sQuaternionVal.getLatestData().getValues();

            //Override
            //prevQuaternion = new float[]{1, 0, 0, 0};
            //currQuaternion = new float[]{(float) 0.924, 0, 0, (float) 0.383};
            //strokebuffer.get(strokebuffer.size() - 1).setData(new float[]{-1, -1});
            //strokebuffer.get(strokebuffer.size() - 2).setData(new float[]{1, 1});


            Quaternion q_currQuaternion = new Quaternion(currQuaternion[0], currQuaternion[1], currQuaternion[2], currQuaternion[3]);
            q_currQuaternion = q_currQuaternion.multiply(q_initQua.getInverse());
            //com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion q_curr = new com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion();
            //q_curr.setXYZW(currQuaternion[1], currQuaternion[2], currQuaternion[3], currQuaternion[0]);
            //MatrixF4x4 m_curr = q_curr.getMatrix4x4();
            //double[][] currQuaternionRot = new double[][]{
            //        {m_curr.getMatrix()[0], m_curr.getMatrix()[1], m_curr.getMatrix()[2]},
             //       {m_curr.getMatrix()[4], m_curr.getMatrix()[5], m_curr.getMatrix()[6]},
             //       {m_curr.getMatrix()[8], m_curr.getMatrix()[9], m_curr.getMatrix()[10]}};
            //SimpleMatrix currQuaternionRot_m = new SimpleMatrix(currQuaternionRot);


            Quaternion q_prevQuaternion = new Quaternion(prevQuaternion[0], prevQuaternion[1], prevQuaternion[2], prevQuaternion[3]);
            q_prevQuaternion = q_prevQuaternion.multiply(q_initQua.getInverse());
            //com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion q = new com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion();
            //q.setXYZW(prevQuaternion[1], prevQuaternion[2], prevQuaternion[3], prevQuaternion[0]);
            //MatrixF4x4 m = q.getMatrix4x4();
            //double[][] prevQuaternionRot = new double[][]{
            //        {m.getMatrix()[0], m.getMatrix()[1], m.getMatrix()[2]},
            //        {m.getMatrix()[4], m.getMatrix()[5], m.getMatrix()[6]},
            //        {m.getMatrix()[8], m.getMatrix()[9], m.getMatrix()[10]}
           // };
            //SimpleMatrix prevQuaternionRot_m = new SimpleMatrix(prevQuaternionRot);

            Quaternion q_currminusprev = q_prevQuaternion.getInverse().multiply(q_currQuaternion);

            /*
            com.project.nicki.displaystabilizer.dataprocessor.utils.Quaternion q_currminusprev2
                    = new com.project.nicki.displaystabilizer.dataprocessor.utils.Quaternion(
                    q_currminusprev.getQ0(),
                    q_currminusprev.getQ1(),
                    q_currminusprev.getQ2(),
                    q_currminusprev.getQ3());
            com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion q_currminusprev3 = new com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion();
*/
            Rotation convert2rot = new Rotation(q_currminusprev.getQ0(), q_currminusprev.getQ1(), q_currminusprev.getQ2(), q_currminusprev.getQ3(), false);
            Log.d("Rotation", String.valueOf(convert2rot.getAngle()));
            currRot = convert2rot.getMatrix();
            SimpleMatrix currTouchVec = new SimpleMatrix(new double[][]{
                    {strokebuffer.get(strokebuffer.size() - 1).getData()[0]},
                    {strokebuffer.get(strokebuffer.size() - 1).getData()[1]},
                    {strokebuffer.get(strokebuffer.size() - 1).getData()[2]}
            });
            SimpleMatrix prevTouchVec = new SimpleMatrix(new double[][]{
                    {strokebuffer.get(strokebuffer.size() - 2).getData()[0]},
                    {strokebuffer.get(strokebuffer.size() - 2).getData()[1]},
                    {strokebuffer.get(strokebuffer.size() - 2).getData()[2]}
            });
            SimpleMatrix currRot_m = new SimpleMatrix(currRot).transpose();//.mult(currQuaternionRot_m.mult(initQua_m.invert()));


            //convert touch to world coordinate
            //currTouchVec = currQuaternionRot_m.mult(currTouchVec);
            //prevTouchVec = prevQuaternionRot_m.mult(prevTouchVec);

            SimpleMatrix rotate_current = currRot_m.mult(currTouchVec);
            SimpleMatrix minus_prev = rotate_current.minus(prevTouchVec);

            //SimpleMatrix rotate_to_world = currRot_m.mult(initQua_m.invert()).mult(minus_prev);
            //SimpleMatrix rotate_to_world = currQuaternionRot_m.mult(initQua_m.invert()).mult(minus_prev);

            SimpleMatrix rotate_to_world;
            rotate_to_world = minus_prev;
            // rotate_to_world = currQuaternionRot_m.invert().mult(rotate_to_world);
            // rotate_to_world = initQua_m.invert().mult(rotate_to_world);

            //SimpleMatrix strokedelta_m = new SimpleMatrix(currQuaternionRot).mult(currRot_m.mult(currTouchVec).minus(prevTouchVec));
            Log.d("results", String.valueOf(rotate_to_world.get(2, 0)));

            //convert result to phone coordinate
            // rotate_to_world = currQuaternionRot_m.invert().mult(rotate_to_world);



            strokedeltabuffer.add(new SensorCollect.sensordata(
                    strokebuffer.get(strokebuffer.size() - 1).getTime(),
                    new float[]{(float) rotate_to_world.get(0, 0), (float) rotate_to_world.get(1, 0), (float) rotate_to_world.get(2, 0)}
            ));/*
            strokedeltabuffer.add(new SensorCollect.sensordata(strokebuffer.get(strokebuffer.size() - 1).getTime(),new float[]{
                    (float) (currTouchVec.get(0,0) - prevTouchVec.get(0,0)),
                    (float) (currTouchVec.get(1,0) - prevTouchVec.get(1,0)),
            }));*/


        }

        prevQuaternion = currQuaternion;
        //get stabilized  result
        if (strokedeltabuffer.size() > 0 && posdeltabuffer.size() > 0) {
            //generate stabilize vector
            SensorCollect.sensordata stastrokedelta = new SensorCollect.sensordata();
            stastrokedelta.setTime(strokedeltabuffer.get(strokedeltabuffer.size() - 1).getTime());
            //stastrokedelta.setData(new float[]{
            //        strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[0]+ posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0] * cX / (float) com.project.nicki.displaystabilizer.init.pix2m,
            //       strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[1]+ posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[1] * cY / (float) com.project.nicki.displaystabilizer.init.pix2m});

            //float[] translation = mTP.TranslationOnInitPlane(new Quaternion(currQuaternion[0],currQuaternion[1],currQuaternion[2],currQuaternion[3]),
            //        q_initQua,stastrokedelta.getTime(),
            //        stastrokebuffer.size());
            float[] translation = mTP.TranslationOnInitPlane(
                    new Quaternion(currQuaternion[0],currQuaternion[1],currQuaternion[2],currQuaternion[3]),
                    q_initQua,
                    stastrokedelta.getTime(),
                    stastrokebuffer.size());
            stastrokedelta.setData(new float[]{
                    strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[0] +translation[0],
                    strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[1] +translation[0]});


            stastrokedeltabuffer.add(stastrokedelta);

            if (prevStroke != null) {
                //generate stabilized point
                float[] delta = new float[]{
                        stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[0],
                        stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[1]};

                /*
                if (init.initglobalvariable.RotationVal != null) {
                    SimpleMatrix delta_m = new SimpleMatrix(new double[][]{{(double) delta[0]}, {(double) delta[1]}, {0}});
                    SimpleMatrix rot_m = new SimpleMatrix(init.initglobalvariable.RotationVal).invert();
                    SimpleMatrix fin = rot_m.mult(delta_m);
                    delta = new float[]{(float) fin.getMatrix().get(0, 0), (float) fin.getMatrix().get(1, 0)};
                }
                    */
                prevStroke[0] += delta[0];
                prevStroke[1] += delta[1];
                stastrokebuffer.add(new SensorCollect.sensordata(stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getTime(), prevStroke));


                init.initTouchCollection.sta_Online_raw.add(new SensorCollect.sensordata(stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getTime(), prevStroke));

                //stick to finger_array
                float tofinger[] = new float[]{strokebuffer.get(strokebuffer.size() - 1).getData()[0] - prevStroke[0],
                        strokebuffer.get(strokebuffer.size() - 1).getData()[1] - prevStroke[1]};
                ArrayList<SensorCollect.sensordata> r_stastrokebuffer = new ArrayList<>();

                //init.initTouchCollection.sta_Online_todraw_stroke = new ArrayList<>();


                List<stabilize_v3.Point> tofingerList = new ArrayList<>();
                for (int i = 0; i < stastrokebuffer.size(); i++) {
                    SensorCollect.sensordata tmp = new SensorCollect.sensordata(strokebuffer.get(i));
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

                return viewlizeTouch(r_stastrokebuffer);


            } else {
                ArrayList<SensorCollect.sensordata> r_stastrokebuffer = new ArrayList<>();
                prevStroke = new float[]{
                        strokebuffer.get(0).getData()[0],
                        strokebuffer.get(0).getData()[1]};
                prevStroke = new float[]{0, 0};
                r_stastrokebuffer.add(new SensorCollect.sensordata(strokebuffer.get(0).getTime(), prevStroke));
                return viewlizeTouch(r_stastrokebuffer);

            }


        }

        //}catch (Exception ex){
        //    ex.printStackTrace();
        //    Log.e("gen_Draw",String.valueOf(ex));
        //}

        return null;
    }

    public void initQuaternionReset() {
        ////init Qua
        globalvariable.SensorData.Data initQua_Array = init.initglobalvariable.sQuaternionVal.getLatestData();
        com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion initQua = new com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion();
        q_initQua = new Quaternion(
                (double) initQua_Array.getValues()[0],
                (double) initQua_Array.getValues()[1],
                (double) initQua_Array.getValues()[2],
                (double) initQua_Array.getValues()[3]
        );
        initQua.setXYZW(
                initQua_Array.getValues()[3], initQua_Array.getValues()[0], initQua_Array.getValues()[1], initQua_Array.getValues()[2]
        );
        MatrixF4x4 initQua_m4x4 = initQua.getMatrix4x4();
        initQua_m = new SimpleMatrix(new double[][]{
                {initQua_m4x4.getMatrix()[0], initQua_m4x4.getMatrix()[1], initQua_m4x4.getMatrix()[2]},
                {initQua_m4x4.getMatrix()[4], initQua_m4x4.getMatrix()[5], initQua_m4x4.getMatrix()[6]},
                {initQua_m4x4.getMatrix()[8], initQua_m4x4.getMatrix()[9], initQua_m4x4.getMatrix()[10]}
        });
        Log.d("RESET", "REST");
    }

    private SensorCollect.sensordata centerlizeTouch(SensorCollect.sensordata sensordata) {
        //convert view coordinate to screen
        sensordata.setData(new float[]{
                sensordata.getData()[0] - 720,
                sensordata.getData()[1] - 1280
        });

        return sensordata;
    }

    private ArrayList<SensorCollect.sensordata> viewlizeTouch(ArrayList<SensorCollect.sensordata> sensordatas) {
        for (int i = 0; i < sensordatas.size(); i++) {
            //convert view coordinate to screen
            sensordatas.get(i).setData(new float[]{
                    sensordatas.get(i).getData()[0] + 720,
                    sensordatas.get(i).getData()[1] + 1280
            });
        }
        return sensordatas;
    }


    private SensorCollect.sensordata bundle2sensordata(Bundle bundle) {
        SensorCollect.sensordata returesensordata = new SensorCollect.sensordata();
        for (String keys : bundle.keySet()) {
            if (keys == "Time") {
                returesensordata.setTime(bundle.getLong(keys));
            } else {
                returesensordata.setData(bundle.getFloatArray(keys));
            }
        }
        return returesensordata;
    }

    private SensorCollect.sensordata getlatestdelta(ArrayList<SensorCollect.sensordata> strokebuffer) {
        SensorCollect.sensordata msensordata = new SensorCollect.sensordata();
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
    private SensorCollect.sensordata multiply_multiplier_caused_by_rotation(float[] prevStroke, SensorCollect.sensordata acce) {
        SensorCollect.sensordata toreturn = new SensorCollect.sensordata(acce);

        //returnvec = [accevector] + [Rotationvector][touch relative to center] -  [touch relative to center]
        float[] accevector = acce.getData();
        float[] touch_relative_to_center = new float[]{prevStroke[0] - 691, prevStroke[1] - 905};
        //[Rotationvector][touch relative to center]
        SimpleMatrix Rot_mult_rev = new SimpleMatrix(currRot).mult(new SimpleMatrix(new double[][]{{touch_relative_to_center[0]}, {touch_relative_to_center[1]}, {0}}));
        //Rot_mult_rev = new SimpleMatrix(new double[][]{{0.866,-0.5,0},{0.5,0.866,0},{0,0,1}}).mult(new SimpleMatrix(new double[][]{{touch_relative_to_center[0]},{touch_relative_to_center[1]},{0}}));
        //Rot_mult_rev = new SimpleMatrix(new double[][]{{1d,0d,0d},{0d,1d,0d},{0d,0d,1d}}).mult(new SimpleMatrix(new double[][]{{touch_relative_to_center[0]},{touch_relative_to_center[1]},{0}}));
        toreturn.setData(new float[]{
                (float) (accevector[0] + ((float) Rot_mult_rev.get(0, 0) - touch_relative_to_center[0]) * 0.0000001),
                (float) (accevector[1] + ((float) Rot_mult_rev.get(1, 0) - touch_relative_to_center[1]) * 0.0000001),
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

