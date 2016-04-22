package com.project.nicki.displaystabilizer.UI.UIv1;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


import com.canvas.Stroke;
import com.project.nicki.displaystabilizer.contentprovider.utils.TouchCollect;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3;

import java.util.ArrayList;
import java.util.List;

public class UIv1_view_view extends View {

    public List<List<stabilize_v3.Point>> ori_pending_to_draw = new ArrayList<>();
    public List<List<stabilize_v3.Point>> sta_pending_to_draw = new ArrayList<>();

    public  int drawing = 3;
    public  Paint paint2 = new Paint();
    public  Path path2 = new Path();
    public  Path path3 = new Path();
    public  Rect rectangle;
    public  Handler clean_and_refresh;
    public  Handler refresh;
    public  Paint paint = new Paint();
    public  Paint paint3 = new Paint();
    public List<stabilize_v3.Point> incremental = new ArrayList<>();
    public Path path = new Path();
    protected Context mContext;




    public UIv1_view_view(Context context) {
        super(context);
        this.mContext = context.getApplicationContext();
    }

    public UIv1_view_view(Context context, AttributeSet attrs) {
        super(context, attrs);


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

        try {
            draw_ListofPaths(canvas,path3, paint2,ori_pending_to_draw);
            draw_ListofPaths(canvas,path2,paint3, sta_pending_to_draw);

        }catch (Exception ex){
            //Log.e("onDraw",String.valueOf(ex));
        }
    }

    public void draw_ListofPaths(Canvas canvas,Path path,Paint paint,List<List<stabilize_v3.Point>> pending_to_draw){
        path= new Path();
        for(List<stabilize_v3.Point> impending_to_draw:pending_to_draw){
            try {
                Log.e("TESTING",String.valueOf("THE SIZE: "+impending_to_draw.size()));
            }catch (Exception ex){

            }

            drawCanvas(canvas, path,impending_to_draw,paint);
        }
    }

    public void drawStrokes(TouchCollect.StabilizeResult mrecognized_data){
        ori_pending_to_draw = new ArrayList<>();
        sta_pending_to_draw = new ArrayList<>();
        List<List<SensorCollect.sensordata>> ori_char = mrecognized_data.ori_Online_todraw_char;
        List<List<SensorCollect.sensordata>> sta_char = mrecognized_data.sta_Online_todraw_char;
        for (List<SensorCollect.sensordata> mchar : ori_char) {
            ori_pending_to_draw.add(init.initTouchCollection.sensordataList2pntList(mchar));
        }
        for (List<SensorCollect.sensordata> mchar : sta_char) {
            sta_pending_to_draw.add(init.initTouchCollection.sensordataList2pntList(mchar));
        }
        Log.e("draw",String.valueOf(ori_char.size()+" "+sta_char.size()));
        invalidate();
    }

    public List<List<stabilize_v3.Point>> moveStroke(String mode){
        List<List<stabilize_v3.Point>> returnList = new ArrayList<>();
        if(mode == "ori"){

        }
        return returnList;
    }

    //todraw
    private void drawCanvas(Canvas canvas,Path mpath, List<stabilize_v3.Point> pts,Paint mpaint) {
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
            canvas.drawPath(mpath, mpaint);
        } else {
            if (pts.size() == 1) {
                stabilize_v3.Point point = pts.get(0);
                canvas.drawCircle(point.x, point.y, 2, mpaint);
            }
        }
    }


    private  List<Stroke> merge_strokes(List<Stroke> strokes) {
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


}