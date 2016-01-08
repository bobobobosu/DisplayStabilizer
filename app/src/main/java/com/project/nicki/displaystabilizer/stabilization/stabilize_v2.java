package com.project.nicki.displaystabilizer.stabilization;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by nickisverygood on 1/3/2016.
 */
public class stabilize_v2 implements Runnable {
    public static Handler getDraw;
    public static Handler getSensor;
    //draw to DemoDraw
    public static List<Point> toDraw = new ArrayList<Point>();
    public static BoundingBox bbox = new BoundingBox();
    //constants
    private static float cX = 3000;
    private static float cY = 3000;
    //mods
    public boolean CalibrationMode = true;
    FileWriter mFileWriter;
    //buffers
    ArrayList<sensordata> strokebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> strokedeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> accebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posbuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posdeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> stastrokebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> stastrokedeltabuffer = new ArrayList<sensordata>();
    //tmps
    long prevTime = 0;
    float[] prevStroke = null;
    boolean drawSTATUS = false;
    boolean prevdrawSTATUS = false;
    boolean init = false;
    sensordata tmpaccesensordata;
    private String csvName = "stabilize_v2.csv";
    private String TAG = "stabilize_v2";
    private Context mContext;

    public stabilize_v2(Context context) {
        mContext = context;
    }

    //setConstants
    public static void setcX(float cX) {
        stabilize_v2.cX = cX;
    }

    public static void setcY(float cY) {
        stabilize_v2.cY = cY;
    }

