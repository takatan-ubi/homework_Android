package com.example.ubi;

import android.Manifest;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

public class MainActivity extends Activity implements Runnable, SensorEventListener {
    SensorManager sm;
    TextView tv;
    Button recordStartButton,recordStopButton;

    RadioGroup radioGroup;
    RadioButton radioButton;

    Handler h;
    double gx, gy, gz, a;
    boolean isRecording = false;
    String filename = "Standing.csv";
    String state ="Standing";
    List<String> stateList = Arrays.asList("Standing","Walking","Running");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout ll = new LinearLayout(this);
        setContentView(R.layout.activity_main);

        h = new Handler();
        h.postDelayed(this, 500);

        // 加速度データの表示テキストウィンド
        tv = (TextView)findViewById(R.id.SenserDataText) ;

        // データ記録時の状態の選択をするラジオボタン
        radioGroup = (RadioGroup)findViewById(R.id.RadioGroup);

        // デバッグ用　状態表示
        final TextView stateText = (TextView)findViewById(R.id.stateText);

        //データ記録開始,停止ボタン
        recordStartButton = (Button)findViewById(R.id.recordStartButton);
        recordStopButton = (Button)findViewById(R.id.recordStopButton);


        recordStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isRecording){
                    stateText.setText("記録中");
                    isRecording = true;
                }
            }
        });

        recordStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isRecording){
                    stateText.setText("待機中");
                    isRecording = false;
                }
            }
        });

        //学習データ保存用ファイルの作成
//        String output =
//                "@relation smartphoneZombie\n" +
//                "\n" +
//                "@attribute x numeric\n" +
//                "@attribute y numeric\n" +
//                "@attribute z numeric\n" +
//                "@attribute a numeric\n" +
//                "@attribute state {Standing, Walking, Running}\n" +
//                "\n" +
//                "@DATA\n";
//        FileOutputStream outputStream;
//        try {
//            filename = "Standing.arff";
//            outputStream = openFileOutput(filename, MODE_APPEND);
//            outputStream.write(output.getBytes());
//            File file = new File(filename);
//            if (file.exists()){
//                Log.v("test","ファイルは存在します");
//                file.delete();
//            }else{
//                Log.v("test","ファイルは存在しません");
//            }
//            outputStream.close();
//
//            filename = "Walking.arff";
//            outputStream = openFileOutput(filename, MODE_APPEND);
//            outputStream.write(output.getBytes());
//            outputStream.close();
//
//            filename = "Running.arff";
//            outputStream = openFileOutput(filename, MODE_APPEND);
//            outputStream.write(output.getBytes());
//            outputStream.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public void checkRadioButton(View v){
        int radioId = radioGroup.getCheckedRadioButtonId();
        radioButton = findViewById(radioId);
        filename = radioButton.getText()+".csv";
        state = radioButton.getText()+"";
    }

    public void startLearning(View v){
        try {
            Log.v("test","startleanin");

            //学習データ保存用ファイルの作成
            String filetop =
                    "@relation smartphoneZombie\n" +
                    "\n" +
                    "@attribute x numeric\n" +
                    "@attribute y numeric\n" +
                    "@attribute z numeric\n" +
                    "@attribute a numeric\n" +
                    "@attribute state {Standing, Walking, Running}\n" +
                    "\n" +
                    "@DATA\n";
            FileOutputStream learnoutputStream;
            learnoutputStream = openFileOutput("learnData.arff", MODE_APPEND);
            learnoutputStream.write(filetop.getBytes("UTF-8"));

            for(String csvFileName : stateList){
                FileInputStream inputStream = openFileInput(csvFileName+".csv");
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferReader = new BufferedReader(inputStreamReader);

                String line = "";
                while ((line = bufferReader.readLine()) != null) {
                    learnoutputStream.write(line.getBytes("UTF-8"));
                    learnoutputStream.write("\n".getBytes("UTF-8"));
                }
                bufferReader.close();
                Log.v("test",csvFileName+"finish");
            }
            learnoutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
            Log.v("test","erroer");
        }



    }

    @Override
    public void run() {
        tv.setText("X-axis : " + gx + "\n"
                + "Y-axis : " + gy + "\n"
                + "Z-axis : " + gz + "\n"
                + "XYZ    : " + a + "\n");
        h.postDelayed(this, 500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors =
                sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (0 < sensors.size()) {
            sm.registerListener(this, sensors.get(0),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        h.removeCallbacks(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(isRecording) {
            gx = event.values[0];
            gy = event.values[1];
            gz = event.values[2];
            a = (float)Math.sqrt(gx*gx+gy*gy+gz*gz);

            String output = BigDecimal.valueOf(gx).toPlainString() + ","
                    + BigDecimal.valueOf(gy).toPlainString() + ","
                    + BigDecimal.valueOf(gz).toPlainString() + ","
                    + BigDecimal.valueOf(a).toPlainString() + ","
                    + state +"\n";
            FileOutputStream outputStream;
            try {
                outputStream = openFileOutput(filename, MODE_APPEND);
                outputStream.write(output.getBytes("UTF-8"));
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 0x01;
    private static String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };




}
