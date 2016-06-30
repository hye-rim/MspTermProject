package com.msp.hyunjihyerim.termproject;

import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

//메인액티비티
public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    TextView t_record; // 기록한 파일을 읽어와 데이터를 출력하는 뷰
    TextView t_step,t_date; // 기록한 파일을 읽어와 데이터를 출력하는 뷰
    Button b_start,b_stop; //기록,모니터링 시작,정지 버튼
    String message,temp;

    boolean isStart; //시작 버튼

    String name;
    String isEnter; //실내, 실외
    String insideResult = ""; //머문 장소
    boolean insideChange = false; //머무른장소가 바뀌었는지 아닌지 - 바뀐경우만 insideResult 출력
    DataReceiver dataReceiver;

    //총 걸음수, 총 이동시간, 오래 머무른장소
    int steps; //총 걸음 수
    String topPlace;//오래 머무른장소
    int movingTime;//총 이동시간

    WifiManager wifiManager;

    //데이터 받는 broadcastReceiver
    public class DataReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            //정보 출력되는 뷰의 내용
            steps= arg1.getIntExtra("steps", 0); //총 걸음수
            topPlace = arg1.getStringExtra("topPlace"); //오래 머무른 장소
            movingTime = arg1.getIntExtra("movingTime", 0); //총 이동 시간
            isEnter = arg1.getStringExtra("IsEnter"); //실내-실외

            String str = arg1.getStringExtra("resultData"); //시간, 걸린시간, 이동-정지여부, 걸음수

            insideChange = arg1.getBooleanExtra("insideChange", false); //정지한 장소가 업데이트된지를 알아보기 위함

            Log.e(TAG, "DataReceiver");

            //정보 출력 뷰에 출력
            t_step.setText("Moving Time : " + movingTime + "\nSteps : " + steps + "\nTop Place : " + topPlace + "  " + isEnter );

            //정지한 장소가 업데이트 된 경우
            if(insideChange) {
                insideResult = arg1.getStringExtra("inside"); //장소를 받아온다
                t_record.append("\n" + str +"  "+  insideResult); //시간, 걸린시간, 이동-정지여부, 걸음수, 장소 덧붙여 출력

            }
            else
                t_record.append("\n" + str); //시간, 걸린시간, 이동-정지여부, 걸음수 출력
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");
        t_record = (TextView) findViewById(R.id.t_record);
        t_step = (TextView) findViewById(R.id.t_step);
        t_date = (TextView) findViewById(R.id.t_date);
        b_start = (Button) findViewById(R.id.b_start);
        b_stop = (Button) findViewById(R.id.b_stop);

        //초기화
        topPlace = "없음";
        movingTime = 0;

        //현재시간정보
        long now = System.currentTimeMillis();//현재시간
        Date date = new Date(now);
        SimpleDateFormat CurDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일"); // 시간 포맷 지정
        String strCurDate = CurDateFormat.format(date);
        t_date.setText(strCurDate); //현재시간출력

        //선언
        dataReceiver = new  DataReceiver();

        wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        if(wifiManager.isWifiEnabled() == false)
            wifiManager.setWifiEnabled(true);
        Log.e(TAG, "startWifi");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");

        unregisterReceiver(dataReceiver); //리시버 해제
    }

    @Override

    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        //서비스클래스에서 보낸 메세지인지 구별하기 위해 어떤 종류의 액션을받을지 설정.
        intentFilter.addAction(AlarmService.Data_ACTION);
        intentFilter.addAction(LocationMonitor.Entering_ACTION);
        registerReceiver(dataReceiver, intentFilter);

        //텍스트 읽기
        String str;
        if((str = ReadTextFile()) != null) { //null이 아닐경우만 읽어오기
            Log.i(TAG, " onResume");
            t_record.setText(ReadTextFile());
        }

    }

    // 텍스트파일에서 기록을 읽어와 t_record 뷰에 출력한다.
    public String ReadTextFile() {
        String resulteContents = "";
        try{
            FileInputStream fos = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Steprecord.txt");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fos));
            temp="";
            while((temp = bufferedReader.readLine())!= null) {
                resulteContents += ("\n"+temp);

            }
        }catch(Exception e){
            return null;
        }
        return resulteContents;
    }

    //클릭이벤트
    public void onClick(View view){
        if(view.getId() == R.id.b_start){ //시작버튼
            if(!isStart) { //정지였을 경우만 실행
                Log.e(TAG, "startService");

                IntentFilter intentFilter = new IntentFilter();
                //서비스클래스에서 보낸 메세지인지 구별하기 위해 어떤 종류의 액션을받을지 설정.
                intentFilter.addAction(AlarmService.Data_ACTION);
                registerReceiver(dataReceiver, intentFilter);

                Toast.makeText(this, "스텝모니터링 시작", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, AlarmService.class);
                startService(intent);

                isStart = true;
            }
        }
        else if(view.getId() == R.id.b_stop){ //중지 버튼
            Intent intent = new Intent( this,AlarmService.class);
            if(isStart) { //시작상태일 경우만 실행
                Log.e(TAG, "stopService");
                Toast.makeText(this, "스텝모니터링 정지", Toast.LENGTH_SHORT).show();
                stopService(intent);
                isStart = false;
                unregisterReceiver(dataReceiver); //해제
            }
        }
    }




}