package renren.wawabox.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

import java.util.List;

import commlib.xun.com.commlib.handler.CommWeakHandler;
import renren.wawabox.utils.DeviceUtil;
import renren.wawabox.utils.LogUtil;

/**
 * Created by xunwang on 2017/12/12.
 */

public class ScreenOnOffService extends Service {
    private static final String PACKAGE_NAME = "renren.wawabox";
    private static final long CHECK_TIME = 1000 * 60 * 10;

    /**
     * 为了防止一开始就检查不到直接无限重启，采取的策略是第一次如果检查到包名的主进程，证明是正常的，如果后面突然某一次主进程挂掉了，可以认为是程序crash了，这时候可以尝试重启
     * 目前可能在5分钟左右主进程被拉起来，不过被拉起来无所谓，importance会变（从100变到300）
     */
//    private boolean isShouldReboot;
    private boolean isHasPackage;
    private int importance;

    private CommWeakHandler mHandler = new CommWeakHandler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            checkRunPackageName();
            mHandler.sendEmptyMessageDelayed(0, CHECK_TIME);
            return false;
        }
    });

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler.removeCallbacksAndMessages(null);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenOReceiver, filter);

        mHandler.sendEmptyMessageDelayed(0, CHECK_TIME);
    }

    private void checkRunPackageName() {
        ActivityManager _ActivityManager = (ActivityManager) this
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> list = _ActivityManager
                .getRunningAppProcesses();
        int i = list.size();
        LogUtil.d("kkkkkkkk", String.valueOf(i));
        for (int k = 0; k < list.size(); k++) {
            LogUtil.d("kkkkkkkk", list.get(k).processName + " importance --> " + list.get(k).importance);
        }
        for (int j = 0; j < list.size(); j++) {
            //如果上一次记录的importance小于这一次的，证明已经挂了但又被拉起来了，就算有主进程也重启
            if (importance != 0 && importance < list.get(j).importance && PACKAGE_NAME.equals(list.get(j).processName)) {
                LogUtil.d("kkkkkkkk", "主进程挂了但被拉起来了，重启");
                DeviceUtil.reboot("主进程挂了但被拉起来了，重启",false);
                break;
            }
            //如果importance变大且有主进程，认为正常的，更新下就行
            if (PACKAGE_NAME.equals(list.get(j).processName)) {
                isHasPackage = true;
                importance = list.get(j).importance;
                break;
            }
            //如果没有主进程了，直接重启
            if (j == list.size() - 1 && !PACKAGE_NAME.equals(list.get(j).processName) && isHasPackage) {
                DeviceUtil.reboot("主进程没了，重启",false);
                LogUtil.d("kkkkkkkk", "主进程没了，重启");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenOReceiver);
        mHandler.removeCallbacksAndMessages(null);
    }

    private BroadcastReceiver mScreenOReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                LogUtil.d("kkkkkkkk", "android.intent.action.SCREEN_ON");
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                LogUtil.d("kkkkkkkk", "android.intent.action.SCREEN_OFF");
            }
        }
    };
}
