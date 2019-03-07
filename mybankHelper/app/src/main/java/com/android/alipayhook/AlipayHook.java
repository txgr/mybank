package com.android.alipayhook;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.http.message.BasicHeader;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class AlipayHook {


    private static final String TAG =" AlipayHook ";

    public String getOrderList(Context paramContext)
    {
        XposedBridge.log(TAG+"orderList +"+paramContext);
        SimpleDateFormat simpleDateFormat =new SimpleDateFormat("yyyyMMdd");
        String endDate = simpleDateFormat.format(new Date(System.currentTimeMillis()));
        String  startDate= simpleDateFormat.format(new Date(System.currentTimeMillis()-2*24*60*60*1000));
        JSONArray array =new JSONArray();
        JSONObject params =new JSONObject();
        params.put("endDate",endDate);
        params.put("startDate",startDate);
        params.put("maxDate","");
        params.put("page","1");
        params.put("pageSize","20");
        array.add(params);
        XposedBridge.log(" params" +array);
        JSONObject object = httpRequest(paramContext, "https://mobilegw.alipay.com/mgw.htm", array.toString(), "com.mybank.pcreditbatch.MYBKBillQueryFacade.queryBillListView");
        XposedBridge.log(TAG+object.toJSONString());
        if (!JSON.toJSONString(object).contains("mResData")) {
            return "";
        }
        String mResData = new String(Base64.decode(object.getString("mResData"), 2));

        StringBuilder  localStringBuilder = new StringBuilder();
        localStringBuilder.append("值：");
        localStringBuilder.append(mResData);
        XposedBridge.log(TAG+ localStringBuilder.toString());
        return mResData;
    }



    public String get64HexCurrentTimeMillis()
    {
        return XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.common.transport.utils.GtsUtils", MainHook.mClassLoader), "get64HexCurrentTimeMillis", new Object[0]).toString();
    }

    public String getDeviceId()
    {
        return XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.common.netsdkextdependapi.deviceinfo.DeviceInfoUtil", MainHook.mClassLoader), "getDeviceId", new Object[0]).toString();
    }

    public String getMiniWuaData(Context paramContext)
    {
        return XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.rdssecuritysdk.v2.face.RDSClient", MainHook.mClassLoader), "getMiniWuaData", new Class[] { Context.class }, new Object[] { paramContext }).toString();
    }

    public String getOrderDetails(Context paramContext, String paramString)
    {
        JSONObject object = httpRequest(paramContext, "https://mobilegw.alipay.com/mgw.htm", "[{\"tradeNo\":\"2019010410130010110000000011160013457117\"}]", "com.mybank.pcreditbatch.MYBKBillQueryFacade.queryBillDetailView");
        if (object.getBoolean("success").booleanValue())
        {
            String object1 = new String(Base64.decode(object.getString("resData"), 2));
            JSONObject.parseObject(object1);
            return object1;
        }
        return null;
    }

    public String getRequestDataDigest(String paramString)
    {
        try
        {
            paramString = new String(Base64.encode(MessageDigest.getInstance("MD5").digest(paramString.getBytes()), 0));
            return paramString;
        }
        catch (Exception e)
        {

        }
        return "";
    }

    public String getSign(Context paramContext, String paramString1, String paramString2, String paramString3)
    {
        StringBuilder localStringBuilder = new StringBuilder();
        localStringBuilder.append("Operation-Type=");
        localStringBuilder.append(paramString1);
        localStringBuilder.append("&Request-Data=");
        localStringBuilder.append(Base64.encodeToString(paramString2.getBytes(), 2));
        localStringBuilder.append("&Ts=");
        localStringBuilder.append(paramString3);
        paramString1 = localStringBuilder.toString();
        return XposedHelpers.getObjectField(XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.common.transport.utils.RpcSignUtil", MainHook.mClassLoader), "signature", new Object[] { paramContext, "", Boolean.valueOf(true), paramString1, Boolean.valueOf(true) }), "sign").toString();
    }

    public JSONObject httpRequest(Context context, String str, String str2, String str3)
    {
        try {
            XposedBridge.log("httpRequest "+str);
            Object newInstance = XposedHelpers.newInstance(XposedHelpers.findClass("com.alipay.mobile.common.transport.http.HttpManager", MainHook.mClassLoader),context);
            Object strOb = XposedHelpers.newInstance( XposedHelpers.findClass("com.alipay.mobile.common.transport.http.HttpUrlRequest",MainHook.mClassLoader),str);
            String str4 = get64HexCurrentTimeMillis();
            XposedBridge.log("httpRequest "+newInstance+" strOb "+strOb);
            XposedHelpers.callMethod(strOb, "setReqData", new Object[]{str2.getBytes()});
            XposedHelpers.callMethod(strOb, "setContentType", new Object[]{"application/json"});
            XposedHelpers.callMethod(strOb, "addTags", new Object[]{"id", ""});
            XposedHelpers.callMethod(strOb, "addTags", new Object[]{"operationType", str3});
            XposedHelpers.callMethod(strOb, "addTags", new Object[]{"reqDataDigest", getRequestDataDigest(str2)});
            XposedHelpers.callMethod(strOb, "addTags", new Object[]{"rpcVersion", "2"});
            Object[] objArr = new Object[2];
            objArr[0] = "UUID";
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getDeviceId());
            stringBuilder.append(str4);
            objArr[1] = stringBuilder.toString();
            XposedHelpers.callMethod(strOb, "addTags", objArr);
            XposedHelpers.callMethod(strOb, "addTags", new Object[]{"sign_time", "1"});
            XposedHelpers.callMethod(strOb, "setRequestMethod", new Object[]{"POST"});
            XposedHelpers.callMethod(strOb, "addHeader", new Object[]{new BasicHeader("miniwua", getMiniWuaData(context))});
            XposedHelpers.callMethod(strOb, "addHeader", new Object[]{new BasicHeader("visibleflag", "1")});
            XposedHelpers.callMethod(strOb, "addHeader", new Object[]{new BasicHeader("AppId", "Android-container")});
            XposedHelpers.callMethod(strOb, "addHeader", new Object[]{new BasicHeader("Version", "2")});
            XposedHelpers.callMethod(strOb, "addHeader", new Object[]{new BasicHeader("Did", getDeviceId())});
            XposedHelpers.callMethod(strOb, "addHeader", new Object[]{new BasicHeader("Operation-Type", str3)});
            XposedHelpers.callMethod(strOb, "addHeader", new Object[]{new BasicHeader("Ts", str4)});
            XposedHelpers.callMethod(strOb, "addHeader", new Object[]{new BasicHeader("Content-Type", "application/json")});
            XposedHelpers.callMethod(strOb, "addHeader", new Object[]{new BasicHeader("Sign", getSign(context, str3, str2, str4))});
            XposedHelpers.callMethod(strOb, "addHeader", new Object[]{new BasicHeader("signType", "0")});
            XposedHelpers.callMethod(strOb, "addHeader", new Object[]{new BasicHeader("clientVersion", "10.1.22.2139")});
            Object o = ((Future) XposedHelpers.callMethod(newInstance, "execute", new Object[]{strOb})).get(10000, TimeUnit.MILLISECONDS);
            StringBuilder sb = new StringBuilder();
            sb.append("[rsp]");
            sb.append(JSON.toJSONString(o ));
            Log.e(this.TAG, sb.toString());
            XposedBridge.log(this.TAG+sb.toString());
            if (o == null) {
                return JSON.parseObject("{\"msg\":\"请求超时\",\"resData\":\"\",\"success\":false}");
            }
            return JSON.parseObject(JSON.toJSONString(o));
        } catch (Exception e) {
            XposedBridge.log(e);
            Log.e(this.TAG, e.getMessage());
            StringBuilder sb = new StringBuilder();
            sb.append("{\"msg\":\"");
            sb.append(e.getMessage());
            sb.append("\",\"resData\":\"\",\"success\":false}");
            return JSON.parseObject(sb.toString());
        }

    }
}
