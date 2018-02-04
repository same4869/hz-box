package renren.wawabox.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.av.utils.CrashHandler;
import com.tencent.bugly.imsdk.CrashModule;
import com.tencent.bugly.imsdk.crashreport.CrashReport;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.view.AVRootView;

import java.util.Iterator;
import java.util.Map;

import commlib.xun.com.commlib.thread.CommThreadPool;
import renren.wawabox.R;
import renren.wawabox.app.WawaNewApplication;
import renren.wawabox.config.Constants;
import renren.wawabox.config.MqttConstants;
import renren.wawabox.manager.live.TencentLiveManager;
import renren.wawabox.manager.mind.MindControllerManager;
import renren.wawabox.manager.mqtt.MqttManager;
import renren.wawabox.service.ScreenOnOffService;
import renren.wawabox.sp.CommSetting;
import renren.wawabox.utils.AppUtil;
import renren.wawabox.utils.ConvertUtil;
import renren.wawabox.utils.FileUtil;
import renren.wawabox.utils.LogUtil;
import renren.wawabox.utils.NetworkKeeperUtil;
import renren.wawabox.utils.PropertyUtil;
import renren.wawabox.utils.StreamLogUtil;

import static renren.wawabox.config.Constants.IS_UPLOAD_TO_SERVER;

