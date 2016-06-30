package com.msp.hyunjihyerim.termproject;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by hyun ji Ra on 2016-06-13.
 */
//GPS 측정 모니터
public class LocationMonitor implements LocationListener {
    final static String  Location_ACTION = "Location_ACTION";
    final static String  Entering_ACTION = "Entering_ACTION";
    final static String Step_ACTION = "Step_ACTION";

    LocationManager manager;
    Double latitude = null,longitude = null; //위도, 경도
    float gpsAccuracy; //정확도

    private Context context;
    PendingIntent pendingIntent;
    Intent i;

    AlertReceiver alertReceiver;

    @Override
    public void onLocationChanged(Location location) {
        // 현재 위치를 알아온다
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        gpsAccuracy = location.getAccuracy(); //정확도 측정
    }

    public class AlertReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals( Location_ACTION )){

                //Entering 값 받아오기
                boolean isEntering = intent.getBooleanExtra(
                        LocationManager.KEY_PROXIMITY_ENTERING, false);
                if(isEntering) { //운동장 범위 이내 일경우
                    i.setAction(Step_ACTION);
                    String isEnterLocation = "운동장";

                    //운동장 위치 및 범위 이내라는 정보 전달
                    i.putExtra("IsEnterLocation",  isEnterLocation);
                    i.putExtra("IsEntering", isEntering);
                    context.sendBroadcast(i);

                }else{ //운동장 범위 밖일 경우
                    //운동장 범위 밖이라는 정보 전달
                    i.putExtra("IsEntering", isEntering);

                    // 받아들인 gps 정확도가 45퍼보다 낮으면 실내있다고 판단
                    if(gpsAccuracy < 45) {
                        //실내라는 데이터 전달
                        i.setAction(Step_ACTION);
                        String IsEnter = "실내";

                        i.putExtra("IsEnter",  IsEnter);
                        context.sendBroadcast(i);
                    }
                    else{ //반대일 경우 실외라고 판단
                        //실외라는 데이터 전달
                        i.setAction(Step_ACTION);
                        String IsEnter = "실외";

                        i.putExtra("IsEnter",  IsEnter);
                        context.sendBroadcast(i);
                    }

                }

            }

        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onProviderDisabled(String provider) {}

    //업데이트 요청
    public LocationMonitor(Context context) {
        this.context = context;

        i = new Intent();

        i.setAction(Entering_ACTION);

        manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void onStart() {

        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        alertReceiver = new AlertReceiver();
        IntentFilter filter = new IntentFilter(Location_ACTION);
        context.registerReceiver(  alertReceiver, filter);
        // ProximityAlert 등록을 위한 PendingIntent 객체 얻기
        Intent intent = new Intent(Location_ACTION);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        try {
            //현재위치의 위도와 경도로 설정
            //운동장 위치 등록
            manager.addProximityAlert(36.762581, 127.284527, 80, 5, pendingIntent); //업데이트 간격 5m
        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    public void onStop(){
        // 자원 사용 해제
        try {//경보 설정이 되었다면 아래의 코드 수행하여 해제
            manager.removeUpdates(this); // 등록한 리스너를 해제한다
            manager.removeProximityAlert(pendingIntent);
            context.unregisterReceiver(alertReceiver);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

}

