package com.msp.hyunjihyerim.termproject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by hyun ji Ra on 2016-06-12.
 */

//알람서비스 - 메인서비스
public class AlarmService extends Service {
    private static final String TAG = "AlertService";
    final static String Entering_ACTION = "Entering_ACTION";
    final static String Alarm_ACTION = "Alarm_ACTION";
    final static String Data_ACTION = "Data_ACTION";

    AlarmManager am;
    PendingIntent pendingIntent;
    private CountDownTimer timer;
    StepMonitor accelMonitor;
    LocationMonitor locationMonitor;
    int sleepingTime, beforeTime;

    List<ScanResult> scanList; //와이파이 스캔한 리스트 저장할 변수

    WifiManager wifiManager;
    ArrayList<WifiList> wifiList = new ArrayList<WifiList>(); //등록된 와이파이 리스트

    //Top place 위치와 시간
    String topPlace = " ";
    int topTime = 0;

    long start, end; // wifi 스캔시 duringtime 시간 측정을 위한 변수
    int time;
    int isIn = -1;
    int outCheck = 0;
    String resultString;
    Intent i;

    int duringTime; // 정지 또는 이동 시간
    int movingTime = 0;
    boolean isMoving; // 이동 중인가 정지 했는가
    long now; // 최근 시간
    long prior; // 측정시작시간
    long now2; // 최근 시간
    long prior2; // 측정시작시간
    String priorString, nowString; // 측정 시각을 string값으로 저장
    String priorString2, nowString2;
    boolean final_isMoved;
    String sectionTime; // 측정 구간 시각 표시

    String resultData;
    String isEnter;
    boolean isResistLocation, iswifi;
    String nowLocation;

    Boolean isEntering; // 반경 안에 들어 갔는가
    String gpsLocation;
    int isInGps = -1;

    double final_steps, cur_steps,betweenSteps, totalSteps;

    // Alarm 시간이 되었을 때 안드로이드 시스템이 전송해주는 broadcast를 받을 receiver 정의
    // 그리고 다시 동일 시간 후 alarm이 발생하도록 설정한다.

    //스텝리시버
    StepReceiver stepReceiver;

    public class StepReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            boolean change = false; //정지한 장소의 값이 바뀐지 아닌지 측정
            cur_steps = arg1.getIntExtra("steps", 0); //현재 스텝 수 받기

            isEnter = arg1.getStringExtra("IsEnter"); //실내-실외여부 받기
            if(isEnter == "") isEnter = " "; //null일 때 초기화

