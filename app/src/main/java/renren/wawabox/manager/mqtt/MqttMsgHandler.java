package renren.wawabox.manager.mqtt;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import java.util.Arrays;

import commlib.xun.com.commlib.handler.CommWeakHandler;
import renren.wawabox.app.WawaNewApplication;
import renren.wawabox.config.MqttConstants;
import renren.wawabox.sp.CommSetting;
import renren.wawabox.update.AppUpdateManager;
import renren.wawabox.update.UpdateReceiver;
import renren.wawabox.utils.AppUtil;
import renren.wawabox.utils.DeviceUtil;
import renren.wawabox.utils.LogUtil;
import renren.wawabox.utils.PropertyUtil;

/**
 * Created by xunwang on 2017/10/9.
 */

public class MqttMsgHandler {
    private static final String TAG = "MqttMsgHandler";
    private static final MqttMsgHandler sInstance = new MqttMsgHandler();

    private static final int MSG_REBOOT = 0x01;
    private CommWeakHandler mHandler;

    private MqttMsgHandler() {
        mHandler = new CommWeakHandler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                if (MSG_REBOOT == message.what) {
                    MqttManager.getInstance().publistRebootMsg();
                    LogUtil.d(TAG, (String) message.obj, LogUtil.LOG_FILE_NAME_MQTT);
                    DeviceUtil.reboot((String) message.obj);
                }
                return false;
            }
        });
    }

    public static final MqttMsgHandler getInstance() {
        return sInstance;
    }

    public boolean handlerMsg(String topic, String msg) {
        if (TextUtils.isEmpty(topic)) {
            return false;
        }
        try {
            if (MqttConstants.TOPIC_ANDROID_IN.equalsIgnoreCase(topic)) {
                if (TextUtils.isEmpty(msg)) {
                    return false;
                }
                msg = msg.trim();
                if (msg.startsWith("update")) {
                    checkUpdateAndDownLoad();
                } else if (msg.startsWith("install")) {
                    install();
                } else if (msg.startsWith("version")) {
                    pubVersion();
                } else if (msg.startsWith("reboot")) {
                    sendRebootMsg(String.format("收到重启命令 topic:%s , msg:%s", topic, msg));
                } else if (msg.startsWith("deleteshare;")) {
                    //删除sharepreference
                    String[] temp = msg.split(";");
                    if (temp.length > 1) {
                        String key = temp[1];
                        CommSetting.clearKey(key);
                        DeviceUtil.reboot("清除key:" + key);
                    }
                }
                return true;
            } else if (MqttConstants.TOPIC_ANDROID_DEBUG.equalsIgnoreCase(topic)) {
                msg = msg.trim();
                if (msg.startsWith("allupdate")) {
                    checkUpdateAndDownLoad();
                } else if (msg.startsWith("allinstall")) {
                    install();
                } else if (msg.startsWith("allversion")) {
                    pubVersion();
                } else if (msg.startsWith("allreboot")) {
                    sendRebootMsg(String.format("收到重启命令 topic:%s , msg:%s", topic, msg));
                } else if (msg.startsWith("update,id=")) {
                    //eg: update,id=12,34,56,43
                    String[] ids = msg.substring(10).split(",");
                    if (Arrays.asList(ids).contains(PropertyUtil.MQTT_CLAW_MACHINE_ID + "")) {
                        checkUpdateAndDownLoad();
                    }
                } else if (msg.startsWith("deleteshare;")) {
                    // eg: deleteshare;key;id=3001
                    String[] temp = msg.split(";");
                    if (temp != null && temp.length > 2) {
                        String key = temp[1];
                        String[] ids = temp[2].split("=");
                        if (ids != null && ids.length > 1) {
                            String[] idArray = ids[1].split(",");
                            if (Arrays.asList(idArray).contains(PropertyUtil.MQTT_CLAW_MACHINE_ID + "")) {
                                CommSetting.clearKey(key);
                                DeviceUtil.reboot("清除key:" + key);
                            }
                        }
                    }
                } else if (msg.startsWith("install,id=")) {
                    String[] ids = msg.substring(11).split(",");
                    if (Arrays.asList(ids).contains(PropertyUtil.MQTT_CLAW_MACHINE_ID + "")) {
                        install();
                    }
                } else if (msg.startsWith("version,id=")) {
                    LogUtil.d(TAG, "ids = " + msg.substring(11));
                    String[] ids = msg.substring(11).split(",");
                    if (Arrays.asList(ids).contains(PropertyUtil.MQTT_CLAW_MACHINE_ID + "")) {
                        pubVersion();
                    }
                } else if (msg.startsWith("reboot,id=")) {
                    LogUtil.d(TAG, "ids = " + msg.substring(10));
                    String[] ids = msg.substring(10).split(",");
                    if (Arrays.asList(ids).contains(PropertyUtil.MQTT_CLAW_MACHINE_ID + "")) {
                        sendRebootMsg(String.format("收到重启命令 topic:%s , msg:%s", topic, msg));
                    }
                } else if (msg.startsWith("deleteapk")) {
                    AppUtil.deleteFile(UpdateReceiver.APK_PATH);
                } else if (msg.startsWith("deletetencentlog")) {
                    AppUtil.deleteDir(Environment.getExternalStorageDirectory() + "/tencent");
                }
                return true;
            }
        } catch (Exception e) {
            //出错就不管
            LogUtil.d(TAG, "handler msg error topic = " + topic + " , msg = " + msg);
        }
        return false;
    }

    private void sendRebootMsg(String reason) {
        Message rebootMsg = mHandler.obtainMessage(MSG_REBOOT);
        rebootMsg.obj = reason;
        LogUtil.d(TAG, "sendRebootMsg", true);
        if (PropertyUtil.CAM2_ONLY) {
            mHandler.sendMessage(rebootMsg);
        } else {
            //正面盒子等五秒重启 保障成功率
            mHandler.sendMessageDelayed(rebootMsg, 5000);
        }
    }

    private static void checkUpdateAndDownLoad() {
        AppUpdateManager.getInstance().updateAppRightNow(WawaNewApplication.getAppContext());
    }

    private static void install() {
        AppUpdateManager.getInstance().intallAppRightNow(WawaNewApplication.getAppContext());
    }

    private static void pubVersion() {
        MqttManager.getInstance().publistVersionMsg();
    }
}
