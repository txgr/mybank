package com.android.alipayhook;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 *  

* @ClassName: DaemonService

* @Description: TODO(这里用一句话描述这个类的作用)

* @author SuXiaoliang

* @date 2018年6月23日 下午1:26:14

*
 */
public class DaemonService extends Service {
	public static String NOTIFY_ACTION = "com.tools.payhelper.notify";
    private static final String TAG = "DaemonService";
    String id="";
    int time=60000;
    public static final int NOTICE_ID = 100;
    public static boolean withdrawing=false;
    RestartReserve restartReserve =new RestartReserve();
    public static final String ALIPAY_ACTION="com.android.alipayhook.getbill";
    public static final String ALIPAY_ACTION_Bill="com.android.alipayhook.bill";
    public Handler handler =new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what==1){
                Object[] param =( Object[]) msg.obj;
                upload(param[0].toString(), (Integer) param[1]);
            }
        }
    };

   /**
         * 查询任务
         * @param id
         */
        private void getTask(String id) {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction( ALIPAY_ACTION);
            sendBroadcast(broadCastIntent);

        }




    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;  
    }

    public static final String CHANNEL_ID_STRING = "002233";
    @Override
    public void onCreate() {  
        super.onCreate();  
        //如果API大于18，需要弹出一个可见通知
        //适配8.0service
        NotificationManager notificationManager = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel(CHANNEL_ID_STRING, "网商银行Hook正在运行中...", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
            Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID_STRING).build();
            startForeground(1, notification);
        }else {
            startForeground(NOTICE_ID,new Notification());
        }

        /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setContentTitle("网商银行");
            builder.setContentText("网商银行Hook正在运行中...");
            builder.setAutoCancel(false);
            builder.setOngoing(true);
            startForeground(NOTICE_ID,builder.build());
        }else{  
            startForeground(NOTICE_ID,new Notification());
        } */
        IntentFilter filter =new IntentFilter();

        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction("com.android.alipayhook.bill");
        filter.addAction("com.payhelper.withdraw");
        registerReceiver(restartReserve,filter);
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        final Intent i = new Intent(NOTIFY_ACTION);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        withdrawTimer = new Timer();
        withdrawTask = new TimerTask() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(id))
                 getTask(id);
            }
        };
        withdrawTimer.schedule(withdrawTask, 10000, time);
    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int i = START_STICKY;
        return super.onStartCommand(intent, i, startId);
        // 如果Service被终止  
        // 当资源允许情况下，重启service  

    }


    private Timer withdrawTimer = new Timer();
    private TimerTask withdrawTask = new TimerTask() {
        @Override
        public void run() {
            if (!TextUtils.isEmpty(id))
                getTask(id);
        }
    };
    public void updateId(){
        SharedPreferences sharedPreferences =getSharedPreferences("config",MODE_PRIVATE);
        id=sharedPreferences.getString("id","");
        time=Integer.valueOf(sharedPreferences.getString("time","60000"));
    }
    @Override
    public void onDestroy() {  
        super.onDestroy();  
        // 如果Service被杀死，干掉通知  
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            NotificationManager mManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            mManager.cancel(NOTICE_ID);  
        }
       unregisterReceiver(restartReserve);
/*        // 重启自己
        Intent intent = new Intent(getApplicationContext(),DaemonService.class);
        startService(intent);*/
        Intent intent = new Intent(getApplicationContext(),DaemonService.class);

//开启服务兼容
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

    }

    public class RestartReserve extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            if (intent!=null){
                boolean re = intent.getBooleanExtra("restart", false);
                updateId();
                if (intent.getAction().equals("com.payhelper.withdraw")){

                    if (re) {
                        if (withdrawTimer!=null)
                        withdrawTimer.cancel();
                          time = Integer.valueOf(intent.getIntExtra("time",time));
                        withdrawTimer = new Timer();
                        withdrawTask = new TimerTask() {
                            @Override
                            public void run() {
                            //    Log.e("Xposed",!isAppRunning(context, "com.eg.android.AlipayGphone")+"----start");
                                if(!isAppRunning(context, "com.eg.android.AlipayGphone")){
                                    Intent intent1 =new Intent(DaemonService.ALIPAY_ACTION_Bill);
                                    intent1.putExtra("data","啓動支付寶");
                                    sendBroadcast(intent1);
                                    startAPP(context, "com.eg.android.AlipayGphone");
                                    return;
                                }
                                if (!TextUtils.isEmpty(id))
                                  getTask(id);
                            }
                        };
                        withdrawTimer.schedule(withdrawTask, 5000, time);
                        Toast.makeText(context, "开启轮询", Toast.LENGTH_SHORT).show();
                    }else {
                        withdrawTimer.cancel();
                        withdrawTimer =null;
                        Toast.makeText(context, "停止轮询", Toast.LENGTH_SHORT).show();
                    }
                }else if (intent.getAction().equals("com.payhelper")){
                }else if (ALIPAY_ACTION_Bill.equals(intent.getAction())){


                    String data =intent.getStringExtra("data");
                    upload(data,0);
                }
            }
        }
    }
    /*
	 * 启动一个app
	 */
    public static void startAPP(Context context, String appPackageName) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(appPackageName);
            context.startActivity(intent);
        } catch (Exception e) {
        }
    }
    /**
     * 方法描述：判断某一应用是否正在运行
     *
     * @param context     上下文
     * @param packageName 应用的包名
     * @return true 表示正在运行，false表示没有运行
     */
    public static boolean isAppRunning(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        if (list.size() <= 0) {
            return false;
        }
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.baseActivity.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }
    private void upload(final String data, final int count) {
      SharedPreferences preferences = getSharedPreferences("config",MODE_PRIVATE);
      String sign=  preferences.getString("id","");
      String url=  preferences.getString("url","");
      String orderId = preferences.getString("orderid","");
      if (!TextUtils.isEmpty(url)){
          RequestParams params=new RequestParams();
          params.addBodyParameter("data", data);
          params.addBodyParameter("sign", sign);
          params.addBodyParameter("orderId", orderId);
          HttpUtils httpUtils=new HttpUtils(15000);
          httpUtils.send(HttpRequest.HttpMethod.POST, url,params, new RequestCallBack<String>() {
              @Override
              public void onSuccess(ResponseInfo<String> responseInfo) {
                  Toast.makeText(DaemonService.this, "回调成功 "+responseInfo.result, Toast.LENGTH_SHORT).show();
              }

              @Override
              public void onFailure(HttpException e, String s) {
                  Log.i(TAG,"上传二维码 "+s+" count ="+count);
                  Log.e(TAG,"错误 "+e.toString());
                  if (count>=3){

                  }else {
                      Message message =new Message();
                      message.what=1;
                      message.obj=new Object[] {data,count+1};
                      handler.sendMessageDelayed(message,1000);
                  }
              }
          });
      }
    }
}  