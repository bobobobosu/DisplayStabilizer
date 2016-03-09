package com.project.nicki.displaystabilizer.contentprovider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.project.nicki.displaystabilizer.stabilization.stabilize_v2_1;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v3;

import java.util.ArrayList;
import java.util.List;

public class DemoDraw2 extends View {
    private static final String TAG = "DemoDraw";
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

    public DemoDraw2(Context context) {
        super(context);
        this.mContext = context.getApplicationContext();
    }

    public DemoDraw2(Context context, AttributeSet attrs) {
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
        Log.d(TAG, String.valueOf(stabilize_v2_1.toDraw.size()));
        drawCanvas(canvas, stabilize_v2_1.toDraw);
        canvas.drawPath(path, paint);
        canvas.drawPath(path2, paint2);
    }


    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        float eventX = event.getX();
        float eventY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.reset();
                path2.reset();
                path3.reset();
                drawing = 0;
                new passTouch(event);
                path.moveTo(eventX, eventY);
                return true;
            case MotionEvent.ACTION_MOVE:
                new passTouch(event);
                path.lineTo(eventX, eventY);
                break;
            case MotionEvent.ACTION_UP:
                new passTouch(event);
                drawing = 2;
                // nothing to do
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
                stabilize_v2_1.getDraw.sendMessage(msgDRAWING);
            }
        }
    }
}