package com.project.nicki.displaystabilizer.stabilization;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.project.nicki.displaystabilizer.UI.DemoDrawUI;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.utils.LogCSV;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by nickisverygood on 1/3/2016.
 */
public class stabilize_v3_1 {
    public static Handler getDraw;
    public static Handler getSensor;
    //draw to DemoDraw
    public static List<stabilize_v3.Point> toDraw = new ArrayList<>();
    public static BoundingBox bbox = new BoundingBox();
    public static boolean posdrawing = false;
    public static boolean updatefakepos = false;
    public static int oneorminusone = 1;
    //constants
    private static float cX;
    private static float cY;
    //mods
    public boolean CalibrationMode = true;
    public ArrayList<sensordata> stroke2pos;
    public int fakeposposition;
    FileWriter mFileWriter;
    //buffers
    ArrayList<sensordata> strokebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> strokedeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posbuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> posdeltabuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> stastrokebuffer = new ArrayList<sensordata>();
    ArrayList<sensordata> stastrokedeltabuffer = new ArrayList<sensordata>();
    //tmps
    long prevTime = 0;
    float[] tmp_prevStroke = null;
    float[] prevStroke = null;
    boolean drawSTATUS = false;
    boolean prevdrawSTATUS = false;
    boolean init = false;
    sensordata tmpaccesensordata;
    sensordata tmporiensensordata;
    float[] Pos = null;
    int deltaingStatus = 0;
    sensordata tmp1accesensordata;
    //init specific
    float[] orieninit;
    private String csvName = "stabilize_v3_1.csv";
    private String TAG = "stabilize_v3_1";
    private Context mContext;


    public stabilize_v3_1(Context context) {
        mContext = context;
    }

    public stabilize_v3_1() {
    }

    String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    String fileName = csvName;
    String filePath = baseDir + File.separator + fileName;
    File f = new File(filePath);
    CSVWriter writer = null;

    public void set_Sensor(final Bundle bundlegot) {
        Pos = bundlegot.getFloatArray("Pos");

        tmpaccesensordata = new sensordata(bundlegot.getLong("Time"), bundlegot.getFloatArray("Pos"));
        tmporiensensordata = new sensordata(bundlegot.getLong("Time"), bundlegot.getFloatArray("Orien"));
        if (DemoDraw3.orienreset == false) {
            orieninit = tmporiensensordata.getData();
            DemoDraw3.orienreset = true;
        }

        prevdrawSTATUS = drawSTATUS;
        drawSTATUS = DemoDraw3.drawing < 2;

        if (deltaingStatus == 0) {
            tmp1accesensordata = tmpaccesensordata;
            tmpaccesensordata = new sensordata(tmp1accesensordata.getTime(), new float[]{0, 0});
            deltaingStatus = 1;
        } else if (deltaingStatus == 1) {
            tmpaccesensordata = new sensordata(tmp1accesensordata.getTime(), new float[]{
                    tmpaccesensordata.getData()[0] - tmp1accesensordata.getData()[1],
                    tmpaccesensordata.getData()[1] - tmp1accesensordata.getData()[1]});
        }
        //init
        if (prevdrawSTATUS == false && drawSTATUS == true || init == false) {
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
            tmp_prevStroke = null;
            init = true;
            tmpaccesensordata = null;
            deltaingStatus = 0;
        }

    }

