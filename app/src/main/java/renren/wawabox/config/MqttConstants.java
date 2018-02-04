package renren.wawabox.config;

import java.util.ArrayList;
import java.util.List;

import renren.wawabox.utils.LogUtil;

/**
 * Created by xunwang on 2017/9/14.
 */

public class MqttConstants {
    public static final int MSG_PREPARE_PUB = 1;
    public static final int MSG_PUB_NOW = 2;
    public static final int MSG_GET_RESULT_OVERTIME = 3;
    public static final int MSG_ADD_SUB = 4;
    public static final int MSG_WAIT_RESULT = 5;
    public static final int MSG_CAM2_HANDLE = 6;
    public static final int MSG_CAM2_WAITING = 7;
    public static final int MSG_R1_CMD = 8;
    public static final int MSG_SETTING_TRIG = 9;

    public static final String TAG = "MqttManager";
    public static final int TIME_TRIP = 6;//Z 向下保护时间改为 6 秒

    public static final String ConnectionStatusProperty = "ConnectionStatusProperty";

    public static final String HANDLE_CONNECTION = "unique";
    public static final String HANDLE_SUBSCRIPTION = "subscription";

    public static String TOPIC_SUB = "claw/out/";
    public static String TOPIC_PUB = "claw/in/";
    public static String TOPIC_CAM = "f_camera/";

    public static String TOPIC_ANDROID_OUT = "android/out/";
    public static String TOPIC_ANDROID_IN = "android/in/";
    public static String TOPIC_ANDROID_DEBUG = "android/debug";
    public static String TOPIC_ANDROID_VERSION = "android/version";
    public static String TOPIC_ANDROID_REBOOT = "android/reboot";
    public static String TOPIC_ANDROID_STATUS = "android/status";
    public static String TOPIC_ANDROID_REBOOT_REASON = "android/rebootreason";

    /**
     * 配置娃娃机id;
     */
    public static String TOPIC_SETTING = "setting";
    public static String TOPIC_SETTING_ID = "setting/";

    public static String TOPIC_WATCH_DOG = "watch_dog/";

    public static String TOPIC_DOG_REPORT = "dog_report";

    public static String CMD_CAM_START = "start";
    public static String CMD_CAM_END = "end";
    public static String CMD_CAM_WAIT = "wait";
    public static String CMD_CAM_REBOOT = "reboot";
    public static String CMD_CAM_ON = "on";
    public static String CMD_CAM_REQUEST_LIVE_URL = "req_url";
    public static String CMD_STREAM_ON = "sream_on";


    public static String CMD_RESTART = "r";

    public static final String SENSE_1_RESULT = "in1;";
    public static final String SENSE_2_RESULT = "in2;";

    public static int SENSOR_1_GOT = 1;
    public static int SENSOR_2_GOT = 1;

    public static String TOPIC_IP_WATCH = "nidaye";
    public static String TOPIC_IP_OUT = "daye";

    /**
     * 预设速度, 0~100
     */
    public static final int SPEED = 100;

    public static String ORI_Y_FRONT = "";
    public static String ORI_Y_BACK = "-";
    public static String ORI_X_FRONT = "";
    public static String ORI_X_BACK = "-";

    public static String SWITCH_Y_FRONT = "y_2";
    public static String SWITCH_Y_BACK = "y_1";

    public static final int MAX_CLAW = 100;

    public static final int MAX_DOWN_CLAWTIME = 1600;
    public static final int MIN_DOWN_CLAWTIME = 1000;

    public static final int CLAW_MODE_TAKE_RELEASE = 0;
    public static final int CLAW_MODE_TOP_RELEASE = 1;

    public static final int G_RAZ_2 = 10;
    public static final int G_RAZ_1 = 11;
    public static final int G_LENGTH = 12;

    private static int mCurClawMode = 1;

    public static final String CONTROL_FORWARD_START_NEW = "1";
    public static final String CONTROL_FORWARD_END_NEW = "2";
    public static final String CONTROL_BACKWARD_START_NEW = "3";
    public static final String CONTROL_BACKWARD_END_NEW = "4";
    public static final String CONTROL_RIGHT_START_NEW = "7";
    public static final String CONTROL_RIGHT_END_NEW = "8";
    public static final String CONTROL_LEFT_START_NEW = "5";
    public static final String CONTROL_LEFT_END_NEW = "6";
    public static final String CONTROL_TAKE_NEW = "0";

    public static void setCurMode(int mode) {
        mCurClawMode = mode;
        LogUtil.d(Constants.TAG, "mode = " + mode, LogUtil.LOG_FILE_NAME_MQTT);
    }

    public static int getClawMode() {
        return mCurClawMode;
    }