            Log.e(TAG, "cursteps : " + cur_steps);
            if (cur_steps >= 5) { //현재 스텝수 5이상일 경우
                if(isMoving == true){ //움직이는 중이면
                    isMoving = true; //계속 이동중이였다!
                    final_steps += cur_steps; //최종스텝에 현재 스텝 더하기
                    //betweenSteps =  final_steps;
                }
                else {
                    isMoving = true; // 그전까지만에도 정지했다가 움직인거다!
                    now = System.currentTimeMillis();
                    Date date = new Date(now);

                    SimpleDateFormat CurTimeFormat = new SimpleDateFormat("HH:mm");
                    nowString = CurTimeFormat.format(date);

                    sectionTime = priorString +"~" +nowString;
                    duringTime=  (int) ((now-prior)/(1000.0*60));

                    if(duringTime >= 5) { // 5분 이상 움직여야 텍스트뷰에 출력됨
                        final_steps += cur_steps; // 스텝수 저장
                        betweenSteps = final_steps; // 구간 스텝수 저장
                        totalSteps += betweenSteps;

                        // 데이터를 메인에 보낸다
                        change = true;
                        i.putExtra("insideChange", change);
                        resultData = sectionTime + "    " + duringTime + "분    " + "정지  " + isEnter;
                        i.putExtra("resultData", resultData);
                        i.putExtra("steps", (int) totalSteps);
                    }

                    // duringtime을 측정하기 위해 prior 시간 세팅
                    prior = System.currentTimeMillis();

                    Date date2 = new Date(prior);

                    SimpleDateFormat CurTimeFormat2 = new SimpleDateFormat("HH:mm");
                    priorString = CurTimeFormat2.format(date2);

                    if(duringTime >= 5) { // 움직인 시간이 5분 넘어가면 파일에 쓴다.
                        Log.e(TAG, "WriteToFile");
                        WriteToFile("\n" + resultData);
                        sendBroadcast(i);
                    }

                }
            }
            else {
                if(isMoving == false) { // 계속 정지해있는다
                    isMoving = false;

                }
                else{
                    isMoving = false; // 이동중이였다가 정지했다!

                    //이동에서 정지로 바꼈으니 그 동안의 시간 측정
                    now = System.currentTimeMillis();
                    Date date = new Date(now);

                    SimpleDateFormat CurTimeFormat = new SimpleDateFormat("HH:mm");
                    nowString = CurTimeFormat.format(date);
                    final_isMoved = true; // 이동
                    sectionTime = priorString +"~" +nowString;
                    duringTime=  (int) ((now-prior)/(1000.0*60));
                    movingTime += duringTime; // movingTime 측정

                    //정지 시간이 1분 이상일 경우 정지 표시,메인에 데이터를 보낸다.
                    if(duringTime >= 1) {
                        change = false;
                        i.putExtra("movingTime", movingTime);
                        i.putExtra("insideChange", change);
                        resultData = sectionTime + "    " + duringTime + "분    " + "이동" + "    " + (int) betweenSteps + "걸음" + isEnter;
                        i.putExtra("resultData", resultData);
                        i.putExtra("steps", (int) totalSteps);
                    }
                    prior = System.currentTimeMillis();

                    Date date2 = new Date(prior);

                    SimpleDateFormat CurTimeFormat2 = new SimpleDateFormat("HH:mm");
                    priorString = CurTimeFormat2.format(date2);
                    if(duringTime >= 1) { // 정지 시간이 1분 이상 일경우 파일에 쓴다.
                        Log.e(TAG, "WriteToFile");
                        WriteToFile("\n" + resultData);
                        sendBroadcast(i);
                    }
                }

            }


        }
    }

    private BroadcastReceiver WifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                checkProximity(); // 와이파이 스캔 시작
            }
        }
    };

    private BroadcastReceiver AlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {

            if (intent.getAction().equals(Entering_ACTION)) {
                //범위내에 들어와 있는지 확인. 위치 저장
                isEntering = intent.getBooleanExtra("IsEntering", false);
                gpsLocation = intent.getStringExtra("IsEnterLocation");

                //시간 측정
                checkGpsLocation();
                now2 = System.currentTimeMillis();
                Date date2 = new Date(now2);
                SimpleDateFormat CurTimeFormat2 = new SimpleDateFormat("HH:mm");
                nowString2 = CurTimeFormat2.format(now2);
                sectionTime = priorString2 +"~" +nowString2;
                duringTime=  (int) ((now2-prior2)/(1000.0*60));

                isEnter = intent.getStringExtra("isEnter");

            }
            if (intent.getAction().equals(Alarm_ACTION)) {

                accelMonitor = new StepMonitor(context);
                locationMonitor = new LocationMonitor(context);

                Log.e(TAG, "accelMonitor.onStart");
                //Toast.makeText(context, "start", Toast.LENGTH_SHORT).show();
                accelMonitor.onStart();

                wifiManager.startScan();
                iswifi = true;

                //걷고있는 중이라면 location모니터링 시작
                if (isMoving && isResistLocation == false) {
                    locationMonitor.onStart();
                    isResistLocation = true;
                }
                long time = 10000; //10초 동안 스텝감지
                timer = new CountDownTimer(time, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {

                    }

                    @Override
                    public void onFinish() {
                        Log.e(TAG, "accelMonitor.onStop");
                        // Toast.makeText(context, "stop", Toast.LENGTH_SHORT).show();
                        //시간이 다 되면 step모니터 종료( 가속도 센서 리스너 등록 해제)
                        accelMonitor.onStop();


                        //로케이션 모니터가 실행중이면
                        if (isResistLocation) {
                            // 로케이션 모니터 종료
                            locationMonitor.onStop();
                            isResistLocation = false; // 실행중이지 않다라는 의미로 false 대입
                        }
                        //자원 반납을 위해 null값으로 초기화 한다.
                        accelMonitor = null;
                        locationMonitor = null;
                        // 다음 alarm 등록
                        Intent in = new Intent(Alarm_ACTION);

                        // sleepingtime 계산 (스텝수에 따라 달라진다)
                        getSleepingTime(cur_steps);
                        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, in, 0);
                        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                SystemClock.elapsedRealtime() + sleepingTime, pendingIntent);
                    }
                };
                timer.start();


                // 타이머(10초)동안 세었던 걸음 수가 5 이상일 경우 걸었다고 판단하여 메인으로 값을 보내고 메인에서 출력한다.

            }
        }
    };

    //Wifi Check
    private void checkProximity() {
        scanList = wifiManager.getScanResults(); //와이파이 스캔 리스트 저장
        nowLocation = wifiList.get(20).getName(); //위치 저장
        boolean isProximate = false; //와이파이 일치하는지 여부

        int cnt = 0; //와이파이 일치횟수
        for(int i = 1; i < scanList.size(); i++) { //와이파이 스캔 리스트 크기만큼 for문
            ScanResult result = scanList.get(i); //값 하나 받아오기
            for(int j = 0; j < wifiList.size(); j++){ //저장된 wifiList와 비교
                //스캔된 와이파이 값과 저장된 wifiList값의 bssid가 일치하고 rssi값이 wifilist의 rssi-10보다 클 경우 일치한다고 판단
                if( (result.BSSID.equals(wifiList.get(j).getBssid())) && (result.level > (wifiList.get(j).getRssi() - 10)) ) {
                    isProximate = true; //장소라고 판단
                    cnt++; //일치 횟수 증가
                    nowLocation = wifiList.get(j).getName(); //일치하므로 이름 저장
                }else {
                    isProximate = false; //장소가 아니라고 판단
                }
            }
            isProximate = (cnt >=  4) ? true : false; //스캔 와이파이 리스트와 wififlit의 일치횟수가 4회 이상이라면 그 장소에 있다고 판단
        }
        //isin 정보 : -1 => 시작한적 없음, 1 => 시작, 0 => 나감
        //outCheck를 통해 해당 장소 와이파이 신호 불량으로 1번 정도는 잘못 측정되더라도 넘어갈 수 있도록 함
        //와이파이 장소에 있을 경우
        if(isProximate) {
            if(isIn == -1){ //시작하지 않았으므로 시작
                start = System.currentTimeMillis(); //처음 시간 기록
                isIn = 1;
                outCheck = 0; //나간 횟수 0으로 초기화
            }
            Toast.makeText(this, "접근중 : " + nowLocation, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "접근중 : " + nowLocation );
        } else { //장소에 없는 경우
            if(outCheck > 0 &&isIn == 1){ //한번 나가게 되고 시작된 상태라면 종료
                end = System.currentTimeMillis(); //끝나는 시간 기록
                isIn = 0; //나감상태로 전환
            }
            outCheck++; //나간 횟수 증가
            Log.d(TAG, "접근아님 : " + nowLocation);
        }

        if(isIn == 0) { //나간 경우 계산
            time = (int) ((end-start)/(1000.0*60)); //시간 계산
            if(time > 0) { //1분 이상일 경우 기록
                resultString = nowLocation; //현재위치 등록
                Toast.makeText(this, resultString + "  시간 : " + time, Toast.LENGTH_SHORT).show();
                if(time >= topTime) //최고 머무른 시간보다 time이 클 경우 현재 위치를 Top place로 등록
                    topPlace = nowLocation;
                //초기화
                start = 0;
                end = 0;
                isIn = -1;
                outCheck = 0;

                if(time >= 5) {//5분이상 머무른 경우만 데이터 전송 및 파일 기록
                    i.putExtra("inside", resultString);
                    i.putExtra("topPlace", topPlace);

                    sendBroadcast(i);
                    WriteToFile("  " + resultString);
                }
            }
        }
    }

    //운동장인지 아닌지 맞는지 구분해주는 함수
    private void checkGpsLocation(){
        if(isEntering){ //들어와있을 경우
            if(isInGps == -1){ //처음 시작하는 경우
                start = System.currentTimeMillis(); //시작 값 저장
                isInGps = 1; //들어와있다는 값으로 바꾼다
            }
            Toast.makeText(this, gpsLocation + " 범위", Toast.LENGTH_SHORT).show();
        }else{
            if(isInGps == 1){//시작되있는 경우
                end = System.currentTimeMillis(); //종료 값 저장
                isInGps = 0; //나갔다는 값으로 바꾼다
            }
        }

        if(isInGps == 0){ //처음 시작이 아닌 시작했다가 종료했을 경우 실행
            time = (int) ((end-start)/(1000.0*60)); //시간 저장
            if(time > 0) {//1분부터 저장
                if(time >= topTime) // top place 교체
                    topPlace = gpsLocation;

                //초기화
                start = 0;
                end = 0;
                isIn = -1; //시작한적 없던 상태로 리셋

                if(time >= 5) {//5분 이상일 경우 데이터 보내기 및 파일 쓰기
                    i.putExtra("inside", gpsLocation);
                    i.putExtra("topPlace", topPlace);

                    sendBroadcast(i);
                    WriteToFile("  " + resultString);
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        stepReceiver = new StepReceiver();

        // 현재 시간을 msec으로 구한다.duringtime 측정을 위해 prior 세팅
        prior = System.currentTimeMillis();

        Date date = new Date(prior);


        SimpleDateFormat CurTimeFormat = new SimpleDateFormat("HH:mm");
        priorString = CurTimeFormat.format(date);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        init(); //와이파이 리스트 저장

        //초기화
        gpsLocation = " ";
        isEnter = "";
        isEntering = false;
        isMoving = false;
        isResistLocation = false;
        iswifi = false;
        resultData = "A312";
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(WifiReceiver, filter);

        // Alarm 발생 시 전송되는 broadcast를 수신할 receiver 등록
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Alarm_ACTION);
        registerReceiver(AlarmReceiver, intentFilter);

        //스텝 발생시 전송되는 브로드캐스트를 수신할 리시버 등록
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(StepMonitor.Step_ACTION);
        intentFilter2.addAction(LocationMonitor.Step_ACTION);
        registerReceiver(stepReceiver, intentFilter2);


        final_steps = 0; // 총 스텝 수 초기화
        sleepingTime = 1000; // 처음 알람 시간( 물론 자동적으로 운영체제가 5초로 바꾸기는 함)

        i = new Intent();

        i.setAction(Data_ACTION); // 데이터를 메인으로 보낸다는 의미

        // AlarmManager 객체 얻기
        am = (AlarmManager) getSystemService(ALARM_SERVICE);
    }

    public void init() {
        // 다산과 강의실에서 스캔되는 와이파이 목록 추가

        //다산 와이파이 정보
        wifiList.add(new WifiList("다산로비", "KUTAP","a4:18:75:58:77:df",-65));
        wifiList.add(new WifiList("다산로비", "KUTAP", "20:3a:07:49:5c:ef", -65));
        wifiList.add(new WifiList("다산로비", "KUTAP_N", "20:3a:07:49:5c:ee", -66));
        wifiList.add(new WifiList("다산로비", "KUTAP_N", "a4:18:75:58:77:de", -65));
        wifiList.add(new WifiList("다산로비", "KUTAP_N", "20:3a:07:49:5c:e1", -65));
        wifiList.add(new WifiList("다산로비", "KUTAP_N", "a4:18:75:58:77:d1", -65));
        wifiList.add(new WifiList("다산로비", "KUTAP_N", "88:75:56:c7:1f:11", -66));
        wifiList.add(new WifiList("다산로비", "KUTAP_N", "20:3a:07:9e:a6:c1", -65));
        wifiList.add(new WifiList("다산로비", "KUTAP", "88:75:56:c7:1f:10", -65));
        wifiList.add(new WifiList("다산로비", "KUTAP", "a4:18:75:58:77:d0", -63));
        wifiList.add(new WifiList("다산로비", "KUTAP", "20:3a:07:9e:a6:c0", -66));
        wifiList.add(new WifiList("다산로비", "KUTAP", "20:3a:07:49:5c:e0", -64));
        wifiList.add(new WifiList("다산로비", "KUTAP", "18:33:9d:c6:6a:f0", -64));

        //A312 와이파이 정보
        wifiList.add(new WifiList("A312", "ap1-voice", "00:1d:e5:8d:30:a1", -68));
        wifiList.add(new WifiList("A312", "KUTAP", "50:1c:bf:5b:2c:c0", -65));
        wifiList.add(new WifiList("A312", "NSTL 5GHz", "00:26:66:cc:e3:88", -70));
        wifiList.add(new WifiList("A312", "KUTAP_N", "50:1c:bf:41:cf:21", -49));
        wifiList.add(new WifiList("A312", "NSTL 2.4GHz", "00:26:66:cc:e3:8c", -65));
        wifiList.add(new WifiList("A312", "KUTAP", "50:1c:bf:41:cf:20", -46));
        wifiList.add(new WifiList("A312", "KUTAP_N", "50:1c:bf:5b:2c:c1", -66));
        wifiList.add(new WifiList("A312", "KUTAP", "50:1c:bf:5f:7c:e0", -62));
        wifiList.add(new WifiList("A312", "NETGEAR61", "e4:f4:c6:1c:7b:6f", -69));
        wifiList.add(new WifiList("A312", "NETGEAR61-5G", "e4:f4:c6:1c:7b:6e", -68));
        wifiList.add(new WifiList("A312", "ap1-data", "00:1d:e5:8d:30:a0", -65));
        wifiList.add(new WifiList("A312", "KUTAP_N", "50:1c:bf:5f:7c:e1", -60));
        wifiList.add(new WifiList("A312", "A313-3", "64:e5:99:51:18:60", -61));

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id
        // Alarm이 발생할 시간이 되었을 때, 안드로이드 시스템에 전송을 요청할 broadcast를 지정
        Intent in = new Intent(Alarm_ACTION);
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, in, 0);

        // Alarm이 발생할 시간 및 alarm 발생시 이용할 pending intent 설정
        // 설정한 시간 (5000-> 5초, 10000->10초) 후 alarm 발생
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + sleepingTime, pendingIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    //Sleeping time - 걸음수에 따라 계산
    public void getSleepingTime(double cur_steps) {

        if (cur_steps >= 32) { //32 이상
            sleepingTime = 3000;
        } else if (cur_steps >= 16 && cur_steps < 32) { //16 이상 32 미만
            sleepingTime = 5000;
        } else { //16미만
            sleepingTime += 3000;

            if (sleepingTime >= 30000) {
                sleepingTime = 30000;
            }
        }

    }

    public void WriteToFile(String msg) {
        //외부메모리에 파일 생성
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "Steprecord.txt");
        Log.e(" WriteToFile", Environment.getExternalStorageDirectory().getAbsolutePath());

        try {
            //파일이 기존에 있다면 그 파일에 그대로 값을 append를 한다(덮어쓰는게 아님)
            FileOutputStream fos = new FileOutputStream(file, true);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fos));


            bufferedWriter.write(msg);
            bufferedWriter.newLine();
            bufferedWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //레지스터 해제;
        unregisterReceiver(AlarmReceiver);
        unregisterReceiver(stepReceiver);
    }
}