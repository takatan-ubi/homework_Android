package com.example.ubi;

import android.Manifest;
import android.os.Bundle;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.List;

public class MainActivity extends Activity implements Runnable, SensorEventListener {
    SensorManager sm;
    TextView tv;
    Button recordStartButton,recordStopButton;
    Handler h;
    double gx, gy, gz, a;
    boolean isRecording = false;
    boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout ll = new LinearLayout(this);
        setContentView(R.layout.activity_main);

        h = new Handler();
        h.postDelayed(this, 500);

        // 加速度データの表示テキストウィンド
        tv = (TextView)findViewById(R.id.SenserDataText) ;

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
        gx = event.values[0];
        gy = event.values[1];
        gz = event.values[2];
        a = (float)Math.sqrt(gx*gx+gy*gy+gz*gz);

        if(isRecording) {
            String filename = "kasokusenser.csv";
            String output = BigDecimal.valueOf(gx).toPlainString() + ","
                    + BigDecimal.valueOf(gy).toPlainString() + ","
                    + BigDecimal.valueOf(gz).toPlainString() + ","
                    + BigDecimal.valueOf(a).toPlainString() + "\n";
            FileOutputStream outputStream;
            try {
                outputStream = openFileOutput(filename, MODE_APPEND);
                outputStream.write(output.getBytes());
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
