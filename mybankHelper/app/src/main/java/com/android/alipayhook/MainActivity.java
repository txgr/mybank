package com.android.alipayhook;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText id, time, url, orderID;
    Button start, stop, create_qrcode, save;
    TextView label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        id = findViewById(R.id.id);
        url = findViewById(R.id.url);
        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);
        time = findViewById(R.id.time);
        save = findViewById(R.id.save);
        orderID = findViewById(R.id.orderID);
        create_qrcode = findViewById(R.id.create_qrcode);
        label = findViewById(R.id.label);
        SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
        String ids = sharedPreferences.getString("id", "");
        String times = sharedPreferences.getString("time", "60000");
        String orderid = sharedPreferences.getString("orderid", "1000");
        String mUrl = sharedPreferences.getString("url", "");
        url.setText(mUrl);
        id.setText(ids);
        time.setText(times);
        orderID.setText(orderid);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mId = id.getText().toString().trim();
                String mUrl = url.getText().toString().trim();
                String mTime = time.getText().toString().trim();
                String mOrderID = orderID.getText().toString().trim();
                if (TextUtils.isEmpty(mId) || TextUtils.isEmpty(mUrl) || TextUtils.isEmpty(mTime) || TextUtils.isEmpty(mOrderID)) {
                    Toast.makeText(MainActivity.this, "参数不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("id", mId);
                editor.putString("orderid", mOrderID);
                editor.putString("url", mUrl);
                editor.putString("time", mTime);

                editor.commit();
                Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
            }
        });


        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.payhelper.withdraw");
                intent.putExtra("restart", true);
                intent.putExtra("time", Integer.valueOf(time.getText().toString()));
                sendBroadcast(intent);
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.payhelper.withdraw");
                intent.putExtra("restart", false);
                sendBroadcast(intent);
            }
        });
        startService(new Intent(this, DaemonService.class));
        create_qrcode.setText("获取账单测试");
        create_qrcode.setVisibility(View.VISIBLE);
        create_qrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent broadCastIntent = new Intent();
                broadCastIntent.setAction(DaemonService.ALIPAY_ACTION);

                sendBroadcast(broadCastIntent);
            }
        });
        IntentFilter filter = new IntentFilter();
        filter.addAction(DaemonService.ALIPAY_ACTION_Bill);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction().contentEquals(DaemonService.ALIPAY_ACTION_Bill)) {
                    String msg = intent.getStringExtra("data");
                    label.setText(label.getText() + "\r\n" + msg);
                    Log.i("qrcodereceived", msg + "");
                }
            }
        }, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }



}