public class BoxMainActivity extends AppCompatActivity {
    private ScrollView outLogSv;
    private TextView outLogTv;
    private AVRootView avRootView;
    private HandlerThread mCheckerThread = null;
    /*是否进行直播检测*/
    private static final boolean mCheckLiveAble = true;
    private static final int MSG_CHECK_LIVE = 0x01;
    private static final int TIME_CHECK_LIVE = 3 * 60 * 1000;
    private Map<String, String> tokenMaps;
    private int statusCheckCount = 60; //60*5秒的时候检测一次并上报
    private long mExitTime;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_box_main);
        verifyStoragePermissions(this);

        initStreamLog();
        initView();
        initActionBar();
        initIdAndToken();
        init();
    }

    private void initIdAndToken() {
        int id = CommSetting.getMachineId();
        int channelId = 0;
        if (id == -1) {
            channelId = (int) initIdIfHas();
        }
        int finalId = (id == -1 ? channelId : id);
        LogUtil.d(Constants.TAG, "finalId --> " + finalId);
        StreamLogUtil.putLog("finalId --> " + finalId + " channelId --> " + channelId + " id --> " + id);
        initToken(finalId);

        String token = CommSetting.getToken();
        if (token == null) {
            StreamLogUtil.putLog("没有配置该设备的token，请检查");
        }
        StreamLogUtil.putLog("token --> " + token);
    }

    /**
     * 现在的token都通过本地map直接拿，拿得到才配上
     */
    private void initToken(int id) {
        if (id == 0) {
            return;
        }
        FileUtil.copyFilesFassets(WawaNewApplication.getAppContext(), "token_mapping.txt", Environment.getExternalStorageDirectory().getPath() + "/token_mapping.txt");
        tokenMaps = FileUtil.fTM(Environment.getExternalStorageDirectory().getPath() + "/token_mapping.txt");
        Iterator iter = tokenMaps.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();
            if (ConvertUtil.convertToInt(key, -1) == id) {
                StreamLogUtil.putLog("set token --> " + val);
                CommSetting.setToken(val);
                break;
            }
        }
    }


    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("WawajiBox TV 【versionName: " + WawaNewApplication.VERSION_NAME + "】 【versionCode: " + WawaNewApplication.VERSION_CODE + "】");
            actionBar.setSubtitle(NetworkKeeperUtil.getIPAddress(true));
        }
    }

    private void init() {
        WawaNewApplication.sActivity = this;

        StreamLogUtil.putLog("versionName --> " + WawaNewApplication.VERSION_NAME + " versionCode --> " + WawaNewApplication.VERSION_CODE);
        StreamLogUtil.putLog("ip --> " + NetworkKeeperUtil.getIPAddress(true));

        TencentLiveManager.init(this, avRootView);

        if (CommSetting.getToken() == null) {
            StreamLogUtil.putLog("没有配置该设备的token，请检查");
        }

        StreamLogUtil.putLog("token --> " + CommSetting.getToken());
        PropertyUtil.init();
        MindControllerManager.init(this);

        if (PropertyUtil.WAIT_ID) {
            // 等id 配置
            MqttManager.getInstance().connect();
        } else {
            MindControllerManager.getInstance().start();
            checkGuardSelf();
        }

        ILiveLoginManager.getInstance().setUserStatusListener(new ILiveLoginManager.TILVBStatusListener() {
            @Override
            public void onForceOffline(int i, String s) {
                MindControllerManager.getInstance().beatCheck();

            }
        });

        TencentLiveManager.checkPermission(this);

        startService(new Intent(this, ScreenOnOffService.class));
    }

    /**
     * 初始化日志输出到屏幕系统
     */
    private void initStreamLog() {
        StreamLogUtil.initLog(getMainLooper(), new StreamLogUtil.OnRecvLog() {
            @Override
            public void onRecvLog(String log) {
                LogUtil.d(Constants.TAG, log);
                String msg = StreamLogUtil.rebuildOutLog();
                outLogTv.setText(msg);
                outLogSv.fullScroll(ScrollView.FOCUS_DOWN);
                if (IS_UPLOAD_TO_SERVER) {
                    LogUtil.startUploadToServer(String.valueOf(PropertyUtil.MQTT_CLAW_MACHINE_ID), PropertyUtil.CAM2_ONLY, 0, msg, false);
                }
            }

            @Override
            public void onStateChange(String state) {
                LogUtil.d(Constants.TAG, "state change to --> " + state);
            }
        });
    }

    private void initView() {
        outLogTv = (TextView) findViewById(R.id.out_log_tv);
        outLogSv = (ScrollView) findViewById(R.id.out_log_sv);
        avRootView = (AVRootView) findViewById(R.id.av_root_view);
    }

    private void checkGuardSelf() {
        CommThreadPool.poolExecute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);
                        statusCheckCount--;
                        if (statusCheckCount <= 0) {
                            String statusCheckStr;
                            if (!PropertyUtil.CAM2_ONLY) {
                                statusCheckStr = MqttConstants.getCam1Status();
                            } else {
                                statusCheckStr = MqttConstants.getCam2Status();
                            }
                            StreamLogUtil.putLog(statusCheckStr);
                            MqttManager.getInstance().publishStatusChecker("盒子id --> " + PropertyUtil.MQTT_CLAW_MACHINE_ID + " 检测结果：" + statusCheckStr);
                            statusCheckCount = 60;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                    MindControllerManager.getInstance().beatCheck();
                }
            }
        });

        if (mCheckLiveAble && (mCheckerThread == null || !mCheckerThread.isAlive())) {
            mCheckerThread = new HandlerThread("check");
            mCheckerThread.start();
            Handler mCheckerHandler = new Handler(mCheckerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == MSG_CHECK_LIVE) {
                        LogUtil.d(Constants.TAG, "tencent check live", LogUtil.LOG_FILE_NAME_NORMAL);
                        StreamLogUtil.putLog("tencent check live");
                        //TencentLiveManager.checkAndKeep();
                        sendEmptyMessageDelayed(MSG_CHECK_LIVE, TIME_CHECK_LIVE);
                    }
                }
            };
            mCheckerHandler.sendEmptyMessageDelayed(MSG_CHECK_LIVE, TIME_CHECK_LIVE);
        }
    }

    /**
     * 根据渠道包获得对应盒子的ID，如果是升级包就没有
     *
     * @return
     */
    private long initIdIfHas() {
        long channelId = 0;
        try {
            channelId = Long.parseLong(AppUtil.getChannelByMeta(getApplicationContext()));
            LogUtil.d(Constants.TAG, "channelId --> " + channelId, LogUtil.LOG_FILE_NAME_NORMAL);
            long channelId2;
            if (channelId > 1000000) {
                channelId2 = channelId - 1000000;
                CommSetting.setCam2Only(true);
            } else {
                channelId2 = channelId;
            }
            LogUtil.d(Constants.TAG, "real id channelId2 --> " + channelId2, LogUtil.LOG_FILE_NAME_NORMAL);
            CommSetting.setMachineId((int) channelId2);
        } catch (NumberFormatException e) {
            LogUtil.d(Constants.TAG, "init id err --> " + e.getMessage(), LogUtil.LOG_FILE_NAME_NORMAL);
            e.printStackTrace();
        }
        return channelId;
    }

    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            } else {
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
