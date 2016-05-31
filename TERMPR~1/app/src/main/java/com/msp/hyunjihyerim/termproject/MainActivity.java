package com.msp.hyunjihyerim.termproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.os.Environment;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class MainActivity extends AppCompatActivity {
    TextView t_record; // 기록한 파일을 읽어와 데이터를 출력하는 뷰
    int index = 0;
    String message;

    DataReceiver dataReceiver;

    public class DataReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub
            long duringtime = arg1.getLongExtra("DURING", 0);
            String datapassed = arg1.getStringExtra("DATAPASSED");
            index++;
            message = index + ". " + datapassed + "   (" + duringtime / 60000 + "분)" + "\n";
            t_record.append(message);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        t_record = (TextView) findViewById(R.id.t_record);

        dataReceiver = new  DataReceiver();
        IntentFilter intentFilter = new IntentFilter();
        //서비스클래스에서 보낸 메세지인지 구별하기 위해 어떤 종류의 액션을받을지 설정.
        intentFilter.addAction(StepMonitor.Data_ACTION);
        registerReceiver(dataReceiver, intentFilter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        t_record.setText(ReadTextFile());
    }

    // 텍스트파일에서 기록을 읽어와 t_record 뷰에 출력한다.
    public String ReadTextFile() {
        String text = null;
        try {
            File file = getFileStreamPath("StepRecord.txt");
            FileInputStream fis = new FileInputStream(file);
            Reader in = new InputStreamReader(fis);
            int size = fis.available();
            char[] buffer = new char[size];
            in.read(buffer);
            in.close();

            text = new String(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return text;
    }
}