    public void gen_Draw(final Bundle bundlegot) {
        if (DemoDraw.drawing == 0) {
            orieninit = tmporiensensordata.getData();
        }
        if (tmpaccesensordata == null || Pos == null) {
            return;
        }
        prevdrawSTATUS = drawSTATUS;
        drawSTATUS = DemoDraw3.drawing < 2;

        if (prevdrawSTATUS == false && drawSTATUS == true || init == false) {

            fakeposposition = 0;
            if (updatefakepos == true) {
                stroke2pos = strokedeltabuffer;
                Toast.makeText(mContext, "fakepos Updated " + stroke2pos.size(),
                        Toast.LENGTH_LONG).show();
                updatefakepos = false;
            }
            fakeposposition = 0;


            //refresh view
            Message msg3 = new Message();
            msg3.what = 1;
            DemoDraw3.mhandler.sendMessage(msg3);
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
            tmp_prevStroke = null;
            init = true;
            toDraw = new ArrayList<stabilize_v3.Point>();
            tmpaccesensordata = null;

        }


        //manual control
        cX = -10f;
        cY = 10f;


        //buffer Draw
        strokebuffer.add(bundle2sensordata(bundlegot));
        if (strokebuffer.size() > 1) {
            strokedeltabuffer.add(getlatestdelta(strokebuffer));
        }

        //exit if set to draw2pos
        Log.d(TAG, "fakepos: " + posdrawing);
        if (posdrawing == true && stroke2pos != null && fakeposposition < stroke2pos.size() - 1) {
            posdeltabuffer.add(new sensordata(stroke2pos.get(fakeposposition).getTime(), new float[]{
                    stroke2pos.get(fakeposposition).getData()[0] * (float) com.project.nicki.displaystabilizer.init.pix2m * 100 * oneorminusone,
                    stroke2pos.get(fakeposposition).getData()[1] * (float) com.project.nicki.displaystabilizer.init.pix2m * 100 * oneorminusone}));
            fakeposposition++;

        } else {
            //buffer Pos
            if (tmpaccesensordata != null) {
                posdeltabuffer.add(tmpaccesensordata);
                tmpaccesensordata = null;
            }
        }

        if (posdeltabuffer != null && stroke2pos != null) {
            if (posdeltabuffer.size() > 0 && stroke2pos.size() > 0) {
                Log.d(TAG, "testhowbig " + String.valueOf(posdeltabuffer.get(0).getData()[0] + " " + stroke2pos.get(0).getData()[0] * (float) com.project.nicki.displaystabilizer.init.pix2m));
            }
        }

        //get stabilized result
        if (strokedeltabuffer.size() > 0 && posdeltabuffer.size() > 0) {
            sensordata stastrokedelta = new sensordata();
            stastrokedelta.setTime(strokedeltabuffer.get(strokedeltabuffer.size() - 1).getTime());
            stastrokedelta.setData(new float[]{
                    strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[0] - posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[0] * cX / (float) com.project.nicki.displaystabilizer.init.pix2m,
                    strokedeltabuffer.get(strokedeltabuffer.size() - 1).getData()[1] - posdeltabuffer.get(posdeltabuffer.size() - 1).getData()[1] * cY / (float) com.project.nicki.displaystabilizer.init.pix2m});
            stastrokedeltabuffer.add(stastrokedelta);

            if (prevStroke != null) {
                prevStroke[0] += stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[0];
                prevStroke[1] += stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getData()[1];
                stastrokebuffer.add(new sensordata(stastrokedeltabuffer.get(stastrokedeltabuffer.size() - 1).getTime(), prevStroke));
                tmp_prevStroke[0] = strokebuffer.get(strokebuffer.size() - 1).getData()[0];
                tmp_prevStroke[1] = strokebuffer.get(strokebuffer.size() - 1).getData()[1];
                float tofinger[] = new float[]{strokebuffer.get(strokebuffer.size() - 1).getData()[0] - prevStroke[0],
                        strokebuffer.get(strokebuffer.size() - 1).getData()[1] - prevStroke[1]};
                ArrayList<sensordata> r_stastrokebuffer = new ArrayList<>();
                for (int i = 0; i < stastrokebuffer.size(); i++) {
                    sensordata tmp = new sensordata(strokebuffer.get(i));
                    tmp.setData(new float[]{
                            stastrokebuffer.get(i).getData()[0] + tofinger[0],
                            stastrokebuffer.get(i).getData()[1] + tofinger[1]
                    });
                    r_stastrokebuffer.add(tmp);
                }

                rotateBuffer mrotateBuffer = new rotateBuffer();
                mrotateBuffer.rotateBuffer(r_stastrokebuffer, new float[]{
                        0, 0, 0
                });


                toDraw = new ArrayList<>();
                for (sensordata msensordata : mrotateBuffer.getRotated()) {
                    stabilize_v3.Point todrawPoint = new stabilize_v3.Point();
                    todrawPoint.x = msensordata.getData()[0];
                    todrawPoint.y = msensordata.getData()[1];
                    Log.e("TESTING", String.valueOf(todrawPoint.x));
                    toDraw.add(todrawPoint);
                }

                bbox.set(toDraw);
            } else {
                tmp_prevStroke = new float[]{0, 0};
                prevStroke = new float[]{
                        strokebuffer.get(0).getData()[0],
                        strokebuffer.get(0).getData()[1]};
                prevStroke = new float[]{0, 0};
            }
            Log.d(TAG, "drawpos: " + prevStroke[0] + " " + prevStroke[1]);
        }
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

    public void LogCSV(String Name, String a, String b, String c, String d, String g, String h) {
        //init CSV logging
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        //String fileName = csvName;
        String fileName = Name + ".csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer = null;
        // File exist
        if (f.exists() && !f.isDirectory()) {
            try {
                mFileWriter = new FileWriter(filePath, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer = new CSVWriter(mFileWriter);
        } else {
            try {
                writer = new CSVWriter(new FileWriter(filePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            String line = String.format("%s,%s,%s,%s,%s,%s\n", a, b, c, d, g, h);
            mFileWriter.write(line);
        } catch (Exception ex) {
        }
        /*
        catch (IOException e) {
            e.printStackTrace();
        }
        */

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //getsetBoundingBox
    public static class BoundingBox {
        public float Xmin = 0;
        public float Xmax = 0;
        public float Ymin = 0;
        public float Ymax = 0;

        public void set(List<stabilize_v3.Point> pointArrayList) {
            if (pointArrayList.size() > 0) {
                Xmin = pointArrayList.get(0).x;
                Xmax = pointArrayList.get(0).x;
                Ymin = pointArrayList.get(0).y;
                Ymax = pointArrayList.get(0).y;
                for (stabilize_v3.Point p : pointArrayList) {
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

    public class rotateBuffer {
        ArrayList<sensordata> rotated = new ArrayList<>();

        public void rotateBuffer(ArrayList<sensordata> msensordatabuffer, float[] rotateangle_rad) {
            rotated = new ArrayList<>();
            for (int i = 0; i < msensordatabuffer.size(); i++) {
                Point tmpP = new Point(msensordatabuffer.get(i).getData()[0], msensordatabuffer.get(i).getData()[1]);
                Point tmpP_cen = new Point(msensordatabuffer.get(msensordatabuffer.size() - 1).getData()[0], msensordatabuffer.get(msensordatabuffer.size() - 1).getData()[1]);
                tmpP = rotate(tmpP, tmpP_cen, rotateangle_rad[2]);

                sensordata tmpsensordata = new sensordata(msensordatabuffer.get(i));
                tmpsensordata.setData(new float[]{
                        tmpP.x, tmpP.y
                });
                rotated.add(tmpsensordata);
            }
        }

        public ArrayList<sensordata> getRotated() {
            return rotated;
        }

        public Point rotate(Point point, Point ard, float angle_rad) {
            float x1 = point.x - ard.x;
            float y1 = point.y - ard.y;

            float x2 = (float) (x1 * Math.cos(angle_rad) - y1 * Math.sin(angle_rad));
            float y2 = (float) (x1 * Math.sin(angle_rad) + y1 * Math.cos(angle_rad));

            point.x = x2 + ard.x;
            point.y = y2 + ard.y;
            return point;
        }

        public class Point {
            float x;
            float y;

            public Point(float x, float y) {
                this.x = x;
                this.y = y;
            }
        }
    }

    public class sensordata {
        private long Time;
        private float[] Data = new float[3];

        public sensordata() {
            this(0, new float[]{0, 0, 0});
        }

        public sensordata(sensordata msensordata) {
            setData(msensordata.getData());
            setTime(msensordata.getTime());
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

    public class display {
        public void displaystatus1(final String s) {
            DemoDrawUI.runOnUI(new Runnable() {
                @Override
                public void run() {
                    try {
                        //DemoDrawUI.mlog_acce.setText(s);
                    } catch (Exception ex) {

                    }

                }
            });
        }

        public void displaystatus2(final String s) {
            DemoDrawUI.runOnUI(new Runnable() {
                @Override
                public void run() {
                    try {
                        DemoDrawUI.mlog_gyro.setText(s);
                    } catch (Exception ex) {

                    }

                }
            });
        }

        public void diaplayperformance(final String s) {
            DemoDrawUI.runOnUI(new Runnable() {
                @Override
                public void run() {
                    try {
                        DemoDrawUI.mperformance.setText(s);
                    } catch (Exception ex) {

                    }

                }
            });
        }
    }


}
