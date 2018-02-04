package renren.wawabox.app;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.PowerManager;

import java.util.List;

import renren.wawabox.config.Constants;
import renren.wawabox.update.AppUpdateManager;
import renren.wawabox.utils.CrashHandlerUtil;
import renren.wawabox.utils.DeviceUtil;
import renren.wawabox.utils.LogUtil;

import static renren.wawabox.config.Constants.IS_ENABLE_UPDATE;

/**
 * Created by xunwang on 2017/9/12.
 */

public class WawaNewApplication extends Application {
    protected static WawaNewApplication instance;
    protected static Context context;
    private PowerManager.WakeLock wakeLock;
    public static Activity sActivity;

    public static int VERSION_CODE = 0;
    public static String VERSION_NAME = null;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        context = getApplicationContext();

        PackageManager pm = getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
            VERSION_CODE = info.versionCode;
            VERSION_NAME = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        PowerManager pm2 = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm2 != null) {
            wakeLock = pm2.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
            wakeLock.acquire();
        }

        DeviceUtil.requestSU();
        LogUtil.init(this, Constants.IS_LOG, Constants.IS_LOG_IN_FILE);

        if (shouldInit() && IS_ENABLE_UPDATE) {
            AppUpdateManager.getInstance().initUpdateAmTime(getApplicationContext());
            AppUpdateManager.getInstance().initInstallAmTime(getApplicationContext());
            LogUtil.deleteFileLog();
        }

        //在这里为应用设置异常处理程序，然后我们的程序才能捕获未处理的异常
        CrashHandlerUtil crashHandler = CrashHandlerUtil.getInstance();
        crashHandler.init(this, LogUtil.getLog(context).getAbsolutePath() + "/");
    }

    public static Context getAppContext() {
        return context;
    }

    public static WawaNewApplication getInstance() {
        return instance;
    }

    public SharedPreferences getCommSharedPreferences(String tbl) {
        return getSharedPreferences(tbl, Context.MODE_PRIVATE);
    }

    private boolean shouldInit() {
        ActivityManager am = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
        String mainProcessName = getPackageName();
        int myPid = android.os.Process.myPid();

        for (ActivityManager.RunningAppProcessInfo info : processInfos) {
            if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (wakeLock != null) {
            wakeLock.release();
        }
    }
}
