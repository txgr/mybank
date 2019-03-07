package com.android.alipayhook;


import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.android.alipayhook.receiver.Receiver1;
import com.android.alipayhook.receiver.Receiver2;
import com.android.alipayhook.receiver.Service2;
import com.marswin89.marsdaemon.DaemonClient;
import com.marswin89.marsdaemon.DaemonConfigurations;


/**
 * 

* @ClassName: CustomApplcation

* @Description: TODO(这里用一句话描述这个类的作用)

* @author SuXiaoliang

* @date 2018年6月23日 下午1:26:02

*
 */

public class CustomApplcation extends Application {

	public static CustomApplcation mInstance;
	private static Context context;

	@Override
	public void onCreate() {
		super.onCreate();
		// 崩溃记录
		context = getApplicationContext();
		mInstance = this;
	}

	public static CustomApplcation getInstance() {
		return mInstance;
	}

	public static Context getContext() {
		return context;
	}
	private DaemonClient daemonClient;

	protected DaemonConfigurations getDaemonConfigurations() {
		DaemonConfigurations.DaemonConfiguration configuration1 = new DaemonConfigurations.DaemonConfiguration("com.android.qqpay:daemon_service", DaemonService.class.getCanonicalName(), Receiver1.class.getCanonicalName());
		DaemonConfigurations.DaemonConfiguration configuration2 = new DaemonConfigurations.DaemonConfiguration("com.android.qqpay:process2", Service2.class.getCanonicalName(), Receiver2.class.getCanonicalName());
		DaemonConfigurations.DaemonListener listener = new DaemonConfigurations.DaemonListener() {
			@Override
			public void onPersistentStart(Context context) {
				Toast.makeText(context,"onPersistentStart",Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onDaemonAssistantStart(Context context) {
				Toast.makeText(context,"onDaemonAssistantStart",Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onWatchDaemonDaed() {

              // CustomApplcation.this.startService(new Intent(getContext(),DaemonService.class));
				//   Toast.makeText(getApplicationContext(),"onWatchDaemonDaed",Toast.LENGTH_SHORT).show();
			}
		};
		// return new DaemonConfigurations(configuration1, configuration2);//listener can be null
		return new DaemonConfigurations(configuration1, configuration2, listener);
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		daemonClient =new DaemonClient(getDaemonConfigurations());
		daemonClient.onAttachBaseContext(this);
	}
}
