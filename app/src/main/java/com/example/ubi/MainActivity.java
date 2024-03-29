package com.example.ubi;

import android.Manifest;
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
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class MainActivity extends Activity implements Runnable, SensorEventListener {
    SensorManager sm;
    TextView tv;
    Button recordStartButton, learnButton;

    RadioGroup radioGroup;
    RadioButton radioButton;

    Handler h;
    double gx, gy, gz, acceleration;
    boolean isRecording = false;
    String filename = "Standing.csv";
    String state ="Standing";
    int sampleNum = 0;
    List<String> stateList = Arrays.asList("Standing","Walking","Running");
    TextView stateText;


    Evaluation eval;
    Instances instances;
    Classifier classifier;
    Attribute a;
    boolean isLearned = false;

    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        h = new Handler();
        h.postDelayed(this, 60);

        // 加速度データの表示テキストウィンド
        tv = (TextView)findViewById(R.id.SenserDataText);

        // データ記録時の状態の選択をするラジオボタン
        radioGroup = (RadioGroup)findViewById(R.id.RadioGroup);

        // 状態表示
        stateText = (TextView)findViewById(R.id.stateText);
        stateText.setText("待機中");

        //データ記録開始ボタン
        recordStartButton = (Button)findViewById(R.id.recordStartButton);
        learnButton = (Button)findViewById(R.id.learnButton);

        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setMax(1000);
        progressBar.setProgress(sampleNum);
        progressBar.setMin(0);


        recordStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                if(!isRecording){
                    String path = "/data/data/com.example.ubi/files/"+state+".csv";
                    File f =   new File(path);
                    if (f.exists()){
                        f.delete();
                        Log.v("test","file delete " + state + ".csv");
                    }
                    stateText.setText("記録中");
                    isRecording = true;
                }
            }
        });
    }

    public void recordStop(){
        if(isRecording){
            stateText.setText("待機中");
            isRecording = false;
            sampleNum = 0;
        }
    }

    public void checkRadioButton(View v){
        int radioId = radioGroup.getCheckedRadioButtonId();
        radioButton = findViewById(radioId);
        filename = radioButton.getText()+".csv";
        state = radioButton.getText()+"";

    }

    public void startLearning(View v){


        if(isLearned){
            isLearned = false;
            stateText.setText("待機中");
            learnButton.setText("LEARN");
            return;
        }

        learnButton.setText("STOP");

        try {
            changeCsvToArff();

            stateText.setText("学習中");
            //学習データの生成
            FileInputStream inputStream = openFileInput("learnData.arff");
            DataSource source = new DataSource(inputStream);
            instances = source.getDataSet();
            instances.setClassIndex(1);

            //分類機の生成
            classifier = new J48();
            classifier.buildClassifier(instances);
            eval = new Evaluation(instances);
            eval.evaluateModel(classifier, instances);      //モデルと学習データから評価
            Log.v("test",eval.toSummaryString());
            a = new Attribute("a", 0);

            judgment();

            isLearned = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void judgment(){
        try {
            Instance instance = new DenseInstance(2);
            instance.setValue(a, acceleration);
            instance.setDataset(instances);
            double result = classifier.classifyInstance(instance);
            stateText.setText(stateList.get((int) result));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void changeCsvToArff(){
        try {
            Log.v("test","startleanin");

            //学習データ保存用ファイルの作成
            String filetop =
                    "@relation smartphoneZombie\n" +
                            "\n" +
                            "@attribute a numeric\n" +
                            "@attribute state {Standing, Walking, Running}\n" +
                            "\n" +
                            "@DATA\n";
            File file = new File("/data/data/com.example.ubi/files/learnData.arff");
            Log.v("test","ファイルを初期化したい");

            if (file.exists()){
                file.delete();
                Log.v("test","ファイルの初期化");
            }
            FileOutputStream learnoutputStream;
            learnoutputStream = openFileOutput("learnData.arff", MODE_APPEND);
            learnoutputStream.write(filetop.getBytes("UTF-8"));

            for(String csvFileName : stateList){
                FileInputStream inputStream = openFileInput(csvFileName+".csv");
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferReader = new BufferedReader(inputStreamReader);

                String line = "";
                while ((line = bufferReader.readLine()) != null) {
                    //カンマ区切りで１つづつ配列に入れる
                    String[] rowData = line.split(",");
                    //合成加速度
                    String data = rowData[3] + ", " + rowData[4];
                    learnoutputStream.write(data.getBytes("UTF-8"));
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
                + "XYZ    : " + acceleration + "\n"
                + "sample :" + sampleNum + "\n");

        if(isLearned) {
            judgment();
        }

        progressBar.setProgress(sampleNum);

        h.postDelayed(this, 150);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors =
                sm.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (0 < sensors.size()) {
            sm.registerListener(this, sensors.get(0),
                    SensorManager.SENSOR_DELAY_FASTEST);
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

            gx = event.values[0];
            gy = event.values[1];
            gz = event.values[2];
            acceleration = (float)Math.sqrt(gx*gx+gy*gy+gz*gz);
            recordSensor();
    }

    public void recordSensor(){
        if(isRecording) {
            String output = BigDecimal.valueOf(gx).toPlainString() + ","
                    + BigDecimal.valueOf(gy).toPlainString() + ","
                    + BigDecimal.valueOf(gz).toPlainString() + ","
                    + BigDecimal.valueOf(acceleration).toPlainString() + ","
                    + " "+state +"\n";
            FileOutputStream outputStream;
            try {
                outputStream = openFileOutput(filename, MODE_APPEND);
                outputStream.write(output.getBytes("UTF-8"));
                outputStream.close();
                sampleNum++;
                if(sampleNum>=1000){
                    recordStop();
                }
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