    /**
     * 以下为盒子的一系列状态，成功则置为true，失败的话会在每次上报轮询中通过android/status上报出去
     */
    public static boolean STATUS_MIND_REQUEST_SERVER_IP_CAM1 = false;
    public static boolean STATUS_WAWAJI_SERVER_AUTH_CAM1 = false;
    public static boolean STATUS_REQUEST_LIVE_ID_CAM1 = false;
    public static boolean STATUS_LIVE_LOGIN_SUC_CAM1 = false;
    public static boolean STATUS_LIVE_CREATE_ROOM_SUC_CAM1 = false;

//    public static boolean STATUS_LIVE_PANGLU_SUC = false;
//    public static boolean STATUS_LIVE_CHECK_LIVE = false;

    public static boolean STATUS_LIVE_ID_GET_SUC_CAM2 = false;
    public static boolean STATUS_LIVE_LOGIN_SUC_CAM2 = false;
    public static boolean STATUS_LIVE_JOIN_ROOM_SUC_CAM2 = false;
    public static boolean STATUS_LIVE_UP_TO_VIDEO_SUC_CAM2 = false;

    private static List<Boolean> statusCam1List = new ArrayList<>();
    private static List<Boolean> statusCam2List = new ArrayList<>();

    private static void setupStatusCam1() {
        statusCam1List.add(STATUS_MIND_REQUEST_SERVER_IP_CAM1);
        statusCam1List.add(STATUS_WAWAJI_SERVER_AUTH_CAM1);
        statusCam1List.add(STATUS_REQUEST_LIVE_ID_CAM1);
        statusCam1List.add(STATUS_LIVE_LOGIN_SUC_CAM1);
        statusCam1List.add(STATUS_LIVE_CREATE_ROOM_SUC_CAM1);
//        statusCam1List.add(STATUS_LIVE_PANGLU_SUC);
//        statusCam1List.add(STATUS_LIVE_CHECK_LIVE);
    }

    private static void setupStatusCam2() {
        statusCam2List.add(STATUS_LIVE_ID_GET_SUC_CAM2);
        statusCam2List.add(STATUS_LIVE_LOGIN_SUC_CAM2);
        statusCam2List.add(STATUS_LIVE_JOIN_ROOM_SUC_CAM2);
        statusCam2List.add(STATUS_LIVE_UP_TO_VIDEO_SUC_CAM2);
//        statusCam2List.add(STATUS_LIVE_PANGLU_SUC);
//        statusCam2List.add(STATUS_LIVE_CHECK_LIVE);
    }

    public static String getCam1Status() {
        if (statusCam1List != null) {
            statusCam1List.clear();
        }
        setupStatusCam1();
        for (int i = 0; i < statusCam1List.size(); i++) {
            if (!statusCam1List.get(0)) {
                return "cam1 获取wawaji-server ip失败";
            } else if (!statusCam1List.get(1)) {
                return "cam1 wawaji-server auth失败";
            } else if (!statusCam1List.get(2)) {
                return "cam1 获取腾讯账号密码失败";
            } else if (!statusCam1List.get(3)) {
                return "cam1 腾讯登录失败";
            } else if (!statusCam1List.get(4)) {
                return "cam1 腾讯创建房间失败";
            }
//            else if (!statusCam1List.get(5)) {
//                return "cam1 腾讯旁路直播推送失败";
//            }
//            else if (!statusCam1List.get(6)) {
//                return "cam1 还没检测腾讯流或者流有问题";
//            }
        }
        return "cam1 all ok";
    }

    public static String getCam2Status() {
        if (statusCam2List != null) {
            statusCam2List.clear();
        }
        setupStatusCam2();
        for (int i = 0; i < statusCam2List.size(); i++) {
            if (!statusCam2List.get(0)) {
                return "cam2 未从主盒子获取到腾讯账号密码";
            } else if (!statusCam2List.get(1)) {
                return "cam2 腾讯登录失败";
            } else if (!statusCam2List.get(2)) {
                return "cam2 腾讯加入房间失败";
            } else if (!statusCam2List.get(3)) {
                return "cam2 腾讯上麦失败";
            }
//            else if (!statusCam2List.get(4)) {
//                return "cam2 腾讯旁路直播推送失败";
//            }
//            else if (!statusCam2List.get(5)) {
//                return "cam2 还没检测腾讯流或者流有问题";
//            }
        }
        return "cam2 all ok";
    }

    public static void resetCheckStatusToUnlogin() {
        STATUS_LIVE_LOGIN_SUC_CAM1 = false;
        STATUS_LIVE_CREATE_ROOM_SUC_CAM1 = false;

//        STATUS_LIVE_PANGLU_SUC = false;
//        STATUS_LIVE_CHECK_LIVE = false;

        STATUS_LIVE_LOGIN_SUC_CAM2 = false;
        STATUS_LIVE_JOIN_ROOM_SUC_CAM2 = false;
        STATUS_LIVE_UP_TO_VIDEO_SUC_CAM2 = false;
    }
}
