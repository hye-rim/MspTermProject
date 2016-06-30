package com.msp.hyunjihyerim.termproject;

/**
 * Created by hyun ji Ra on 2016-05-26.
 */

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

//걸음 수 측정 모니터
public class StepMonitor implements SensorEventListener{
    final static String TAG = "StepMonitor";
    final static String  Step_ACTION = "Step_ACTION";
    private SensorManager mSensorManager;
    LocationManager locManager;
    private Sensor mLinear;
    private long prevT, currT;
    private double[] rmsArray;
    private int rmsCount;
    private double steps; // 걸음 수

    private Context context;
    Intent in;

    // SENSOR_DELAY_NORMAL로 가속도 데이터 수집 시 데이터 업데이트 주기는 약 200ms
    // 3축 가속도 데이터의 RMS 값의 1초간 평균값을 이용하여 걸음이 있었는지 판단하기 위한 기준 문턱값
    private static final double THRESHOLD = 4;

    //엑티비티 시간동안 rms 평균값이 기준 문턱값을 넘었을 때, steps를 1.6 씩 증가
    private static final double NUMBER_OF_STEPS_PER_SEC = 1.6;

    //스텝모니터 생성자
    public StepMonitor(Context context) {
        this.context = context;

        in = new Intent();

        Log.e(TAG, "StepReceiver생성자");

        steps = 0.0; //초기화

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLinear = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void onStart() {
        // SensorEventListener 등록
        if (mLinear != null) {
            Log.e(TAG, "StepMonitor onStart");
            mSensorManager.registerListener(this, mLinear, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void onStop() {
        // SensorEventListener 등록 해제

        Log.e(TAG, "StepMonitor onStop");
        mSensorManager.unregisterListener(this, mLinear);
        mLinear = null;
        in.setAction(Step_ACTION);
        Log.e(" steps", ""+steps);
        // 걸음수는 정수로 표시되는 것이 적합하므로 int로 형변환
        in.putExtra("steps", (int)steps);
        // broadcast 전송
        context.sendBroadcast(in);


    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // 센서 데이터가 업데이트 되면 호출
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            Log.e(TAG, " onSensorChanged");

            //***** sensor data collection *****//
            // event.values 배열의 사본을 만들어서 values 배열에 저장
            float[] values = event.values.clone();

            //스텝계산
            computeSteps(values);
            Log.e(TAG, "  computeSteps");

        }
    }

    //Step계산
    private void computeSteps(float[] values) {

        double rms = Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]); //rms계산

        // 3축 가속도 데이터의 1초 평균 RMS 값이 기준 문턱값을 넘으면 step이 있었다고 판단함
        if(rms > THRESHOLD) {
            // step 수는 1초 걸음 시 step 수가 일정하다고 가정하고, 그 값을 더해 줌
            steps += NUMBER_OF_STEPS_PER_SEC;
        }
    }

}

