package com.project.nicki.displaystabilizer.contentprovider;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.project.nicki.displaystabilizer.dataprocessor.proDataFlow;

public class DemoDraw extends View {
    private static final String TAG = "DemoDraw";
    public static boolean drawing = false;
    public static Paint paint2 = new Paint();
    public static Path path2 = new Path();
    public static Handler mhandler;
    public Paint paint = new Paint();
    public Path path = new Path();
    protected Context mContext;

    public DemoDraw(Context context) {
        super(context);
        this.mContext = context.getApplicationContext();
    }

    public DemoDraw(Context context, AttributeSet attrs) {
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

        mhandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    invalidate();
                }
            }
        };

    }

    @Override
    protected void onDraw(Canvas canvas) {

        //Bitmap bitmap=Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        //Canvas mcanvas=new Canvas(bitmap);
        //canvas.drawColor(Color.WHITE);
        canvas.drawPath(path, paint);
        canvas.drawPath(path2, paint2);


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float eventX = event.getX();
        float eventY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "AAAA down");
                Message msgSTART = new Message();
                msgSTART.what = 0;

                float[] dataSTART = new float[2];
                long currTimeSTART = System.currentTimeMillis();
                Bundle drawposBundleSTART = new Bundle();
                dataSTART[0] = eventX;
                dataSTART[1] = eventY;
                drawposBundleSTART.putFloatArray("Draw", dataSTART);
                drawposBundleSTART.putLong("Time", currTimeSTART);
                msgSTART.setData(drawposBundleSTART);

                proDataFlow.drawHandler.sendMessage(msgSTART);
                //stabilize_v1.getDatas.sendMessage(msgSTART);

                drawing = true;
                path.moveTo(eventX, eventY);
                return true;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "AAAA Drawing");
                Message msgDRAWING = new Message();
                msgDRAWING.what = 1;

                float[] dataDRAWING = new float[2];
                long currTimeDRAWING = System.currentTimeMillis();
                Message msgDrawing = new Message();
                Bundle drawposBundleDRAWING = new Bundle();
                dataDRAWING[0] = eventX;
                dataDRAWING[1] = eventY;
                drawposBundleDRAWING.putFloatArray("Draw", dataDRAWING);
                drawposBundleDRAWING.putLong("Time", currTimeDRAWING);
                msgDRAWING.setData(drawposBundleDRAWING);

                proDataFlow.drawHandler.sendMessage(msgDRAWING);
                //stabilize_v1.getDatas.sendMessage(msgDRAWING);

                drawing = true;
                path.lineTo(eventX, eventY);
                break;
            case MotionEvent.ACTION_UP:


                Log.d(TAG, "AAAA up");
                Message msgSTOP = new Message();

                float[] dataSTOP = new float[2];
                long currTimeSTOP = System.currentTimeMillis();
                Bundle drawposBundleSTOP = new Bundle();
                dataSTOP[0] = eventX;
                dataSTOP[1] = eventY;
                drawposBundleSTOP.putFloatArray("Draw", dataSTOP);
                drawposBundleSTOP.putLong("Time", currTimeSTOP);
                msgSTOP.setData(drawposBundleSTOP);


                msgSTOP.what = 2;
                proDataFlow.drawHandler.sendMessage(msgSTOP);
                //stabilize_v1.getDatas.sendMessage(msgSTOP);

                drawing = false;
                // nothing to do
                break;
            default:
                drawing = false;
                return false;
        }

        // Schedules a repaint.
        invalidate();
        return true;
    }

}