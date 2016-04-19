package com.project.nicki.displaystabilizer.stabilization;

import android.util.Log;

import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect.sensordata;
import com.project.nicki.displaystabilizer.dataprocessor.motion_Inertial;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;
import com.project.nicki.displaystabilizer.dataprocessor.utils.MatMultiply;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat.Matrix3D;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Vect2Mat.Vector3D;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.dataprocessor.motion_Inertial.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * Created by nickisverygood on 3/6/2016.
 */
public class stabilize_v3 {


    public static class stabilize {
        public static stabilizeSession mstabilizeSession;


        //create a stabilizeSession
        public static class stabilizeSession {
            float[] initLocation = init.initSensorCollection.initLocation;
            float[] initOrientation = init.initSensorCollection.initOrientation;
            //Touch
            public List<sensordata> OfflineTouchList = new ArrayList<>();
            public List<sensordata> OnlineTouchList = new ArrayList<>();
            //tmps
            List<Point> tmpPoint = new ArrayList<>();

            public stabilizeSession() {
                init.initSensorCollection = new SensorCollect();
                tmpPoint = new ArrayList<>();
            }

            public void setTouchList(sensordata mTouchsensordata) {
                OfflineTouchList.add(mTouchsensordata);

                if (OnlineTouchList.size() > 0) {
                    OnlineTouchList = new ArrayList<>();
                    OnlineTouchList.add(mTouchsensordata);

                } else {
                    OnlineTouchList.add(mTouchsensordata);
                }
                getStabilized("Online");
            }

            public List<sensordata> getOnlineLocationList() {
                return init.initSensorCollection.getInertialLocationList_online();
            }

            public List<sensordata> getOfflineLocationList() {
                Log.d("getOfflineLocationList", "HELLO");
                return init.initSensorCollection.getInertialLocationList_offline();
            }

            public List<sensordata> getOfflinOrientationList() {
                return init.initSensorCollection.getInertialOrientationList_offline();
            }
        }

        public static void createSession() {
            mstabilizeSession = new stabilizeSession();
        }

        public static List<Point> getStabilized(String Mode) {
            Log.d("getStabilized","init");
            if (mstabilizeSession == null) {
                mstabilizeSession = new stabilizeSession();
            }

            switch (Mode) {
                case "Online":
                    if (mstabilizeSession.OnlineTouchList.size() > 0 && mstabilizeSession.getOnlineLocationList().size() > 10) {
                        return OnlineStabilizationResults(mstabilizeSession.tmpPoint);
                    }
                case "Offline":
                    Log.d("getStabilized", "bbbbb");
                    if (mstabilizeSession.OfflineTouchList.size() > 0 && mstabilizeSession.getOfflinOrientationList().size() > 10) {
                        Log.d("getStabilized", "tttt");
                        return OfflineStabilizationResults();
                    }
                case "TEST":
                    List<Point> t = new ArrayList<>();
                    for (sensordata imTouch : mstabilizeSession.OnlineTouchList) {
                        Point mPoint = new Point();
                        mPoint.setX(imTouch.getData()[0] + 100);
                        mPoint.setY(imTouch.getData()[1] + 100);
                        t.add(mPoint);
                    }
                    return t;
                default:
                    return null;
            }


        }


        private static List<Point> OfflineStabilizationResults() {
            Log.d("OfflineStabilization","hello");
            List<sensordata> mLocationList = mstabilizeSession.getOfflineLocationList();
            List<sensordata> mInertialOrientationList = mstabilizeSession.getOfflinOrientationList();
            List<sensordata> mTouchList = mstabilizeSession.OfflineTouchList;
            return stabilizedPoint(mLocationList, mInertialOrientationList, mTouchList);
        }


