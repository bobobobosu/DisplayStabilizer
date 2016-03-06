package com.project.nicki.displaystabilizer.stabilization;
import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import  com.project.nicki.displaystabilizer.dataprocessor.SensorCollect.sensordata;
import com.project.nicki.displaystabilizer.dataprocessor.motion_Inertial;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.dataprocessor.utils.MatMultiply;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat.Matrix3D;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat.Vector3D;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.dataprocessor.motion_Inertial.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by nickisverygood on 3/6/2016.
 */
public class stabilize_v3 {


    public static class stabilize {
        public static stabilizeSession mstabilizeSession;


        //create a stabilizeSession
        public static class stabilizeSession{
            float[] initLocation = init.initSensorCollection.initLocation;
            float[] initOrientation = init.initSensorCollection.initOrientation;
            //Touch
            public List<sensordata> OfflineTouchList = new ArrayList<>();
            public List<sensordata> OnlineTouchList = new ArrayList<>();
            //tmps
            List<Point> tmpPoint = new ArrayList<>();

            public stabilizeSession(){
                init.initSensorCollection = new SensorCollect();
                tmpPoint = new ArrayList<>();
            }
            public void setTouchList(sensordata mTouchsensordata){
                OfflineTouchList.add(mTouchsensordata);
                if(OnlineTouchList.size()>2){
                    OnlineTouchList.remove(0);
                }
                OnlineTouchList.add(mTouchsensordata);
            }
            public List<sensordata> getOnlineLocationList(){
                return init.initSensorCollection.getInertialLocationList_online();
            }
            public List<sensordata> getOfflineLocationList(){
                return init.initSensorCollection.getInertialLocationList_offline();
            }
            public List<sensordata> getOfflinOrientationList(){
                return init.initSensorCollection.getInertialOrientationList_offline();
            }
        }

        public static void createSession(){
            mstabilizeSession =new stabilizeSession();
        }
        public static List<Point> getStabilized(String Mode){
            if (mstabilizeSession == null){
                mstabilizeSession = new stabilizeSession();
            }
            if(mstabilizeSession.OnlineTouchList.size()>0 && mstabilizeSession.getOnlineLocationList().size()>10 && mstabilizeSession.getOfflinOrientationList().size()>10){
                switch (Mode){
                    case "Online":
                        return OnlineStabilizationResults(mstabilizeSession.tmpPoint);
                    case "Offline":
                        return OfflineStabilizationResults();
                    case "TEST":
                        List<Point> t = new ArrayList<>();
                        for (sensordata imTouch : mstabilizeSession.OnlineTouchList){
                            Point mPoint = new Point();
                            mPoint.setX(imTouch.getData()[0]+100);
                            mPoint.setY(imTouch.getData()[1]+100);
                            t.add(mPoint);
                        }
                        return t;
                    default:
                        return null;
                }
            }
            List<Point> none = new ArrayList<>();
            return none;
        }


        private static List<Point> OfflineStabilizationResults() {
            List<sensordata> mLocationList = mstabilizeSession.getOfflineLocationList();
            List<sensordata> mInertialOrientationList = mstabilizeSession.getOfflinOrientationList();
            List<sensordata> mTouchList = mstabilizeSession.OfflineTouchList;
            return stabilizedPoint(mLocationList,mInertialOrientationList,mTouchList);
        }


        private static List<Point> OnlineStabilizationResults(List<Point> tmpPoint) {
            List<sensordata> mLocationList = mstabilizeSession.getOnlineLocationList();
            List<sensordata> mInertialOrientationList = mstabilizeSession.getOfflinOrientationList();
            List<sensordata> mTouchList = mstabilizeSession.OnlineTouchList;
            mstabilizeSession.tmpPoint.addAll(stabilizedPoint(mLocationList,mInertialOrientationList,mTouchList));
            return mstabilizeSession.tmpPoint;
        }

        private static List<Point> stabilizedPoint(List<sensordata> mLocationList, List<sensordata> mInertialOrientationList, List<sensordata> mTouchList) {
            List<sensordata> sync_mLocationList = new ArrayList<>();
            List<sensordata> sync_InertialOrientationList = new ArrayList<>();
            List<Point> stabilizedPoinyList = new ArrayList<>();
            for (sensordata imTouchList:mTouchList){
                sync_mLocationList.add( new motion_Inertial().getElementByTime_interpolate(imTouchList.getTime(),mLocationList));
                sync_InertialOrientationList.add( new motion_Inertial().getElementByTime_interpolate(imTouchList.getTime(),mInertialOrientationList));
            }
            for(int i=0;i<mTouchList.size();i++){
                //construct 2d transfomation
                Vector3D mTrans = new Vector3D();
                Matrix3D mMatrix = new Matrix3D();

                float Coefficient = 1000000000;
                mTrans.set(
                        (double)(-sync_mLocationList.get(i).getData()[0])*Coefficient,
                        (double)(-sync_mLocationList.get(i).getData()[1])*Coefficient,
                        (double)(-sync_mLocationList.get(i).getData()[2])*Coefficient);
                mMatrix.translate(mTrans);
                mMatrix.rotateX(-sync_InertialOrientationList.get(i).getData()[0]);
                mMatrix.rotateY(-sync_InertialOrientationList.get(i).getData()[1]);
                mMatrix.rotateZ(-sync_InertialOrientationList.get(i).getData()[2]);
                double[][] rotMatrixArray = new double[4][4];
                for (int k = 0; k < 4; k++) {
                    for (int j = 0; j < 4; j++) {
                        rotMatrixArray[k][j] = (double) mMatrix.get(k).get(j);
                    }
                }
                float[][] result = new motion_Inertial().toFloatArray(MatMultiply.multiplyByMatrix(rotMatrixArray, new double[][]{
                        {(double) mTouchList.get(i).getData()[0]},
                        {(double) mTouchList.get(i).getData()[0]},
                        {(double) mTouchList.get(i).getData()[0]},
                        {1}
                }));
                Point addPoint =new Point();
                addPoint.setX(result[0][0]);
                addPoint.setY(result[1][0]);
                stabilizedPoinyList.add(addPoint);
                LogCSV.LogCSV("debug54",
                        mLocationList.get(i).getData()[0],
                        mLocationList.get(i).getData()[1],
                        mLocationList.get(i).getData()[2],
                        mInertialOrientationList.get(i).getData()[0],
                        mInertialOrientationList.get(i).getData()[1],
                        mInertialOrientationList.get(i).getData()[2],
                        mTouchList.get(i).getData()[0],
                        mTouchList.get(i).getData()[1]);
            }
            return stabilizedPoinyList;
        }
    }

    public static class Point implements Serializable {
        public float x;
        public float y;
        public float dx;
        public float dy;

        public void setX(float x) {
            this.x = x;
        }

        public void setY(float y) {
            this.y = y;
        }
    }
}







