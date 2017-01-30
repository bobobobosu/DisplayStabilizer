package com.project.nicki.displaystabilizer.UI.UIv1;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.project.nicki.displaystabilizer.R;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw3;
import com.project.nicki.displaystabilizer.dataprocessor.utils.Calibration;
import com.project.nicki.displaystabilizer.dataprovider.representation.Quaternion;
import com.project.nicki.displaystabilizer.dataprovider.udpBroadcast;
import com.project.nicki.displaystabilizer.globalvariable;
import com.project.nicki.displaystabilizer.init;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v4;

import java.util.ArrayList;
import java.util.List;

import static com.project.nicki.displaystabilizer.init.mudpbroadcast;

/**
 * UIv1_draw0.java
 * DO:
 * # initialize widgets
 * # simple calibration by average & update dialogue
 */

public class UIv1_draw0 extends AppCompatActivity {
    //// # initialize widgets: declartions
    //display results
    public static TextView view_ORI_CHAR;
    public static TextView view_ORI_CONF;
    public static TextView view_STA_CHAR;
    public static TextView view_STA_CONF;
    public static TextView ipport;
    public static Button button_Calibrate;
    DemoDraw3 mDemoDraw;
    //// # simple calibration by average & update dialogue: declaration
    public static Handler update_results;
    public static Handler calibrate;
    public static Handler dialog_recognizing;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.d("ip",ip);

        setTitle("NoDistort Demo");  // provide compatibility to all the versions
        setContentView(R.layout.activity_uiv1_draw0);
        //// # initialize widgets: control
        //init draw view
        mDemoDraw = new DemoDraw3(this);
        mDemoDraw = (DemoDraw3) findViewById(R.id.view_DemoDraw3);

