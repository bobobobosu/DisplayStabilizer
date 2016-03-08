package com.project.nicki.displaystabilizer.dataprocessor.utils;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StreamCorruptedException;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Created by nickisverygood on 3/6/2016.
 */
public class LogCSV {
    public LogCSV(String input,String a,String b,Float... data){
        //init CSV logging
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        //String fileName = csvName;
        String fileName = input + ".csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        /*
        if(f.exists()){
            try{
                boolean deleted = f.delete();
            }catch (Exception ex){
                Log.d("LogCSV","Deletion Failed");
            }

        }*/
        CSVWriter writer = null;
        // File exist
        FileWriter mFileWriter = null;
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
            String mline = "";
            mline += a+",";
            mline += b+",";
            for(float istring:data){
                mline += String.valueOf(istring)+",";
            }
            mline+="\n";
            //String line = String.format("%s,%s,%s,%s,%s,%s\n", a, b, c, d, g, h);
            mFileWriter.write(mline);
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
    public static void LogCSV(String input,String a,String b,Float... data) {
        //init CSV logging
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        //String fileName = csvName;
        String fileName = input + ".csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        /*
        if(f.exists()){
            try{
                boolean deleted = f.delete();
            }catch (Exception ex){
                Log.d("LogCSV","Deletion Failed");
            }

        }*/
        CSVWriter writer = null;
        // File exist
        FileWriter mFileWriter = null;
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
            String mline = "";
            mline += a+",";
            mline += b+",";
            for(float istring:data){
                mline += String.valueOf(istring)+",";
            }
            mline+="\n";
            //String line = String.format("%s,%s,%s,%s,%s,%s\n", a, b, c, d, g, h);
            mFileWriter.write(mline);
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
}
