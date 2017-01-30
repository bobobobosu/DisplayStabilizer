package com.project.nicki.displaystabilizer.UI;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera;
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

import com.project.nicki.displaystabilizer.Odometry.CameraPreview;
import com.project.nicki.displaystabilizer.R;
import com.project.nicki.displaystabilizer.contentprovider.DemoDraw2;
import com.project.nicki.displaystabilizer.dataprocessor.proAcceGyroCali;
import com.project.nicki.displaystabilizer.stabilization.stabilize_v2_1;

import org.ejml.data.DenseMatrix64F;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.MonocularPlaneVisualOdometry;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.android.ConvertBitmap;
import boofcv.android.VisualizeImageData;
import boofcv.core.encoding.ConvertNV21;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

public class DemoDrawUI2 extends AppCompatActivity implements Camera.PreviewCallback {
    private static final String TAG = "getFrontcam";


    ////////////////////Sensor/////////////////////////////
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

    ///////////////////////Camera//////////////////.
    // Object used for synchronizing gray images
    private final Object lockGray = new Object();
    // Object used for synchronizing output image
    private final Object lockOutput = new Object();
    DemoDraw2 DD;
    //odometry init
    MonoPlaneParameters monoparas;
    DenseMatrix64F DM;
    Vector3D_F64 Vec3d;
    ////////////////////
    PkltConfig configKlt;
    ConfigGeneralDetector configDetector;
    PointTracker<ImageUInt8> tracker;
    MonocularPlaneVisualOdometry<ImageUInt8> visualOdometry;
    //myadd
    ImageUInt8 toprocess = new ImageUInt8();
    private ArrayAdapter<String> integralList;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
    private HandlerThread mHandlerThread;
    // camera and display objects
    private Camera mCamera;
    private Visualization mDraw;
    private CameraPreview mPreview;
    // computes the image gradient
    private ImageGradient<ImageUInt8, ImageSInt16> gradient = FactoryDerivative.three(ImageUInt8.class, ImageSInt16.class);
    // Two images are needed to store the converted preview image to prevent a thread conflict from occurring
    private ImageUInt8 gray1, gray2;
    private ImageSInt16 derivX, derivY;
    // Android image buffer used for displaying the results
    private Bitmap output;
    // temporary storage that's needed when converting from BoofCV to Android image buffer types
    private byte[] storage;
    // Thread where image buffer is processed
    private ThreadProcess thread;

    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }

    /**
     * Goes through the size list and selects the one which is the closest specified size
     */
    public static int closest(List<Camera.Size> sizes, int width, int height) {
        int best = -1;
        int bestScore = Integer.MAX_VALUE;

        for (int i = 0; i < sizes.size(); i++) {
            Camera.Size s = sizes.get(i);

            int dx = s.width - width;
            int dy = s.height - height;

            int score = dx * dx + dy * dy;
            if (score < bestScore) {
                best = i;
                bestScore = score;
            }
        }

        return best;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ////////////////////////Sensor/////////////////
        DD = new DemoDraw2(this);
        DD = (DemoDraw2) findViewById(R.id.view2);
        HandlerThread handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_demo_draw_ui2);
        //TEXTVIEW
        mperformance = (TextView) findViewById(R.id.performance2);
        //mlog_draw = (TextView) findViewById(R.id.log_draw);
        //mlog_cam = (TextView) findViewById(R.id.log_cam);
        mlog_acce = (TextView) findViewById(R.id.log_acce2);
        mlog_gyro = (TextView) findViewById(R.id.log_gyro2);
        integralspinner = (Spinner) findViewById(R.id.integralspinner2);
        final String[] integralmethods = {"NoShake", "RK4", "Euler"};
        integralList = new ArrayAdapter<String>(DemoDrawUI2.this, R.layout.support_simple_spinner_dropdown_item, integralmethods);
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
        mfakeposonoff = (CheckBox) findViewById(R.id.fakeposcheck2);
        mfakeposonoff.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                stabilize_v2_1.posdrawing = mfakeposonoff.isChecked();
            }
        });
        mupdatefakepos = (Button) findViewById(R.id.updatefakepos2);
        mupdatefakepos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stabilize_v2_1.updatefakepos = true;
            }
        });
        minverse = (CheckBox) findViewById(R.id.inversefakepos2);
        minverse.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (minverse.isChecked()) {
                    stabilize_v2_1.oneorminusone = -1;
                } else {
                    stabilize_v2_1.oneorminusone = 1;
                }
            }
        });
        //mOpenCvCameraView.setMaxFrameSize(800, 600);

        mHandlerThread = new HandlerThread("Deyu");
        mHandlerThread.start();
        //////////////////

        //set paras
        mMovingAvg = (EditText) findViewById(R.id.movingavg2);
        mLP = (EditText) findViewById(R.id.LP2);
        mHPa = (EditText) findViewById(R.id.HPa2);
        mHPv = (EditText) findViewById(R.id.HPv2);
        mHPp = (EditText) findViewById(R.id.HPp2);
        mMultiplier = (EditText) findViewById(R.id.multiplier2);
        mStaticOffset = (EditText) findViewById(R.id.staticoffset2);
        mApplyPara = (Button) findViewById(R.id.applyset2);
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


        /*
        /////////////Camera//////////////
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //setContentView(R.layout.video);

        // Used to visualize the results
        mDraw = new Visualization(this);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, this, true);

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);

        preview.addView(mPreview);
        preview.addView(mDraw); */
    }

    @Override
    public void onPause() {
        super.onPause();
        /////////////////////Camera//////////////////
        // stop the camera preview and all processing
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;

            thread.stopThread();
            thread = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        /////////////////////Camera////////////////////
        //setUpAndConfigureCamera();
    }

    public void onDestroy() {
        super.onDestroy();

    }

    /**
     * Sets up the camera if it is not already setup.
     */
    private void setUpAndConfigureCamera() {
        // Open and configure the camera
        mCamera = Camera.open();

        Camera.Parameters param = mCamera.getParameters();

        // Select the preview size closest to 320x240
        // Smaller images are recommended because some computer vision operations are very expensive
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        Camera.Size s = sizes.get(closest(sizes, 320, 240));
        param.setPreviewSize(s.width, s.height);
        mCamera.setParameters(param);

        // declare image buffer
        gray1 = new ImageUInt8(s.width, s.height);
        gray2 = new ImageUInt8(s.width, s.height);
        derivX = new ImageSInt16(s.width, s.height);
        derivY = new ImageSInt16(s.width, s.height);
        toprocess = new ImageUInt8(s.width, s.height);
        output = Bitmap.createBitmap(s.width, s.height, Bitmap.Config.ARGB_8888);
        storage = ConvertBitmap.declareStorage(output, storage);

        // start image processing thread
        thread = new ThreadProcess();
        thread.start();

        // Start the video feed by passing it to mPreview
        mPreview.setCamera(mCamera);


        //Odometry set
        monoparas = new MonoPlaneParameters();
        monoparas.setIntrinsic(
                new IntrinsicParameters(
                        238.35081481933594,
                        238.35081481933594,
                        0.0,
                        200.0,
                        200.0,
                        400,
                        400));
        DM = new DenseMatrix64F(new double[][]{
                {0.9999732346484106, 0.007304326750623114, 4.204729657194234E-4},
                {-0.00731641898709808, 0.9983205254687793, 0.05746823854048171},
                {-0.0, -0.0574697767392619, 0.9983472465838424}});
        Vec3d = new Vector3D_F64(-0.013122543900519043, 1.7935266821320288, -0.10324421522745254);

        monoparas.setPlaneToCamera(new Se3_F64(DM, Vec3d));

        // specify how the image features are going to be tracked
        configKlt = new PkltConfig();
        configKlt.pyramidScaling = new int[]{1, 2, 4, 8};
        configKlt.templateRadius = 3;
        configDetector = new ConfigGeneralDetector(600, 3, 1);

        tracker = FactoryPointTracker.klt(configKlt, configDetector, ImageUInt8.class, null);

        // declares the algorithm
        visualOdometry =
                FactoryVisualOdometry.monoPlaneInfinity(75, 2, 1.5, 200, tracker, ImageType.single(ImageUInt8.class));
        // Pass in intrinsic/extrinsic calibration.  This can be changed in the future.
        visualOdometry.setCalibration(monoparas);


    }

    public String inlierPercent(VisualOdometry<?> alg) {
        if (!(alg instanceof AccessPointTracks3D))
            return "";

        AccessPointTracks3D access = (AccessPointTracks3D) alg;

        int count = 0;
        int N = access.getAllTracks().size();
        for (int i = 0; i < N; i++) {
            if (access.isInlier(i))
                count++;
        }

        return String.format("%%%5.3f", 100.0 * count / N);
    }

    /**
     * Called each time a new image arrives in the buffer stream.
     */
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

        // convert from NV21 format into gray scale
        synchronized (lockGray) {
            ConvertNV21.nv21ToGray(bytes, gray1.width, gray1.height, gray1);
            ConvertNV21.nv21ToBoof(bytes, gray1.width, gray1.height, toprocess);
        }

        // Can only do trivial amounts of image processing inside this function or else bad stuff happens.
        // To work around this issue most of the processing has been pushed onto a thread and the call below
        // tells the thread to wake up and process another image
        thread.interrupt();
    }

    public void LogCSV(String Name, String a, String b, String c, String d, String g, String h) {
        FileWriter mFileWriter = null;
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

    /**
     * Draws on top of the video stream for visualizing computer vision results
     */
    private class Visualization extends SurfaceView {

        Activity activity;

        public Visualization(Activity context) {
            super(context);
            this.activity = context;

            // This call is necessary, or else the
            // draw method will not be called.
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {

            synchronized (lockOutput) {
                int w = canvas.getWidth();
                int h = canvas.getHeight();

                // fill the window and center it
                double scaleX = w / (double) output.getWidth();
                double scaleY = h / (double) output.getHeight();

                double scale = Math.min(scaleX, scaleY);
                double tranX = (w - scale * output.getWidth()) / 2;
                double tranY = (h - scale * output.getHeight()) / 2;

                canvas.translate((float) tranX, (float) tranY);
                canvas.scale((float) scale, (float) scale);

                // draw the image
                canvas.drawBitmap(output, 0, 0, null);


            }
        }
    }

    /**
     * External thread used to do more time consuming image processing
     */
    private class ThreadProcess extends Thread {

        // true if a request has been made to stop the thread
        volatile boolean stopRequested = false;
        // true if the thread is running and can process more buffer
        volatile boolean running = true;

        /**
         * Blocks until the thread has stopped
         */
        public void stopThread() {
            stopRequested = true;
            while (running) {
                thread.interrupt();
                Thread.yield();
            }
        }

        @Override
        public void run() {

            while (!stopRequested) {

                // Sleep until it has been told to wake up
                synchronized (Thread.currentThread()) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {
                    }
                }

                // process the most recently converted image by swapping image buffered
                synchronized (lockGray) {
                    ImageUInt8 tmp = gray1;
                    gray1 = gray2;
                    gray2 = tmp;
                }

                // process the image and compute its gradient
                //gradient.process(gray2, derivX, derivY);

                // render the output in a synthetic color image
                synchronized (lockOutput) {
                    //VisualizeImageData.colorizeGradient(derivX,derivY,-1,output,storage);
                    VisualizeImageData.binaryToBitmap(gray1, true, output, storage);
                }


                //my
                // Process the video sequence and output the location plus number of inliers

                ImageUInt8 image = gray1;

                if (!visualOdometry.process(image)) {
                    System.out.println("Fault!");
                    visualOdometry.reset();
                }

                Se3_F64 leftToWorld = visualOdometry.getCameraToWorld();
                Vector3D_F64 V3d = leftToWorld.getT();
                System.out.printf("Location %8.2f %8.2f %8.2f      inliers %s\n", V3d.x, V3d.y, V3d.z, inlierPercent(visualOdometry));
                Log.d("camerameasure", "Location " + String.valueOf(V3d.x) + " " + String.valueOf(V3d.y) + " " + String.valueOf(V3d.z) + "inliers " + String.valueOf(inlierPercent(visualOdometry)));
                LogCSV("camera", String.valueOf(V3d.x), String.valueOf(V3d.y), String.valueOf(V3d.z), String.valueOf(System.currentTimeMillis()), "", "");
                //init.initSensorCollection.append(new SensorCollect.sensordata(System.currentTimeMillis(), new float[]{(float) V3d.x, (float)V3d.y,(float)V3d.z}, SensorCollect.sensordata.TYPE.CAME));
                mDraw.postInvalidate();
            }
            running = false;
        }
    }

}







