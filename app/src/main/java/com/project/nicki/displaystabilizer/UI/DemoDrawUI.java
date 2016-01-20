package com.project.nicki.displaystabilizer.UI;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
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
import com.project.nicki.displaystabilizer.dataprocessor.proCamera;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v2;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

public class DemoDrawUI extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "getFrontcam";
    public static Handler DrawStabilizerHandler;
    /////from getFrontcam
    public static Mat curMat;
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

    public Mat preMat;
    public Mat nxtMat;
    public MatOfKeyPoint preKeypoints, nxtKeypoints;
    DemoDraw DD;
    private ArrayAdapter<String> integralList;
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    private Mat mRgba, mGray;
    private HandlerThread mHandlerThread;
    private Runnable mRunnable = new proCamera();
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
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

        DrawStabilizerHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.getData() != null) {
                    runOnUiThread(new overridePrecessed(msg));
                    Message msg2 = new Message();
                    msg2.what = 1;
                    DemoDraw.mhandler.sendMessage(msg2);
                    //new Thread(new overridePrecessed(msg)).start();
                }

            }
        };

        //from getFrontcam
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_demo_draw_ui);


        //TEXTVIEW
        mperformance = (TextView) findViewById(R.id.performance);
        //mlog_draw = (TextView) findViewById(R.id.log_draw);
        //mlog_cam = (TextView) findViewById(R.id.log_cam);
        mlog_acce = (TextView) findViewById(R.id.log_acce);
        mlog_gyro = (TextView) findViewById(R.id.log_gyro);
        //mseekBar_cX = (SeekBar) findViewById(R.id.seekBar_cX);
        /*
        mseekBar_cX.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                stabilize_v2.setcX((float) seekBar.getProgress() - 10000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        */
        //mseekBar_cY = (SeekBar) findViewById(R.id.seekBar_cY);
        /*
        mseekBar_cY.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                stabilize_v2.setcY((float) seekBar.getProgress() - 10000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
*/
        integralspinner = (Spinner) findViewById(R.id.integralspinner);
        String[] integralmethods = {"NoShake", "RK4", "Euler"};
        integralList = new ArrayAdapter<String>(DemoDrawUI.this, R.layout.support_simple_spinner_dropdown_item, integralmethods);
        integralspinner.setAdapter(integralList);
        integralspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                proAcceGyroCali.selectedMethod = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                proAcceGyroCali.selectedMethod = 0;
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

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.demo_draw_camera_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
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

    //from getFrontcam
    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (DemoDraw.drawing < 2) {
            Log.d(TAG, "onCameraFrame");
            //mRgba = inputFrame.gray();
            curMat = inputFrame.gray();
            //proCameraHandler = new Handler(mHandlerThread.getLooper());
            //proCameraHandler.post(mRunnable);

            //Runnable mproCamera = new proCamera() ;
            //Thread thread = new Thread(mproCamera);
            //thread.start();
            Log.d("cameracamera ", "QQQQQQQQQQQQQQQQQQQQQQ ");
            mRunnable.run();
        }
        return proCamera.proMat;
    }
    ///////////


    public class overridePrecessed implements Runnable {
        public Message msg;

        public overridePrecessed(Message mmsg) {
            this.msg = mmsg;
        }

        @Override
        public void run() {
            Log.d(TAG, "This is Override");
            Bundle bundle;
            if (msg.getData() != null) {
                bundle = msg.getData();
                float[][] DrawPoints = (float[][]) bundle.getSerializable("DrawPoints");
                for (int j = 0; j < DrawPoints.length - 1; j++) {
                    Log.d(TAG, "aaaaaaaaaaaaaaaaa " + DrawPoints[j][0] + " " + DrawPoints[j][1]);
                }
                //Log.d(TAG, "This is Override    " + String.valueOf(DrawPoints[2][0]) + " " + String.valueOf(DrawPoints[0][0]));

                DemoDraw.paint2.setColor(Color.BLUE);

                for (int i = 1; i < DrawPoints.length - 1; i++) {
                    DemoDraw.path2.moveTo(DrawPoints[i][0], DrawPoints[i][1]);
                    DemoDraw.path2.lineTo(DrawPoints[i + 1][0], DrawPoints[i + 1][1]);
                }
            }

        }
    }

}







