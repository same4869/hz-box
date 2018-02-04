package renren.wawabox.utils;

import com.tencent.ilivesdk.ILiveSDK;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;

import renren.wawabox.config.Constants;
import renren.wawabox.manager.mqtt.MqttManager;

import static renren.wawabox.config.Constants.IS_UPLOAD_TO_SERVER;

/**
 * Created by xunwang on 2017/9/15.
 */

public class DeviceUtil {

    /**
     * 重启,只有捕获到crash了才立即重启，不然就用下面那个5秒后重启
     */
    public static void reboot(String reason) {
        MqttManager.getInstance().publishCamReboot();
        LogUtil.d(Constants.TAG, "【reboot reason】:" + reason);
        MqttManager.getInstance().publishRebootChecker("id:" + PropertyUtil.MQTT_CLAW_MACHINE_ID + " isCam2:" + PropertyUtil.CAM2_ONLY + " reason:" + reason);
        if (IS_UPLOAD_TO_SERVER) {
            LogUtil.startUploadToServer(String.valueOf(PropertyUtil.MQTT_CLAW_MACHINE_ID), PropertyUtil.CAM2_ONLY, 0, "reboot reason --> " + reason, true);
        }
        ILiveSDK.getInstance().runOnMainThread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream dataOutputStream = null;
                BufferedReader errorStream = null;
                try {
                    // 申请su权限
                    Process process = Runtime.getRuntime().exec("su");
                    dataOutputStream = new DataOutputStream(process.getOutputStream());
                    dataOutputStream.writeBytes(" reboot \n");
                    dataOutputStream.flush();
                    process.waitFor();
                } catch (Exception e) {
                    LogUtil.e(Constants.TAG, "reboot filed : " + e.getMessage());
                } finally {
                    try {
                        if (dataOutputStream != null) {
                            dataOutputStream.close();
                        }
                        if (errorStream != null) {
                            errorStream.close();
                        }
                    } catch (IOException e) {
                        LogUtil.e(Constants.TAG, "reboot filed : " + e.getMessage());
                    }
                }
            }
        }, 5000);
    }

    public static void reboot(String reason, boolean rebootNow) {
        if (rebootNow) {//如果rebootNow是false，是给ScreenOnOffService用的，其他的暂时不要用
            MqttManager.getInstance().publishCamReboot();
            LogUtil.d(Constants.TAG, "【reboot reason】:" + reason);
            StreamLogUtil.putLog("【reboot reason】:" + reason);
        }
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        try {
            // 申请su权限
            Process process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            dataOutputStream.writeBytes(" reboot \n");
            dataOutputStream.flush();
            process.waitFor();
        } catch (Exception e) {
            LogUtil.e(Constants.TAG, "reboot filed : " + e.getMessage());
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (IOException e) {
                LogUtil.e(Constants.TAG, "reboot filed : " + e.getMessage());
            }
        }
    }

    /**
     * 申请root权限
     */
    public static void requestSU() {
        try {
            Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
