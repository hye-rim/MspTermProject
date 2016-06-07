package com.msp.hyunjihyerim.termproject;

/**
 * Created by hyun ji Ra on 2016-05-26.
 */

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

/**
 * Created by hyun ji Ra on 2016-05-26.
 */
public class StepMonitor extends Service implements SensorEventListener{
    final static String  Data_ACTION = "Data_ACTION";
    private SensorManager mSensorManager;
    LocationManager locManager;
    private Sensor mLinear;
    private long prevT, currT;
    private double[] rmsArray;
    private int rmsCount;
    private double steps; // 걸음 수
    double duringtime; //
    double currLatitude, currLongitude; // 현재 위치의 위도와 경도

    LocationListener locationListener = new LocationListener(){

        @Override
        public void onLocationChanged(Location location) {
            // 현재 위치의 위도와 경도 값을 얻어 온다.
            currLatitude =  location.getLatitude();
            currLongitude = location.getLongitude();
        }
        @Override
        public void onProviderDisabled(String provider) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    // SENSOR_DELAY_NORMAL로 가속도 데이터 수집 시 데이터 업데이트 주기는 약 200ms
    // 따라서 초당 데이터 샘플 수는 5개
    // 이 값은 폰마다 다를 수 있으므로 확인 필요
    private static final int NUMBER_OF_SAMPLES = 5;

    // 3축 가속도 데이터의 RMS 값의 1초간 평균값을 이용하여 걸음이 있었는지 판단하기 위한 기준 문턱값
    private static final double THRESHOLD = 2.5;

    //엑티비티 시간동안 rms 평균값이 기준 문턱값을 넘었을 때, steps를 1 씩 증가
    private static final double NUMBER_OF_STEPS_PER_SEC = 1.5;

    Intent i = new Intent();
    Intent dataIntent = new Intent();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            sendBroadcast(dataIntent);

        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        i.setAction(Data_ACTION);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLinear = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        // SensorEventListener 등록
        mSensorManager.registerListener(this,mLinear,SensorManager.SENSOR_DELAY_NORMAL);


        // 현재 위치를 얻어온다다
       try{
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        } catch (SecurityException e) {
                    e.printStackTrace();
        }

    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // 센서 데이터가 업데이트 되면 호출
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            //***** sensor data collection *****//
            // event.values 배열의 사본을 만들어서 values 배열에 저장
            float[] values = event.values.clone();

            // simple step calculation
            computeSteps(values);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void computeSteps(float[] values) {
        double avgRms = 0;

        //***** feature extraction *****//
        // calculate feature data:
        // 여기서는 3축 가속도 데이터의 RMS 값의 1초 간의 평균값을 이용

        // 1. 현재 업데이트 된 accelerometer x, y, z 축 값의 Root Mean Square 값 계산
        double rms = Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);

        // 2. 위에서 계산한 RMS 값을 rms 값을 저장해 놓는 배열에 넣음
        // 배열 크기는 1초에 발생하는 가속도 데이터 개수 (여기서는 5)
        if(rmsCount < NUMBER_OF_SAMPLES) {
            rmsArray[rmsCount] = rms;
            rmsCount++;
        } else if(rmsCount == NUMBER_OF_SAMPLES) {
            // 3. 1초간 rms 값이 모였으면 평균 rms 값을 계산
            double sum = 0;
            // 3-1. rms 값들의 합을 구함
            for(int i = 0; i < NUMBER_OF_SAMPLES; i++) {
                sum += rmsArray[i];
            }
            // 3-2. 평균 rms 계산
            avgRms = sum / NUMBER_OF_SAMPLES;

            // 4. rmsCount, rmsArray 초기화: 다시 1초간 rms sample을 모으기 위해
            rmsCount = 0;
            for(int i = 0; i < NUMBER_OF_SAMPLES; i++) {
                rmsArray[i] = 0;
            }

            // 5. 이번 업데이트로 계산된 rms를 배열 첫번째 원소로 저장하고 카운트 1증가
            rmsArray[0] = rms;
            rmsCount++;
        }

        //***** classification *****//
        // check if there is a step or not:
        // 1. 3축 가속도 데이터의 1초 평균 RMS 값이 기준 문턱값을 넘으면 step이 있었다고 판단함
        if(avgRms > 3) {
            // 1-1. step 수는 1초 걸음 시 step 수가 일정하다고 가정하고, 그 값을 더해 줌
            steps += NUMBER_OF_STEPS_PER_SEC;
            Log.d("LOGTAG", "steps: " + steps);

            // if step counts increase, send steps data to MainActivity
            Intent intent = new Intent("kr.ac.koreatech.msp.stepmonitor");
            // 걸음수는 정수로 표시되는 것이 적합하므로 int로 형변환
            intent.putExtra("steps", (int)steps);
            // broadcast 전송
            sendBroadcast(intent);
        }
    }

    //발생한 데이터는 파일에 바로 입력한다.
    public void writeToFile(String msg){

        //외부메모리에 파일 생성
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "StepRecord.txt");

        try{
            //파일이 기존에 있다면 그 파일에 그대로 값을 append를 한다(덮어쓰는게 아님)
            FileOutputStream fos = new FileOutputStream(file,true);

            fos.write(msg.getBytes());
            fos.close();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

