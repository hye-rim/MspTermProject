package com.msp.hyunjihyerim.termproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.widget.Toast;

/**
 * Created by HYERIM on 2016-06-13.
 */
public class GpsList{
    double latitude;
    double longitude;
    float radius;
    String name;

    public GpsList(String name, double latitude, double longitude, float radius) {
        this.name = name;
        this.latitude = latitude; //위도
        this.longitude = longitude; //경도
        this.radius = radius;
    }
    public GpsList(){
        super();
    }
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
