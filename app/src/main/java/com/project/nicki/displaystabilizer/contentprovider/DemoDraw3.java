package com.project.nicki.displaystabilizer.contentprovider;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DemoDraw3 extends View {
    //recognize
    private LipiTKJNIInterface _lipitkInterface;
    private static LipiTKJNIInterface _recognizer = null;
    public static int StrokeResultCount = 0;

    private class path_ctrl{
        List<Path> pathList = new ArrayList<>();
        public path_ctrl(){
            pathList.add(new Path());
        }
        public Path getNew(){
            pathList.add(new Path());
            return pathList.get(pathList.size()-1);
        }
        public Path getReplace(){
            try {
                pathList.remove(pathList.size()-1);
            }catch (Exception ex){

            }
            pathList.add(new Path());
            return pathList.get(pathList.size()-1);
        }
        public Path getCurr(){
            return pathList.get(pathList.size()-1);
        }
        public void drawAll(Canvas canvas,Paint paint){
            for(Path mpath:pathList){
                canvas.drawPath(mpath,paint);
            }
        }
    }
    public path_ctrl mpath_ctrl= new path_ctrl();
    public static List<List<stabilize_v3.Point>> pending_to_draw = new ArrayList<>();
    public static List<stabilize_v3.Point> pending_to_draw_direct = new ArrayList<>();


    private static final String TAG = "DemoDraw";
    public static boolean orienreset = false;
    public static int drawing = 3;
    public static Paint paint2 = new Paint();
    public static Path path2 = new Path();
    public static Path path3 = new Path();
    public static Rect rectangle;
    public static Handler clean_and_refresh;
    public static Handler refresh;
    public static Paint paint = new Paint();
    public static Paint paint3 = new Paint();
    public List<stabilize_v3.Point> incremental = new ArrayList<>();
    public Path path = new Path();
    protected Context mContext;



    public DemoDraw3(Context context) {
        super(context);
        this.mContext = context.getApplicationContext();
    }

    public DemoDraw3(Context context, AttributeSet attrs) {
        super(context, attrs);
        //recognize
        File externalFileDir = getContext().getExternalFilesDir(null);
        final String externalFileDirPath = externalFileDir.getPath();
        Log.d("JNI", "Path: " + externalFileDirPath);
        _lipitkInterface = new LipiTKJNIInterface(externalFileDirPath, "SHAPEREC_ALPHANUM");
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

        refresh = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                invalidate();
            }
        };
        clean_and_refresh = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.e("LOG","bibibib");
                path = new Path();
                path2 = new Path();
                path3 = new Path();
                invalidate();
            }
        };

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(drawing==1 || drawing==0){
            //drawCanvas(canvas, stabilize_v3.stabilize.mstabilizeSession.todraw);
    }

        try {
            drawCanvas(canvas,path3,pending_to_draw_direct);
            draw_ListofPaths(canvas,paint3,pending_to_draw);

        }catch (Exception ex){
            //Log.e("onDraw",String.valueOf(ex));
        }

        mpath_ctrl.drawAll(canvas,paint3);
        canvas.drawPath(path, paint);
        canvas.drawPath(path2, paint2);
    }

    public void draw_ListofPaths(Canvas canvas,Paint paint,List<List<stabilize_v3.Point>> pending_to_draw){
        path3= new Path();
        for(List<stabilize_v3.Point> impending_to_draw:pending_to_draw){
            try {
                Log.e("TESTING",String.valueOf("THE SIZE: "+impending_to_draw.size()));
            }catch (Exception ex){

            }

            drawCanvas(canvas, path3,impending_to_draw);
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final float eventX = event.getX();
        final float eventY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
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
                mpath_ctrl.getReplace();
                drawing = 1;
                new passTouch(event);
                path.lineTo(eventX, eventY);
                break;
            case MotionEvent.ACTION_UP:
                mpath_ctrl.getNew();
                new passTouch(event);
                drawing = 2;
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
    private void drawCanvas(Canvas canvas,Path mpath, List<stabilize_v3.Point> pts) {
        if (pts.size() > 1) {
            final int SMOOTH_VAL = 6;
            for (int i = pts.size() - 2; i < pts.size(); i++) {
                Log.e("draw",String.valueOf(pts.get(i).x));
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
                    mpath.moveTo(point.x, point.y);
                } else {
                    stabilize_v3.Point prev = pts.get(i - 1);
                    mpath.cubicTo(prev.x + prev.dx, prev.y + prev.dy, point.x - point.dx, point.y - point.dy, point.x, point.y);
                }
            }
            canvas.drawPath(mpath, paint3);
        } else {
            if (pts.size() == 1) {
                stabilize_v3.Point point = pts.get(0);
                canvas.drawCircle(point.x, point.y, 2, paint3);
            }
        }
    }

    //pass touch
    public class passTouch {
        public passTouch(final MotionEvent event) {
            init.initTouchCollection.set_Touch(event);
        }
    }


    public static recognized_data recognize_stroke(List<List<SensorCollect.sensordata>> lists) {
        if (lists.size() > 0) {
            String[] character = new String[0];

            List<Stroke> _strokes = new ArrayList<>();

            for (List<SensorCollect.sensordata> stroke : lists) {
                Stroke mstroke = new Stroke();
                for (SensorCollect.sensordata point : stroke) {
                    mstroke.addPoint(new PointF(point.getData()[0], point.getData()[1]));
                }
                _strokes.add(mstroke);
            }

            _strokes = merge_strokes(_strokes);

            Stroke[] _recognitionStrokes = new Stroke[_strokes.size()];
            for (int s = 0; s < _strokes.size(); s++)
                _recognitionStrokes[s] = _strokes.get(s);

            _recognitionStrokes = new Stroke[1];
            _recognitionStrokes[0] = _strokes.get(0);

            LipitkResult[] results = _recognizer.recognize(_recognitionStrokes);
            String configFileDirectory = _recognizer.getLipiDirectory() + "/projects/alphanumeric/config/";
            character = new String[results.length];
            for (int i = 0; i < character.length; i++) {
                character[i] = _recognizer.getSymbolName(results[i].Id, configFileDirectory);
                Log.e("jni", _recognizer.getSymbolName(results[i].Id, configFileDirectory) + " ShapeAID = " + results[i].Id + " Confidence = " + results[i].Confidence);
            }

            StrokeResultCount = results.length;

            return new recognized_data(results);
        } else {
            return null;
        }

    }

    private static List<Stroke> merge_strokes(List<Stroke> strokes) {
        List<Stroke> return_strokeList = new ArrayList<>();
        Stroke merged_stroke = new Stroke();
        for(Stroke mstroke:strokes){
            for (int i=0;i<mstroke.getPoints().size();i++){
                merged_stroke.addPoint(mstroke.getPointAt(i));
            }
        }
        return_strokeList.add(merged_stroke);
        return return_strokeList;
    }

    public static class recognized_data{
        String configFileDirectory = _recognizer.getLipiDirectory() + "/projects/alphanumeric/config/";
        String[] characterArr;
        float[] confidenceArr;
        public recognized_data(LipitkResult[] results){
            characterArr = new String[results.length];
            confidenceArr = new float[results.length];
            for(int i=0;i<results.length;i++){
                characterArr[i] = _recognizer.getSymbolName(results[i].Id, configFileDirectory);
                confidenceArr[i] = results[i].Confidence;
            }
        }

        public String getCharIndex(int i){
            return characterArr[i];
        }
        public float getConfidenceIndex(int i){
            return confidenceArr[i];
        }
    }

}