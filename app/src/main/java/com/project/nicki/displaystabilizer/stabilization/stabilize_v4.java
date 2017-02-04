package com.project.nicki.displaystabilizer.stabilization;

import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.dataprovider.representation.MatrixF4x4;
import com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion;
import com.project.nicki.displaystabilizer.dataprovider.representation.Vector3f;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;

/**
 * Created by nicki on 1/30/2017.
 */

public class stabilize_v4 {
    public stabilize_v3_1_func mstabilize_v3_func = new stabilize_v3_1_func();
    public void set_Sensor(Bundle bundlegot){
        mstabilize_v3_func.set_Sensor(bundlegot);
    }
    public ArrayList<SensorCollect.sensordata> gen_Draw(Bundle bundlegot) {
        return  mstabilize_v3_func.gen_Draw(bundlegot);
    }

    public float[] projectTouchVec(float[] prev_Touch_World, float[] curr_Touch_World, Quaternion curr_Orien, float near) {
        // Set the camera position (View matrix)
        //void setLookAtM (float[] rm,int rmOffset,float eyeX,float eyeY, float eyeZ,float centerX,float centerY,float centerZ,float upX,float upY,float upZ)
        float[] mViewMatrix = new float[16];
        double[] UpVector = getUpVectorfromQuaternionandGravity(curr_Orien);
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, (float) UpVector[0], (float) UpVector[1], (float) UpVector[2]);
        //Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        float[] perspectiveM = new float[16];

        float ratio = (float) 16 / 9;
        //Matrix.perspectiveM(perspectiveM, 0, 1.09956f,ratio, 0, 100);
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(perspectiveM, 0, -ratio, ratio, -1, 1, 3, 7);

        // Calculate the projection and view transformation
        float[] transformation = new float[16];
        Matrix.multiplyMM(transformation, 0, perspectiveM, 0, mViewMatrix, 0);
        SimpleMatrix transformation_M = new SimpleMatrix(new double[][]{
                {(double) transformation[0], (double) transformation[4], (double) transformation[8], (double) transformation[12]},
                {(double) transformation[1], (double) transformation[5], (double) transformation[9], (double) transformation[13]},
                {(double) transformation[2], (double) transformation[6], (double) transformation[10], (double) transformation[14]},
                {(double) transformation[3], (double) transformation[7], (double) transformation[11], (double) transformation[15]}
        });
        SimpleMatrix prev_Touch_World_M = new SimpleMatrix(new double[][]{
                {prev_Touch_World[0]},
                {prev_Touch_World[1]},
                {prev_Touch_World[2]},
                {1}
        });
        SimpleMatrix curr_Touch_World_M = new SimpleMatrix(new double[][]{
                {curr_Touch_World[0]},
                {curr_Touch_World[1]},
                {curr_Touch_World[2]},
                {1}
        });
        prev_Touch_World_M = transformation_M.mult(prev_Touch_World_M);
        curr_Touch_World_M = transformation_M.mult(curr_Touch_World_M);
        double[] car_prev_Touch_World_M = homogeneous2cartesian(new double[]{
                prev_Touch_World_M.get(0, 0), prev_Touch_World_M.get(1, 0), prev_Touch_World_M.get(2, 0), prev_Touch_World_M.get(3, 0)
        });
        double[] car_curr_Touch_World_M = homogeneous2cartesian(new double[]{
                curr_Touch_World_M.get(0, 0), curr_Touch_World_M.get(1, 0), curr_Touch_World_M.get(2, 0), curr_Touch_World_M.get(3, 0)
        });
        double[] projectedVec_out = new double[]{
                (car_curr_Touch_World_M[0] - car_prev_Touch_World_M[0]),
                (car_curr_Touch_World_M[1] - car_prev_Touch_World_M[1]),
                (car_curr_Touch_World_M[2] - car_prev_Touch_World_M[2]),
        };
        double[] projectedVec_inside = out2inside(projectedVec_out);
        Log.d("STA4", String.valueOf(car_curr_Touch_World_M[0] + " " + car_curr_Touch_World_M[1] + " " + car_curr_Touch_World_M[2]));
        //return new float[]{(float) projectedVec_inside[0], (float) projectedVec_inside[1], (float) projectedVec_inside[2]};
        return new float[]{
                (float) car_curr_Touch_World_M[0], (float) car_curr_Touch_World_M[1], (float) car_curr_Touch_World_M[2]
        };

    }

    public double[] getUpVectorfromQuaternionandGravity(Quaternion curr_Orien) {
        float[] rotarray = curr_Orien.getMatrix4x4().getMatrix();
        SimpleMatrix rot = new SimpleMatrix(new double[][]{
                {(double) rotarray[0], (double) rotarray[4], (double) rotarray[8], (double) rotarray[12]},
                {(double) rotarray[1], (double) rotarray[5], (double) rotarray[9], (double) rotarray[13]},
                {(double) rotarray[2], (double) rotarray[5], (double) rotarray[10], (double) rotarray[14]},
                {(double) rotarray[3], (double) rotarray[6], (double) rotarray[11], (double) rotarray[15]}
        });
        SimpleMatrix direction = new SimpleMatrix(new double[][]{{1}, {0}, {0},{1}});
        direction = rot.mult(direction);
        Vector3D curr_Orien_Vec = new Vector3D(direction.get(0,0)/direction.get(3,0), direction.get(1,0)/direction.get(3,0), direction.get(2,0)/direction.get(3,0));
        Vector3D gravity_Vec = new Vector3D(0, 0, -1);
        Vector3D UpVector_throughscreen = curr_Orien_Vec.crossProduct(gravity_Vec);
        UpVector_throughscreen = curr_Orien_Vec.crossProduct(UpVector_throughscreen);
        double[] UpVector_insidescreen_f = new double[]{
                (double) UpVector_throughscreen.getX(),
                (double) UpVector_throughscreen.getY(),
                (double) UpVector_throughscreen.getZ()
        };
        double[] UpVector_outscreen_f = inside2out(UpVector_insidescreen_f);
        return UpVector_outscreen_f;
    }

    public double[] inside2out(double[] vec) {
        return new double[]{
                vec[0],
                vec[2],
                -vec[1]
        };
    }

    public double[] out2inside(double[] vec) {
        return new double[]{
                vec[0],
                -vec[2],
                vec[1]
        };
    }

    public double[] homogeneous2cartesian(double[] homogeneous) {
        return new double[]{
                (homogeneous[0] / homogeneous[3]),
                (homogeneous[1] / homogeneous[3]),
                (homogeneous[2] / homogeneous[3]),
        };
    }

    public void computeAbsoluteTouchDisplacement(){

    }
}
