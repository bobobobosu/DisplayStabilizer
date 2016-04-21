package com.project.nicki.displaystabilizer.contentprovider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


import com.canvas.LipiTKJNIInterface;
import com.canvas.LipitkResult;
import com.canvas.Stroke;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3_1;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DemoDraw2 extends View {
    public static int StrokeResultCount=0;
    private static final String TAG = "DemoDraw";
    public static boolean resetted = true;
    public static boolean orienreset = false;
    public static int drawing = 3;
    public static Paint paint2 = new Paint();
    public static Path path2 = new Path();
    public static Path path3 = new Path();
    public static Rect rectangle;
    public static Handler mhandler;
    public static Paint paint = new Paint();
    public static Paint paint3 = new Paint();
    public static int rectX, rectY, sideLength;
    public List<stabilize_v3.Point> incremental = new ArrayList<>();
    public Path path = new Path();
    protected Context mContext;
    private LipiTKJNIInterface _lipitkInterface;
    private LipiTKJNIInterface _recognizer = null;
    public recognize_stroke mrecognize_stroke = new recognize_stroke();

    public DemoDraw2(Context context) {
        super(context);
        this.mContext = context.getApplicationContext();
    }

    public DemoDraw2(Context context, AttributeSet attrs) {

        super(context, attrs);

        //recognize
        Context contextlipi = getContext();
        File externalFileDir = contextlipi.getExternalFilesDir(null);
        String path = externalFileDir.getPath();
        Log.d("JNI", "Path: " + path);
        _lipitkInterface = new LipiTKJNIInterface(path, "SHAPEREC_ALPHANUM");
        _lipitkInterface.initialize();
        _recognizer = _lipitkInterface;


        paint.setAntiAlias(true);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);

        paint2.setAntiAlias(true);
        paint2.setStrokeWidth(5f);
        paint2.setColor(Color.BLACK);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeJoin(Paint.Join.ROUND);

        paint3.setAntiAlias(true);
        paint3.setStrokeWidth(5f);
        paint3.setColor(Color.RED);
        paint3.setStyle(Paint.Style.STROKE);
        paint3.setStrokeJoin(Paint.Join.ROUND);

        mhandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                invalidate();
            }
        };

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(drawing==1 || drawing==0){
           //drawCanvas(canvas, stabilize_v3.stabilize.mstabilizeSession.todraw);
        }
        //drawCanvas(canvas, init.initTouchCollection.sta_Online_todraw_stroke);
        canvas.drawPath(path, paint);
        canvas.drawPath(path2, paint2);
    }


    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final float eventX = event.getX();
        final float eventY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        stabilize_v3.stabilize.createSession();
                        stabilize_v3.stabilize.mstabilizeSession.setTouchList(new SensorCollect.sensordata(System.currentTimeMillis(), new float[]{eventX, eventY, 0}, SensorCollect.sensordata.TYPE.TOUCH));
                    }
                }).start();

                mrecognize_stroke.collect(event);
                //resetted = false;
                orienreset = false;
                //path.reset();
                //path2.reset();
                //path3.reset();
                drawing = 0;
                new passTouch(event);
                path.moveTo(eventX, eventY);
                return true;
            case MotionEvent.ACTION_MOVE:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        stabilize_v3.stabilize.mstabilizeSession.setTouchList(new SensorCollect.sensordata(System.currentTimeMillis(), new float[]{eventX, eventY, 0}, SensorCollect.sensordata.TYPE.TOUCH));
                    }
                }).start();
                mrecognize_stroke.collect(event);
                drawing = 1;
                new passTouch(event);
                path.lineTo(eventX, eventY);
                break;
            case MotionEvent.ACTION_UP:

                mrecognize_stroke.collect(event);
                new passTouch(event);
                drawing = 2;
                // nothing to do
                Thread r = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        stabilize_v3.stabilize.getStabilized("Offline");
                    }
                });
                r.start();


                break;
            default:
                drawing = 3;
                return false;
        }

        // Schedules a repaint.
        invalidate();
        return true;
    }


    //todraw
    private void drawCanvas(Canvas canvas, List<stabilize_v3.Point> pts) {
        if (pts.size() > 1) {
            path3 = new Path();
            final int SMOOTH_VAL = 6;
            for (int i = pts.size() - 2; i < pts.size(); i++) {
                if (i >= 0) {
                    stabilize_v3.Point point = pts.get(i);
                    if (i == 0) {
                        stabilize_v3.Point next = pts.get(i + 1);
                        point.dx = ((next.x - point.x) / SMOOTH_VAL);
                        point.dy = ((next.y - point.y) / SMOOTH_VAL);
                    } else if (i == pts.size() - 1) {
                        stabilize_v3.Point prev = pts.get(i - 1);
                        point.dx = ((point.x - prev.x) / SMOOTH_VAL);
                        point.dy = ((point.y - prev.y) / SMOOTH_VAL);
                    } else {
                        stabilize_v3.Point next = pts.get(i + 1);
                        stabilize_v3.Point prev = pts.get(i - 1);
                        point.dx = ((next.x - prev.x) / SMOOTH_VAL);
                        point.dy = ((next.y - prev.y) / SMOOTH_VAL);
                    }
                }
            }
            boolean first = true;
            for (int i = 0; i < pts.size(); i++) {
                stabilize_v3.Point point = pts.get(i);
                if (first) {
                    first = false;
                    path3.moveTo(point.x, point.y);
                } else {
                    stabilize_v3.Point prev = pts.get(i - 1);
                    path3.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx, point.y - point.dy, point.x, point.y);
                }
            }
            canvas.drawPath(path3, paint3);
        } else {
            if (pts.size() == 1) {
                stabilize_v3.Point point = pts.get(0);
                canvas.drawCircle(point.x, point.y, 2, paint3);
            }
        }
    }

    private void drawBitmap(Bitmap bmp, List<stabilize_v3.Point> pts) {
        Canvas c = new Canvas(bmp);
        drawCanvas(c, pts);
    }

    public static class passTouch {
        public passTouch(MotionEvent event) {
            Message msgDRAWING = new Message();
            msgDRAWING.what = 1;
            msgDRAWING.arg1 = 0;
            float[] dataDRAWING = new float[2];
            long currTimeDRAWING = System.currentTimeMillis();
            Bundle drawposBundleDRAWING = new Bundle();
            dataDRAWING[0] = event.getX();
            dataDRAWING[1] = event.getY();
            drawposBundleDRAWING.putFloatArray("Draw", dataDRAWING);
            drawposBundleDRAWING.putLong("Time", currTimeDRAWING);
            msgDRAWING.setData(drawposBundleDRAWING);
            if (dataDRAWING[0] != 0 && dataDRAWING[1] != 0) {
                stabilize_v3_1.getDraw.sendMessage(msgDRAWING);
            }
        }
    }

    public class recognize_stroke {
        private Stroke strokes = new Stroke();
        private String[] character;

        public void collect(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                PointF p = new PointF(event.getX(), event.getY());
                strokes.addPoint(p);
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                PointF p = new PointF(event.getX(), event.getY());
                strokes.addPoint(p);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {

                PointF p = new PointF(event.getX(), event.getY());
                strokes.addPoint(p);
                recognize(strokes);
                strokes = new Stroke();
            }
        }

        private String[] recognize(Stroke _strokes) {

            Stroke[] _recognitionStrokes = new Stroke[1];
            _recognitionStrokes[0] = _strokes;
            LipitkResult[] results = _recognizer.recognize(_recognitionStrokes);
            String configFileDirectory = _recognizer.getLipiDirectory() + "/projects/alphanumeric/config/";
            for (LipitkResult result : results) {
                Log.e("jni",_recognizer.getSymbolName(results[0].Id, configFileDirectory) + " ShapeAID = " + result.Id + " Confidence = " + result.Confidence);
            }

            StrokeResultCount = results.length;

            _recognitionStrokes = null;
            return character;
        }
    }
}