package com.msp.hyunjihyerim.termproject;

import java.util.List;

//Wifi 정보가 담긴 리스트 클래스
public class WifiList {
    private String name; //WIfi 이름
    private String ssid; //Wifi SSID
    private String bssid; //Wifi bssid (MAC)
    private int rssi; //Wifi 최대 rssi

    //wifi 생성자
    public WifiList(String name, String ssid, String bssid, int rssi) {
        this.name = name;
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
    }

    //wifi 생성자
    public WifiList() {
        super();
    }

    //각 변수에 대한 getther, setter
    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
