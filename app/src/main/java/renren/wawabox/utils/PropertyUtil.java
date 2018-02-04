package renren.wawabox.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import renren.wawabox.app.WawaNewApplication;
import renren.wawabox.config.Constants;
import renren.wawabox.config.MachineProperties;
import renren.wawabox.config.MqttConstants;
import renren.wawabox.manager.mind.MindControllerManager;
import renren.wawabox.sp.CommSetting;

/**
 * 读取assets下面的配置，并保存在sp中
 * Created by xunwang on 2017/9/13.
 */

public class PropertyUtil {
    private static final String PROPERTY_FILE_NAME = "wawa.properties";

    public static String MQTT_SERVER_IP = "mqtt_ip";
    public static int MQTT_SERVER_PORT = 1883;
    public static boolean WAIT_ID = false;
    public static String MQTT_CONNECTION_ID = "mqtt_id";
    public static boolean CAM2_ONLY = false;
    public static int ROOM_NUM;
    public static String TOKEN = "ma:0";
    public static int MQTT_CLAW_MACHINE_ID;
    public static boolean SENSOR_1_SETTERED = false;
    public static boolean SENSOR_2_SETTERED = false;
    public static boolean RELEASE_DOWN_DOWN = false;
    public static boolean SENSOR_1_REVERSE = false;
    public static boolean SENSOR_2_REVERSE = false;
    public static boolean SANDBOX = false;
    public static boolean ENABLE_FAST_LIVE = false;

    private static final String P_MQTT_SERVER_IP = "mqtt_ip";
    private static final String P_MQTT_SERVER_PORT = "mqtt_port";
    private static final String P_MQTT_CONNECTION_ID = "mqtt_connection_id";
    private static final String P_MQTT_CLAW_MACHINDE_ID = "mqtt_machine_id";
    private static final String P_WAIT_ID = "wait_id";
    private static final String P_CAM2_ONLY = "cam2_only";
    private static final String P_ROOM_NUM = "room";
    private static final String P_TOKEN = "token";
    private static final String P_MACHINE_INIT_LOC = "mac_init_loc";
    private static final String P_MACHINE_BACK_FRONT = "mqtt_back_front";


    public static void init() {
        Properties properties = new Properties();
        InputStream is;

        try {
            is = WawaNewApplication.getInstance().getApplicationContext().getAssets().open(PROPERTY_FILE_NAME);
            properties.load(is);

            MQTT_SERVER_IP = properties.getProperty(P_MQTT_SERVER_IP);
            MQTT_SERVER_PORT = Integer.valueOf(properties.getProperty(P_MQTT_SERVER_PORT));
            WAIT_ID = Boolean.parseBoolean(properties.getProperty(P_WAIT_ID));

            if (WAIT_ID) {
                MQTT_CONNECTION_ID = "andr_board:" + NetworkKeeperUtil.getMacAddress();
                StreamLogUtil.putLog("mqtt_connection_id = " + MQTT_CONNECTION_ID);

                readSp();
            } else {
                CAM2_ONLY = Boolean.parseBoolean(properties.getProperty(P_CAM2_ONLY));
                ROOM_NUM = Integer.valueOf(properties.getProperty(P_ROOM_NUM, "2"));
                TOKEN = properties.getProperty(P_TOKEN, "ma:1");
                MQTT_CONNECTION_ID = properties.getProperty(P_MQTT_CONNECTION_ID);
                MQTT_CLAW_MACHINE_ID = Integer.valueOf(properties.getProperty(P_MQTT_CLAW_MACHINDE_ID));

                MachineProperties.MACHINE_INIT_LOC = Integer.valueOf(properties.getProperty(P_MACHINE_INIT_LOC));
                MachineProperties.MACHINE_BACK_FRONT = Integer.valueOf(properties.getProperty(P_MACHINE_BACK_FRONT));

                LogUtil.d(Constants.TAG, String.format("Properties mqtt: %s : %s, client = %s, to machine = %s", MQTT_SERVER_IP, MQTT_SERVER_PORT, MQTT_CONNECTION_ID, MQTT_CLAW_MACHINE_ID));

            }
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                LogUtil.d(Constants.TAG, e.getMessage());
            }
        } catch (FileNotFoundException e) {
            LogUtil.e(Constants.TAG, e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            LogUtil.e(Constants.TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean readSp() {
        boolean setted;

        int id = CommSetting.getMachineId();
        int room = CommSetting.getRoomId();
        if (room < 0) {
            room = id;
        }

        if (id > 0) {
            CAM2_ONLY = CommSetting.getCam2Only();
            ROOM_NUM = room;
            MQTT_CLAW_MACHINE_ID = id;

            MachineProperties.MACHINE_BACK_FRONT = CommSetting.getBackFront();
            MachineProperties.MACHINE_INIT_LOC = CommSetting.getInitLoc();

            SENSOR_1_SETTERED = CommSetting.getSensor1Settled();
            SENSOR_2_SETTERED = CommSetting.getSensor2Settled();

            RELEASE_DOWN_DOWN = CommSetting.getDownDown();

            SENSOR_1_REVERSE = CommSetting.getSensor1Reverse();
            MqttConstants.SENSOR_1_GOT = SENSOR_1_REVERSE ? 0 : 1;

            SENSOR_2_REVERSE = CommSetting.getSensor2Reverse();
            MqttConstants.SENSOR_2_GOT = SENSOR_2_REVERSE ? 0 : 1;


            SANDBOX = CommSetting.getSandBox();
            ENABLE_FAST_LIVE = CommSetting.getEnableFastLive();

//            TOKEN = (SANDBOX ? "test_ma:" : "ma:") + id;
            TOKEN = CommSetting.getToken();//"ma:6";//

            // id 已经有了,不需要了
            WAIT_ID = false;
            setted = true;
            StreamLogUtil.putLog("read sp id = " + id + ", room " + room + ", cam2 = " + CAM2_ONLY + ", bf = " + MachineProperties.MACHINE_BACK_FRONT);
        } else {
            // 等id配置;
            WAIT_ID = true;
            setted = false;
        }
        return setted;
    }

    public static void writeDone() {
        readSp();
        //开始工作,配置结束
        MindControllerManager.getInstance().start();
    }

}
