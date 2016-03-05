package com.project.nicki.displaystabilizer.UI;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.project.nicki.displaystabilizer.R;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw;
import com.project.nicki.displaystabilizer.dataprocessor.proAcceGyroCali;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v2;


public class DemoDrawUI extends AppCompatActivity {
    private static final String TAG = "getFrontcam";

    /////from getFrontcam

    public static TextView mlog_draw, mlog_cam, mlog_acce;
    public static TextView mlog_gyro;
    public static Handler proCameraHandler, goto_results;
    public static Handler UIHandler;
    public static SeekBar mseekBar_cX;
    public static SeekBar mseekBar_cY;
    public static Spinner integralspinner;
    //set para
    public static EditText mMovingAvg;
    public static EditText mLP;
    public static EditText mHPa;
    public static EditText mHPv;
    public static EditText mHPp;
    public static EditText mStaticOffset;
    public static EditText mMultiplier;
    public static Button mApplyPara;
    public static TextView mperformance;
    //fakepos
    public static CheckBox mfakeposonoff;
    public static Button mupdatefakepos;
    public static CheckBox minverse;

    static {
        UIHandler = new Handler(Looper.getMainLooper());
    }


    DemoDraw DD;
    private ArrayAdapter<String> integralList;

    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;

    private HandlerThread mHandlerThread;


    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }
////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        int progress = 0;
        DD = new DemoDraw(this);
        DD = (DemoDraw) findViewById(R.id.view);
        HandlerThread handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();
        //DD.setVisibility(DemoDraw.VISIBLE);
        //setContentView(DD);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_demo_draw_ui);


        //TEXTVIEW
        mperformance = (TextView) findViewById(R.id.performance);
        //mlog_draw = (TextView) findViewById(R.id.log_draw);
        //mlog_cam = (TextView) findViewById(R.id.log_cam);
        mlog_acce = (TextView) findViewById(R.id.log_acce);
        mlog_gyro = (TextView) findViewById(R.id.log_gyro);
        integralspinner = (Spinner) findViewById(R.id.integralspinner);
        final String[] integralmethods = {"NoShake", "RK4", "Euler"};
        integralList = new ArrayAdapter<String>(DemoDrawUI.this, R.layout.support_simple_spinner_dropdown_item, integralmethods);
        integralspinner.setAdapter(integralList);

        integralspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                proAcceGyroCali.selectedMethod = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                proAcceGyroCali.selectedMethod = 1;
            }
        });
        integralspinner.post(new Runnable() {
            @Override
            public void run() {
                integralspinner.setSelection(1);
            }
        });
        //fakepos
        mfakeposonoff = (CheckBox) findViewById(R.id.fakeposcheck);
        mfakeposonoff.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                stabilize_v2.posdrawing = mfakeposonoff.isChecked();
            }
        });
        mupdatefakepos = (Button) findViewById(R.id.updatefakepos);
        mupdatefakepos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stabilize_v2.updatefakepos = true;
            }
        });
        minverse = (CheckBox) findViewById(R.id.inversefakepos);
        minverse.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (minverse.isChecked()) {
                    stabilize_v2.oneorminusone = -1;
                } else {
                    stabilize_v2.oneorminusone = 1;
                }
            }
        });
        //mOpenCvCameraView.setMaxFrameSize(800, 600);

        mHandlerThread = new HandlerThread("Deyu");
        mHandlerThread.start();
        //////////////////

        //set paras
        mMovingAvg = (EditText) findViewById(R.id.movingavg);
        mLP = (EditText) findViewById(R.id.LP);
        mHPa = (EditText) findViewById(R.id.HPa);
        mHPv = (EditText) findViewById(R.id.HPv);
        mHPp = (EditText) findViewById(R.id.HPp);
        mMultiplier = (EditText) findViewById(R.id.multiplier);
        mStaticOffset = (EditText) findViewById(R.id.staticoffset);
        mApplyPara = (Button) findViewById(R.id.applyset);
        mMovingAvg.setText(String.valueOf(50));
        mLP.setText(String.valueOf(0.9));
        mHPa.setText(String.valueOf(0.9));
        mHPv.setText(String.valueOf(0.9));
        mHPp.setText(String.valueOf(0.9));
        mStaticOffset.setText(String.valueOf(0.0017));
        mMultiplier.setText(String.valueOf(0.055));
        Message message;
        String obj = "OK";
        message = proAcceGyroCali.applypara.obtainMessage(1, obj);
        proAcceGyroCali.applypara.sendMessage(message);
        mApplyPara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        Message message;
                        String obj = "OK";
                        message = proAcceGyroCali.applypara.obtainMessage(1, obj);
                        proAcceGyroCali.applypara.sendMessage(message);

                    }
                };
                thread.start();
                thread = null;
            }
        });

    }


    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public void onDestroy() {
        super.onDestroy();

    }



}







