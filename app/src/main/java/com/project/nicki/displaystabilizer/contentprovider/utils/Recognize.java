package com.project.nicki.displaystabilizer.contentprovider.utils;

import android.content.Context;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;

import com.canvas.LipiTKJNIInterface;
import com.canvas.LipitkResult;
import com.canvas.Stroke;
import com.project.nicki.displaystabilizer.dataprocessor.SensorCollect;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nickisverygood on 4/21/2016.
 */
public class Recognize {
    //recognize
    //Context contextlipi ;
    //File externalFileDir = contextlipi.getExternalFilesDir(null);
    String path = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    //String path = externalFileDir.getPath();
    private LipiTKJNIInterface _lipitkInterface;
    private LipiTKJNIInterface _recognizer = null;
    public static int StrokeResultCount = 0;


    public Recognize() {
        _lipitkInterface = new LipiTKJNIInterface(path, "SHAPEREC_ALPHANUM");
        _lipitkInterface.initialize();
        _recognizer = _lipitkInterface;
    }


    public String[] recognize_stroke(List<List<SensorCollect.sensordata>> lists) {
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


            Stroke[] _recognitionStrokes = new Stroke[_strokes.size()];
            for (int s = 0; s < _strokes.size(); s++)
                _recognitionStrokes[s] = _strokes.get(s);

            _recognitionStrokes = new Stroke[0];
            _recognitionStrokes[0] = _strokes.get(0);

            LipitkResult[] results = _recognizer.recognize(_recognitionStrokes);
            String configFileDirectory = _recognizer.getLipiDirectory() + "/projects/alphanumeric/config/";
            for (LipitkResult result : results) {
                Log.e("jni", _recognizer.getSymbolName(results[0].Id, configFileDirectory) + " ShapeAID = " + result.Id + " Confidence = " + result.Confidence);
            }

            StrokeResultCount = results.length;

            return character;
        } else {
            return null;
        }

    }
}