        private static List<Point> OnlineStabilizationResults(List<Point> tmpPoint) {
            List<sensordata> mLocationList = mstabilizeSession.getOnlineLocationList();
            List<sensordata> mInertialOrientationList = mstabilizeSession.getOfflinOrientationList();
            List<sensordata> mTouchList = mstabilizeSession.OnlineTouchList;
            mstabilizeSession.tmpPoint.addAll(stabilizedPoint(mLocationList, mInertialOrientationList, mTouchList));
            new LogCSV("Online1","",new BigDecimal(mLocationList.get(mLocationList.size()-1).getTime()).toPlainString(),
                    mstabilizeSession.tmpPoint.get(mstabilizeSession.tmpPoint.size()-1).x,
                    mstabilizeSession.tmpPoint.get(mstabilizeSession.tmpPoint.size()-1).y);
            return mstabilizeSession.tmpPoint;
        }

        private static List<Point> stabilizedPoint(List<sensordata> mLocationList, List<sensordata> mInertialOrientationList, List<sensordata> mTouchList) {

            List<sensordata> sync_mLocationList = new ArrayList<>();
            List<sensordata> sync_InertialOrientationList = new ArrayList<>();
            List<Point> stabilizedPointList = new ArrayList<>();

            Log.d("debug1", String.valueOf(mLocationList.get(0).getTime()));
            for (int i = 0; i < mTouchList.size(); i++) {
                sync_mLocationList.add(new motion_Inertial().getbyTime(mTouchList.get(i).getTime(), mLocationList));
                sync_InertialOrientationList.add(new motion_Inertial().getbyTime(mTouchList.get(i).getTime(), mInertialOrientationList));
            }
            int num_mTouchList = mTouchList.size();
            while (sync_mLocationList.size() < num_mTouchList) {
                sync_mLocationList.add(sync_mLocationList.get(sync_mLocationList.size() - 1));
            }
            while (sync_InertialOrientationList.size() < num_mTouchList) {
                sync_InertialOrientationList.add(sync_InertialOrientationList.get(sync_InertialOrientationList.size() - 1));
            }
            while (sync_mLocationList.size() > num_mTouchList) {
                sync_mLocationList.remove(0);
            }
            while (sync_InertialOrientationList.size() > num_mTouchList) {
                sync_InertialOrientationList.remove(0);
            }
            for (int i = 0; i < mTouchList.size(); i++) {
                //construct 2d transfomation
                Vector3D mVec = new Vector3D();
                Matrix3D mMatrix = new Matrix3D();
                String Coefficient = "1";
                mVec.set(
                        multiply(20, double2String(-sync_mLocationList.get(i).getData()[0]), Coefficient).doubleValue(),
                        multiply(20, double2String(-sync_mLocationList.get(i).getData()[1]), Coefficient).doubleValue(),
                        multiply(20, double2String(-sync_mLocationList.get(i).getData()[2]), Coefficient).doubleValue());
                mMatrix.translate(mVec);
                mMatrix.rotateX(-sync_InertialOrientationList.get(i).getData()[0]);
                mMatrix.rotateY(-sync_InertialOrientationList.get(i).getData()[1]);
                mMatrix.rotateZ(-sync_InertialOrientationList.get(i).getData()[2]);
                double[][] _rotMatrixArray = new double[4][4];
                for (int k = 0; k < 4; k++) {
                    for (int j = 0; j < 4; j++) {
                        _rotMatrixArray[k][j] = (double) mMatrix.get(k).get(j);
                    }
                }
                float[][] _result = new motion_Inertial().toFloatArray(MatMultiply.multiplyByMatrix(_rotMatrixArray, new double[][]{
                        {(double) mTouchList.get(i).getData()[0]},
                        {(double) mTouchList.get(i).getData()[1]},
                        {(double) mTouchList.get(i).getData()[2]},
                        {1}
                }));
                Point addPoint = new Point();
                addPoint.setX(mTouchList.get(i).getData()[0] + 100);
                addPoint.setY(mTouchList.get(i).getData()[1] + 100);
                stabilizedPointList.add(addPoint);

            }
            return stabilizedPointList;
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

    public static BigDecimal multiply(int scale, String... inputs) {

        BigDecimal result = new BigDecimal(String.valueOf(1)).scaleByPowerOfTen(scale);
        for (String toMultiply : inputs) {
            result = result.multiply(new BigDecimal(new BigDecimal(toMultiply).toPlainString()));
        }
        return result;
    }

    public static String double2String(double d) {
        return new BigDecimal(d).toPlainString();
    }
}







