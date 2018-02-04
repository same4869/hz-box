package renren.wawabox.update;

import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import renren.wawabox.config.Constants;
import renren.wawabox.sp.CommSetting;
import renren.wawabox.utils.LogUtil;
import renren.wawabox.utils.StreamLogUtil;

/**
 * 定时发送请求任务，有新版本同时辅助APP下载，静默安装并打开
 * Created by xunwang on 2017/8/28.
 */

public class AppUpdateManager {
    public static final String TAG = "AppUpdateManager";

    public static final String REQUEST_UPDATE_LOCAL_ACTION = "requset.update.local.action";
    public static final String REQUEST_UPDATE_REMOTE_ACTION = "requset.update.remote.action";
    public static final String REQUEST_INSTALL_LOCAL_ACTION = "requset.install.local.action";
    public static final String REQUEST_INSTALL_REMOTE_ACTION = "requset.install.remote.action";
    public static final String REQUEST_INSTALL_OTHER_LOCAL_ACTION = "requset.install.other.local.action";
    public static final String REQUEST_INSTALL_OTHER_REMOTE_ACTION = "requset.install.other.remote.action";

    private static int UPDATE_TIME_HOUR = 3;
    private static final int UPDATE_TIME_MIN = 30;
    private static final int UPDATE_TIME_SEC = 0;
    private static final int INSTALL_TIME_HOUR = 4;
    private static final int INSTALL_TIME_MIN = 0;
    private static final int INSTALL_TIME_SEC = 0;

    private static AppUpdateManager instance;

    private Timer timer1;
    private Timer timer2;
    private Timer timer3;

    public static boolean IS_DEBUG = true; //ture就是正式的

    private AppUpdateManager() {
        int id = CommSetting.getMachineId();
        if (id > 0) {
            UPDATE_TIME_HOUR = id % 10;
        }
    }

    public static AppUpdateManager getInstance() {
        if (instance == null) {
            synchronized (AppUpdateManager.class) {
                if (instance == null) {
                    instance = new AppUpdateManager();
                }
            }
        }
        return instance;
    }

    /**
     * 周期性检查更新，如有更新通知辅助app下载,每天3点
     *
     * @param context
     */
    public void initUpdateAmTime(final Context context) {
        long triggerAtMillis, intervalMillis;
        final Intent intent = new Intent();
        intent.setAction(REQUEST_UPDATE_LOCAL_ACTION);
        if (IS_DEBUG) {
            LogUtil.d(Constants.TAG, "getRequestTime(" + UPDATE_TIME_HOUR + ", " + UPDATE_TIME_MIN + ", " + UPDATE_TIME_SEC + ") --> " + getRequestTime(UPDATE_TIME_HOUR, UPDATE_TIME_MIN, UPDATE_TIME_SEC) + " System.currentTimeMillis() --> " + System.currentTimeMillis(), LogUtil.LOG_FILE_NAME_NORMAL);
            triggerAtMillis = getRequestTime(UPDATE_TIME_HOUR, UPDATE_TIME_MIN, UPDATE_TIME_SEC) - System.currentTimeMillis();
            if (triggerAtMillis < 0) {
                triggerAtMillis = getRequestToTime(UPDATE_TIME_HOUR, UPDATE_TIME_MIN, UPDATE_TIME_SEC) - System.currentTimeMillis();
            }
            intervalMillis = 24 * 60 * 60 * 1000;
        } else {
            triggerAtMillis = getRequestTime(3, 0, 0);
            intervalMillis = 24 * 60 * 60 * 1000;
        }

        LogUtil.d(TAG, "开启自动下载轮询 timer1 triggerAtMillis --> " + triggerAtMillis + " intervalMillis --> " + intervalMillis, LogUtil.LOG_FILE_NAME_NORMAL);
        StreamLogUtil.putLog("开启自动下载轮询 triggerAtMillis --> " + triggerAtMillis);
        if (timer1 != null) {
            timer1.cancel();
        }
        timer1 = new Timer();
        timer1.schedule(new TimerTask() {
            @Override
            public void run() {
                LogUtil.d(TAG, "UpdateReceiver timer1", LogUtil.LOG_FILE_NAME_NORMAL);
                context.sendBroadcast(intent);
            }
        }, triggerAtMillis, intervalMillis);
    }


    /**
     * 周期性发送安装新版本APP的指令,每天4点
     *
     * @param context
     */
    public void initInstallAmTime(final Context context) {
        long triggerAtMillis, intervalMillis;
        final Intent intent = new Intent();
        intent.setAction(REQUEST_INSTALL_LOCAL_ACTION);
        if (IS_DEBUG) {
            LogUtil.d(TAG, "getRequestTime(" + INSTALL_TIME_HOUR + ", " + INSTALL_TIME_MIN + ", " + INSTALL_TIME_SEC + ") --> " + getRequestTime(INSTALL_TIME_HOUR, INSTALL_TIME_MIN, INSTALL_TIME_SEC) + " System.currentTimeMillis() --> " + System.currentTimeMillis(), LogUtil.LOG_FILE_NAME_NORMAL);
            triggerAtMillis = getRequestTime(INSTALL_TIME_HOUR, INSTALL_TIME_MIN, INSTALL_TIME_SEC) - System.currentTimeMillis();
            if (triggerAtMillis < 0) {
                triggerAtMillis = getRequestToTime(INSTALL_TIME_HOUR, INSTALL_TIME_MIN, INSTALL_TIME_SEC) - System.currentTimeMillis();
            }
            intervalMillis = 24 * 60 * 60 * 1000;
        } else {
            triggerAtMillis = getRequestTime(4, 0, 0);
            intervalMillis = 24 * 60 * 60 * 1000;
        }

        LogUtil.d("kkkkkkkk", "开启自动安装轮询 timer2 triggerAtMillis --> " + triggerAtMillis + " intervalMillis --> " + intervalMillis, LogUtil.LOG_FILE_NAME_NORMAL);
        StreamLogUtil.putLog("开启自动安装轮询 triggerAtMillis --> " + triggerAtMillis);
        if (timer2 != null) {
            timer2.cancel();
        }
        timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            @Override
            public void run() {
                LogUtil.d(TAG, "UpdateReceiver timer2", LogUtil.LOG_FILE_NAME_NORMAL);
                context.sendBroadcast(intent);
            }
        }, triggerAtMillis, intervalMillis);
    }

    /**
     * 立即发送检查下载新版本APP的指令
     *
     * @param context
     */
    public void updateAppRightNow(Context context) {
        Intent intent = new Intent();
        intent.setAction(REQUEST_UPDATE_LOCAL_ACTION);
        context.sendBroadcast(intent);
    }

    /**
     * 立即发送安装新版本APP的指令
     *
     * @param context
     */
    public void intallAppRightNow(Context context) {
        Intent intent = new Intent();
        intent.setAction(REQUEST_INSTALL_LOCAL_ACTION);
        context.sendBroadcast(intent);
    }

    /**
     * 获得当天N点时间,如果当天超过N点，则返回明天的N点
     *
     * @param hourOfDay
     * @return
     */
    private long getRequestTime(int hourOfDay, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        if (cal.get(Calendar.HOUR_OF_DAY) > hourOfDay) {
            cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + 1);
        }
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();/// 1000;
    }

    private long getRequestToTime(int hourOfDay, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + 1);
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();/// 1000;
    }

    public void cancelAllTimer() {
        if (timer1 != null) {
            timer1.cancel();
        }
        if (timer2 != null) {
            timer2.cancel();
        }
        if (timer3 != null) {
            timer3.cancel();
        }
    }
}
