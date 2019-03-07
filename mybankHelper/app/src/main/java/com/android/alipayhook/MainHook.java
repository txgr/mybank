package com.android.alipayhook;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

   static Activity  activity;
   static ClassLoader mClassLoader;
   static Context context;
   public AlipayReceived mAlipayReceived;
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        final String packageName = loadPackageParam.packageName;
        final String processName = loadPackageParam.processName;
        if (loadPackageParam.appInfo == null || (loadPackageParam.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM |
                ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
            return;
        }
        if (loadPackageParam.packageName.equals("com.eg.android.AlipayGphone")) {
            securityCheckHook(loadPackageParam.classLoader);
            StringBuilder localStringBuilder = new StringBuilder();
            localStringBuilder.append("开始HooK  ");
            localStringBuilder.append(Build.VERSION.SDK_INT);
            XposedBridge.log(localStringBuilder.toString());
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", new Object[] { Bundle.class, new XC_MethodHook()
            {

                protected void afterHookedMethod(MethodHookParam methodHookParam)
                        throws Throwable {
                    if (methodHookParam.thisObject.getClass().toString().contains("com.eg.android.AlipayGphone.AlipayLogin")){
                        activity = (Activity) methodHookParam.thisObject;
                        context =activity.getApplication();
                        Object mPackageInfo = XposedHelpers.getObjectField(activity.getApplication().getBaseContext(), "mPackageInfo");
                        mClassLoader = (ClassLoader) XposedHelpers.getObjectField(mPackageInfo, "mClassLoader");
                        Toast.makeText(activity, " hook 到支付宝 ", Toast.LENGTH_SHORT).show();
                        IntentFilter filter =new IntentFilter();
                        filter.addAction(DaemonService.ALIPAY_ACTION);
                        filter.addAction(Intent.ACTION_TIME_TICK);
                        mAlipayReceived=new AlipayReceived();
                        activity.registerReceiver(mAlipayReceived,filter);
                    }
                  }
                 }
            });
            XposedHelpers.findAndHookMethod(Activity.class, "onDestroy",  new XC_MethodHook()
            {

                protected void afterHookedMethod(MethodHookParam methodHookParam)
                        throws Throwable {
                    if (methodHookParam.thisObject.getClass().toString().contains("com.eg.android.AlipayGphone.AlipayLogin")){
                       if (mAlipayReceived!=null&&activity!=null){
                           activity.unregisterReceiver(mAlipayReceived);
                       }
                    }
                }

            });
        }

    }

    class AlipayReceived extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            XposedBridge.log("AlipayReceived "+intent.getAction());
            try {
                if (intent.getAction().equals(DaemonService.ALIPAY_ACTION)){
                   String o= new AlipayHook().getOrderList(context);
                    JSONObject gson =new JSONObject(o);
                    JSONObject billListView =gson.optJSONObject("billListView");
                    JSONArray billDetailView =billListView.optJSONArray("billDetailView");
                    Intent intent1 =new Intent(DaemonService.ALIPAY_ACTION_Bill);
                    intent1.putExtra("data",billDetailView.toString());
                   activity.sendBroadcast(intent1);
                }


            } catch (Exception e) {
                XposedBridge.log(e);
            }
        }
    }

    private String getTopActivityInfo(Context context) {
        ActivityManager manager = ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE));
        String packageName = "";
      String topActivityName="";
     List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
        if (runningTaskInfos != null)
            {
             return (runningTaskInfos.get(0).topActivity.getClassName());
             }


        return null;
    }

    private void securityCheckHook(ClassLoader classLoader) {
        try {
            Class<?> securityCheckClazz = XposedHelpers.findClass("com.alipay.mobile.base.security.CI", classLoader);
            XposedHelpers.findAndHookMethod(securityCheckClazz, "a", String.class, String.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object object = param.getResult();
                    XposedHelpers.setBooleanField(object, "a", false);
                    param.setResult(object);
                    super.afterHookedMethod(param);
                }
            });

            XposedHelpers.findAndHookMethod(securityCheckClazz, "a", Class.class, String.class, String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return (byte) 1;
                }
            });
            XposedHelpers.findAndHookMethod(securityCheckClazz, "a", ClassLoader.class, String.class, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return (byte) 1;
                }
            });
            XposedHelpers.findAndHookMethod(securityCheckClazz, "a", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    return false;
                }
            });

        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }


}