    @Override
    public void run() {
        Looper.prepare();
        getSensor = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle bundlegot = msg.getData();
                tmpaccesensordata = bundle2sensordata(bundlegot);
                Log.d(TAG, "get!!!!!");
                prevdrawSTATUS = drawSTATUS;
                drawSTATUS = DemoDraw.drawing < 2;

                //init
                if (prevdrawSTATUS == false && drawSTATUS == true || init == false) {
                    if (CalibrationMode == true && strokebuffer.size() > 1 && posbuffer.size() > 1) {
                        //cX = Math.abs(getSumArray(strokedeltabuffer).get(strokedeltabuffer.size()-1 +1).getData()[0])/Math.abs(getSumArray(posdeltabuffer).get(posdeltabuffer.size() - 1).getData()[0]);
                        //cY = Math.abs(getSumArray(strokedeltabuffer).get(strokedeltabuffer.size()-1 +1).getData()[1])/Math.abs(getSumArray(posdeltabuffer).get(posdeltabuffer.size()-1).getData()[1]);
                        cX = -Math.abs(strokebuffer.get(strokebuffer.size() - 1).getData()[0] - strokebuffer.get(0).getData()[0]) / Math.abs(posbuffer.get(posbuffer.size() - 1).getData()[0] - posbuffer.get(0).getData()[0]);
                        cY = -Math.abs(strokebuffer.get(strokebuffer.size() - 1).getData()[1] - strokebuffer.get(0).getData()[1]) / Math.abs(posbuffer.get(posbuffer.size() - 1).getData()[1] - posbuffer.get(0).getData()[1]);
                        CalibrationMode = false;
                    }
                    strokebuffer = new ArrayList<sensordata>();
                    strokedeltabuffer = new ArrayList<sensordata>();
                    posbuffer = new ArrayList<sensordata>();
                    posdeltabuffer = new ArrayList<sensordata>();
                    stastrokebuffer = new ArrayList<sensordata>();
                    stastrokedeltabuffer = new ArrayList<sensordata>();
                    prevTime = 0;
                    prevStroke = null;
                    init = true;
                    toDraw = new ArrayList<Point>();
                    tmpaccesensordata = null;
                }
            }
        };
        getDraw = new Handler() {
            //noshake
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle bundlegot = msg.getData();
                prevdrawSTATUS = drawSTATUS;
                drawSTATUS = DemoDraw.drawing < 2;

                //init
                if (prevdrawSTATUS == false && drawSTATUS == true || init == false) {
                    //refresh view
                    Message msg3 = new Message();
                    msg3.what = 1;
                    DemoDraw.mhandler.sendMessage(msg3);
                    if (CalibrationMode == true && strokebuffer.size() > 1 && posbuffer.size() > 1) {
                        //cX = Math.abs(getSumArray(strokedeltabuffer).get(strokedeltabuffer.size()-1 +1).getData()[0])/Math.abs(getSumArray(posdeltabuffer).get(posdeltabuffer.size() - 1).getData()[0]);
                        //cY = Math.abs(getSumArray(strokedeltabuffer).get(strokedeltabuffer.size()-1 +1).getData()[1])/Math.abs(getSumArray(posdeltabuffer).get(posdeltabuffer.size()-1).getData()[1]);
                        cX = -(Math.abs(strokebuffer.get(strokebuffer.size() - 1).getData()[0] - strokebuffer.get(0).getData()[0])) / (Math.abs(posbuffer.get(posbuffer.size() - 1).getData()[0] - posbuffer.get(0).getData()[0]));
                        cY = -(Math.abs(strokebuffer.get(strokebuffer.size() - 1).getData()[1] - strokebuffer.get(0).getData()[1])) / (Math.abs(posbuffer.get(posbuffer.size() - 1).getData()[1] - posbuffer.get(0).getData()[1]));
                        CalibrationMode = false;
                        Log.d(TAG, "multiplier: " + cX + " " + cY);
                    }
                    strokebuffer = new ArrayList<sensordata>();
                    strokedeltabuffer = new ArrayList<sensordata>();
                    posbuffer = new ArrayList<sensordata>();
                    posdeltabuffer = new ArrayList<sensordata>();
                    stastrokebuffer = new ArrayList<sensordata>();
                    stastrokedeltabuffer = new ArrayList<sensordata>();
                    prevTime = 0;
                    prevStroke = null;
                    init = true;
                    toDraw = new ArrayList<Point>();
                    tmpaccesensordata = null;

                }


                //buffer Pos
                if (tmpaccesensordata != null) {
                    posbuffer.add(tmpaccesensordata);
                    tmpaccesensordata = null;
                }
                if (posbuffer.size() > 1) {
                    posdeltabuffer.add(getlatestdelta(posbuffer));
                    if (posdeltabuffer.size() > 1) {
                        if (posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0] * posdeltabuffer.get(posdeltabuffer.size() - 2).getData()[0] < 0) {
                            Log.d(TAG, "reversed: " + posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0]);
                        }
                    }
                }
                //buffer Draw
                strokebuffer.add(bundle2sensordata(bundlegot));
                if (strokebuffer.size() > 1) {
                    strokedeltabuffer.add(getlatestdelta(strokebuffer));
                }

                //get stabilized result
                if (strokedeltabuffer.size() > 0 && posdeltabuffer.size() > 0 && posbuffer.size() > 0) {
                    sensordata stastrokedelta = new sensordata();
                    stastrokedelta.setTime(strokedeltabuffer.get(strokedeltabuffer.size() - 1).getTime());
                    stastrokedelta.setData(new float[]{
                            strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[0] - posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0] * cX,
                            strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[1] - posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[1] * cY});
                    stastrokedeltabuffer.add(stastrokedelta);
                    if (prevStroke != null) {
                        //sumof stastrokedeltabuffer
                        prevStroke[0] += stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[0];
                        prevStroke[1] += stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[1];
                        stastrokebuffer.add(new sensordata(stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getTime(), prevStroke));
                        //cal how far is fram prevStroke to finger(now)
                        float tofinger[] = new float[]{strokebuffer.get(strokebuffer.size() - 1).getData()[0] - prevStroke[0],
                                strokebuffer.get(strokebuffer.size() - 1).getData()[1] - prevStroke[1]};
                        toDraw = new ArrayList<Point>();
                        for (sensordata msensordata : stastrokebuffer) {
                            Point todrawPoint = new Point();
                            todrawPoint.x = msensordata.getData()[0] + tofinger[0];
                            todrawPoint.y = msensordata.getData()[1] + tofinger[1];
                            toDraw.add(todrawPoint);
                        }
                        bbox.set(toDraw);
                    } else {
                        prevStroke = new float[]{
                                strokebuffer.get(0).getData()[0],
                                strokebuffer.get(0).getData()[1]};
                        prevStroke = new float[]{0, 0};
                    }
                    Log.d(TAG, "drawpos: " + prevStroke[0] + " " + prevStroke[1]);
                }
                //refresh view
                Message msg3 = new Message();
                msg3.what = 1;
                DemoDraw.mhandler.sendMessage(msg3);
                if (
                        tmpaccesensordata != null &&
                                posbuffer.size() > 0 &&
                                posdeltabuffer.size() > 0 &&
                                strokebuffer.size() > 0 &&
                                strokedeltabuffer.size() > 0 &&
                                stastrokebuffer.size() > 0 &&
                                stastrokedeltabuffer.size() > 0) {
                    LogCSV(
                            String.valueOf(tmpaccesensordata.getTime()),
                            String.valueOf(posdeltabuffer.get(posdeltabuffer.size() - 1).getTime()),
                            String.valueOf(strokebuffer.get(strokebuffer.size() - 1).getTime()),
                            String.valueOf(strokedeltabuffer.get(strokedeltabuffer.size() - 1).getTime()),
                            String.valueOf(stastrokebuffer.get(stastrokebuffer.size() - 1).getTime()),
                            String.valueOf(strokedeltabuffer.get(strokedeltabuffer.size() - 1).getTime()));
                }

            }


        };
        Looper.loop();
    }

    private float vectlen(float[] data) {
        float returnLength = 0;
        double tmp_power_sum = 0;
        for (int i = 0; i < data.length; i++) {
            tmp_power_sum += Math.pow((double) data[i], 2);
        }
        return (float) Math.pow(tmp_power_sum, 0.5);
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

    private ArrayList<sensordata> getSumArray(ArrayList<sensordata> sensordatas) {
        ArrayList<sensordata> returnArray = new ArrayList<sensordata>();
        float[] tmp_data_sum = new float[sensordatas.get(0).getData().length];
        returnArray.add(new sensordata(sensordatas.get(0).getTime(), new float[sensordatas.get(0).getData().length]));
        for (int i = 0; i < sensordatas.size() - 1 + 1; i++) {
            for (int j = 0; j < tmp_data_sum.length; j++) {
                tmp_data_sum[j] += sensordatas.get(i).getData()[j];
            }
            returnArray.add(new sensordata(sensordatas.get(i).getTime(), tmp_data_sum));
        }
        return returnArray;
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

    public void LogCSV(String a, String b, String c, String d, String g, String h) {
        //init CSV logging
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = csvName;
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer = null;
        // File exist
        if (f.exists() && !f.isDirectory()) {
            try {
                mFileWriter = new FileWriter(filePath, true);
            } catch (IOException e) {
                //e.printStackTrace();
            }
            writer = new CSVWriter(mFileWriter);
        } else {
            try {
                writer = new CSVWriter(new FileWriter(filePath));
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }

        try {
            String line = String.format("%s,%s,%s,%s,%s,%s\n", a, b, c, d, g, h);
            mFileWriter.write(line);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        try {
            writer.close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    //getsetBoundingBox
    public static class BoundingBox {
        public float Xmin = 0;
        public float Xmax = 0;
        public float Ymin = 0;
        public float Ymax = 0;

        public void set(List<Point> pointArrayList) {
            if (pointArrayList.size() > 0) {
                Xmin = pointArrayList.get(0).x;
                Xmax = pointArrayList.get(0).x;
                Ymin = pointArrayList.get(0).y;
                Ymax = pointArrayList.get(0).y;
                for (Point p : pointArrayList) {
                    if (p.x < Xmin) {
                        Xmin = p.x;
                    }
                    if (p.x > Xmax) {
                        Xmax = p.x;
                    }
                    if (p.y < Ymin) {
                        Ymin = p.y;
                    }
                    if (p.y > Ymax) {
                        Ymax = p.y;
                    }
                }
                Log.d("bbox: ", Xmin + " " + Xmax + " " + Ymin + " " + Ymax);
            }
        }

        public float getXmin() {
            return Xmin;
        }

        public float getXmax() {
            return Xmax;
        }

        public float getYmin() {
            return Ymin;
        }

        public float getYmax() {
            return Ymax;
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

    public class sensordata {
        private long Time;
        private float[] Data = new float[3];

        public sensordata() {
            this(0, new float[]{0, 0, 0});
        }

        public sensordata(long time, float[] data) {
            this.Time = time;
            for (int i = 0; i < data.length; i++) {
                this.Data[i] = data[i];
            }
        }

        public void setsensordata(long time, float[] data) {
            this.Time = time;
            for (int i = 0; i < data.length; i++) {
                this.Data[i] = data[i];
            }
        }

        public long getTime() {
            return Time;
        }

        public void setTime(long time) {
            this.Time = time;
        }

        public float[] getData() {
            return Data;
        }

        public void setData(float[] data) {
            for (int i = 0; i < data.length; i++) {
                this.Data[i] = data[i];
            }
        }
    }


}
