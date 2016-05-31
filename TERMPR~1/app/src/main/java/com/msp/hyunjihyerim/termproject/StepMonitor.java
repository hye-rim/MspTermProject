package com.msp.hyunjihyerim.termproject;

/**
 * Created by hyun ji Ra on 2016-05-26.
 */

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by hyun ji Ra on 2016-05-26.
 */
public class StepMonitor extends Service {
    final static String  Data_ACTION = "Data_ACTION";

    Intent i = new Intent();
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {

                    /* 브로드캐스트를 사용하여 서비스에서 생긴 데이터들을 메인으로 보낸다.
                    메인에서는 MyReceiver로 통해서 이 값을 구분해서 받는다.*/
                    sendBroadcast(i);

            }
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