        //set spen hover listener
        mDemoDraw.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                //x,y in meters
                float Xval = pxToM(-event.getX() + getWindowManager().getDefaultDisplay().getWidth(), getBaseContext());
                float Yval = pxToM(-event.getY() + getWindowManager().getDefaultDisplay().getHeight(), getBaseContext());
                init.initglobalvariable.sHoverVal.add(event.getEventTime(),new float[]{Xval,Yval});
                Log.e("HOVER", String.valueOf(
                        pxToM(getWindowManager().getDefaultDisplay().getWidth(), getBaseContext()) + " " +
                                pxToM(getWindowManager().getDefaultDisplay().getHeight(), getBaseContext()) + " " +
                                init.initglobalvariable.HoverVal[0]));
                return true;
            }
        });


        //init widgets
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        view_ORI_CHAR = (TextView) findViewById(R.id.ori_char);
        view_ORI_CONF = (TextView) findViewById(R.id.ori_conf);
        view_STA_CHAR = (TextView) findViewById(R.id.sta_char);
        view_STA_CONF = (TextView) findViewById(R.id.sta_conf);
        ipport = (EditText) findViewById(R.id.ipport);
        button_Calibrate = (Button) findViewById(R.id.button_calibrate);
        button_Calibrate.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrate.sendEmptyMessage(0);
            }
        });
        setSupportActionBar(toolbar);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                init.initTouchCollection.save_and_clean();
                Snackbar.make(view, "Recognized & Cleaned", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        //// # simple calibration by average & update dialogue
        //recognize dialog
        final ProgressDialog[] ddialog_recognizing = new ProgressDialog[1];
        dialog_recognizing = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0) {
                    ddialog_recognizing[0] = ProgressDialog.show(UIv1_draw0.this,
                            "辨識中", "請等待...", true);
                } else {
                    ddialog_recognizing[0].dismiss();
                }
            }
        };


        //init calibration dialog
        calibrate = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                init.initglobalvariable.ipportVal = ipport.getText().toString();

                mudpbroadcast.interrupt();mudpbroadcast=null;
                mudpbroadcast = new Thread(new udpBroadcast(getBaseContext(), init.initglobalvariable.ipportVal));
                mudpbroadcast.start();

                if (msg.what != 5 && false) {
                    final ProgressDialog dialog = ProgressDialog.show(UIv1_draw0.this,
                            "校正中", "請靜置等待...", true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            init.initglobalvariable.calibrate_isrunning = true;
                            try {

                                Runnable MagnetometerCalibration = new com.project.nicki.displaystabilizer.dataprovider.MagnetometerCalibration();
                                MagnetometerCalibration.run();

                                float[] rawcali = new float[]{0, 0, 0};
                                List<float[]> caliAcceData = new ArrayList<float[]>();
                                List<globalvariable.CircularBuffer2> CalibrationData = new ArrayList<globalvariable.CircularBuffer2>();


                                boolean poseChanged = true;
                                while (CalibrationData.size() < 5) {
                                    if (poseChanged == true) {
                                        //try {
                                        globalvariable.CircularBuffer2 currbuffer = new globalvariable.CircularBuffer2(500);
                                        while (currbuffer.data.size() < 500) {
                                            if (init.initglobalvariable.StaticVal == true && init.initglobalvariable.AcceBuffer.hasnew && !currbuffer.full()) {
                                                caliAcceData.add(init.initglobalvariable.AcceBuffer.data.get(init.initglobalvariable.AcceBuffer.data.size() - 1));
                                                currbuffer.add(init.initglobalvariable.AcceBuffer.data.get(init.initglobalvariable.AcceBuffer.data.size() - 1));
                                                init.initglobalvariable.AcceBuffer.hasnew = false;
                                                Log.d("cali", String.valueOf(CalibrationData.size() + "　" + currbuffer.data.size()));
                                            } else if (currbuffer.full()) {
                                            }

                                        }
                                        CalibrationData.add(currbuffer);
                                        //}catch (Exception ex){
                                        //break;
                                        //Log.e("error", String.valueOf(ex));
                                        //}
                                    }
                                    if (!init.initglobalvariable.StaticVal) {
                                        poseChanged = true;
                                    } else {
                                        poseChanged = false;
                                    }


                                }


                                init.initglobalvariable.AccelerometerCali = rawcali;
                                Runnable r = new Calibration(CalibrationData);
                                r.run();
                                dialog.dismiss();
                            } catch (Exception ex) {
                                Log.d("msg", String.valueOf(ex));
                                dialog.dismiss();
                            }

                            init.initglobalvariable.calibrate_isrunning = false;
                        }
                    }).start();
                }
            }
        };
        //update widgets
        ipport.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        update_results = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                view_ORI_CHAR.setText(msg.getData().getString("ORI_CHAR"));
                view_ORI_CHAR.setTextColor(Color.BLACK);
                //view_ORI_CONF.setText(String.valueOf("conf: " + msg.getData().getFloat("ORI_CONF")));
                view_ORI_CONF.setText(String.valueOf("難:" + Math.ceil(msg.getData().getFloat("ORI_CONF") * 100) + " 分"));
                view_ORI_CONF.setTextColor(Color.BLACK);
                view_STA_CHAR.setText(msg.getData().getString("STA_CHAR"));
                view_STA_CHAR.setTextColor(Color.RED);
                //view_STA_CONF.setText(String.valueOf("conf: " + msg.getData().getFloat("STA_CONF")));
                view_STA_CONF.setText(String.valueOf("難:" + Math.ceil(msg.getData().getFloat("STA_CONF") * 100) + " 分"));
                view_STA_CONF.setTextColor(Color.RED);
                if (msg.getData().getFloat("ORI_CONF") > msg.getData().getFloat("STA_CONF")) {
                    //view_ORI_CONF.setText(String.valueOf("*conf: " + msg.getData().getFloat("ORI_CONF")));
                    view_ORI_CONF.setText(String.valueOf("易:" + Math.ceil(msg.getData().getFloat("ORI_CONF") * 100) + " 分"));
                } else {
                    //view_STA_CONF.setText(String.valueOf("*conf: " + msg.getData().getFloat("STA_CONF")));
                    view_STA_CONF.setText(String.valueOf("易:" + Math.ceil(msg.getData().getFloat("STA_CONF") * 100) + " 分"));
                    if (Math.ceil(msg.getData().getFloat("STA_CONF") * 100) > 40) {
                        view_STA_CONF.setText(String.valueOf("優秀!" + Math.ceil(msg.getData().getFloat("STA_CONF") * 100) + " 分"));
                        view_STA_CONF.setTextColor(Color.GREEN);
                    }
                }
            }
        };


        //start calibration
        //calibrate.sendEmptyMessage(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.history0:
                Intent goto_UIv1_compare0 = new Intent();
                goto_UIv1_compare0.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                goto_UIv1_compare0.setClass(UIv1_draw0.this, UIv1_compare0.class);
                startActivity(goto_UIv1_compare0);
                break;
            case R.id.setttings0:

                Intent goto_UIv1_settings0 = new Intent();
                goto_UIv1_settings0.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                goto_UIv1_settings0.setClass(UIv1_draw0.this, UIv1_setttings0.class);
                startActivity(goto_UIv1_settings0);

                /*
                Intent goto_Canvas1 = new Intent();
                overridePendingTransition(0, 0);
                goto_Canvas1.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                goto_Canvas1.setClass(UIv1_draw0.this, Canvas1.class);
                startActivity(goto_Canvas1);
                */
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public static float pxToM(final float px, final Context context) {
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();

        //mm
        //return px / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1, dm);
        //m
        return px / (1000 * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1, dm));
    }
}
