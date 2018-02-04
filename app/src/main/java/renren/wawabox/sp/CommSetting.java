package renren.wawabox.sp;

/**
 * Created by xunwang on 2017/8/29.
 */

public class CommSetting {
    public static final String SETTING = "setting";
    private static final String SP_KEY_MACHINE_ID = "sp_key_machine_id";
    private static final String SP_KEY_ROOM_ID = "sp_key_room_id";
    private static final String P_CAM2_ONLY = "p_cam2_only";
    private static final String SP_KEY_BACK_FRONT = "back_front";
    private static final String SP_KEY_INIT_LOC = "init_loc";
    private static final String SP_KEY_SENSOR_1_SETTLED = "sensor_1";
    private static final String SP_KEY_SENSOR_2_SETTLED = "sensor_2";
    private static final String SP_KEY_DOWN_DOWN = "down_down";
    private static final String SP_KEY_SANDBOX = "sandbox";
    private static final String SP_KEY_TOKEN = "token";

    /**
     * sensor 1 reverse :  1 = 没有, 0 = 有
     */
    private static final String SP_KEY_SENSOR_1_REVERSE = "sensor_1_reverse";
    /**
     * sensor 2 reverse :  1 = 没有, 0 = 有
     */
    private static final String SP_KEY_SENSOR_2_REVERSE = "sensor_2_reverse";
    private static final String SP_KEY_ENABLE_FAST_LIVE = "enable_fast_live";

    private static final String CURRENT_GROUP_ID = "current_group_id";

    public static void clearKey(String key){
        PrefsMgr.removeValue(SETTING , key);
    }

    public static String getGroupId() {
        return PrefsMgr.getString(SETTING, CURRENT_GROUP_ID, null);
    }

    public static void setGroupId(String groupId) {
        PrefsMgr.putString(SETTING, CURRENT_GROUP_ID, groupId);
    }

    public static String getToken() {
        return PrefsMgr.getString(SETTING, SP_KEY_TOKEN, null);
    }

    public static void setToken(String token) {
        PrefsMgr.putString(SETTING, SP_KEY_TOKEN, token);
    }

    public static boolean getEnableFastLive() {
        return PrefsMgr.getBoolean(SETTING, SP_KEY_ENABLE_FAST_LIVE, false);
    }

    public static void setEnableFastLive(boolean enableFastLive) {
        PrefsMgr.putBoolean(SETTING, SP_KEY_ENABLE_FAST_LIVE, enableFastLive);
    }

    public static boolean getSensor2Reverse() {
        return PrefsMgr.getBoolean(SETTING, SP_KEY_SENSOR_2_REVERSE, false);
    }

    public static void setSensor2Reverse(boolean sensor2Reverse) {
        PrefsMgr.putBoolean(SETTING, SP_KEY_SENSOR_2_REVERSE, sensor2Reverse);
    }

    public static boolean getSensor1Reverse() {
        return PrefsMgr.getBoolean(SETTING, SP_KEY_SENSOR_1_REVERSE, false);
    }

    public static void setSensor1Reverse(boolean sensor1Reverse) {
        PrefsMgr.putBoolean(SETTING, SP_KEY_SENSOR_1_REVERSE, sensor1Reverse);
    }

    public static boolean getSandBox() {
        return PrefsMgr.getBoolean(SETTING, SP_KEY_SANDBOX, false);
    }

    public static void setSandBox(boolean sandBox) {
        PrefsMgr.putBoolean(SETTING, SP_KEY_SANDBOX, sandBox);
    }

    public static boolean getDownDown() {
        return PrefsMgr.getBoolean(SETTING, SP_KEY_DOWN_DOWN, false);
    }

    public static void setDownDown(boolean downDown) {
        PrefsMgr.putBoolean(SETTING, SP_KEY_DOWN_DOWN, downDown);
    }

    public static boolean getSensor2Settled() {
        return PrefsMgr.getBoolean(SETTING, SP_KEY_SENSOR_2_SETTLED, false);
    }

    public static void setSensor2Settled(boolean sensor2Settled) {
        PrefsMgr.putBoolean(SETTING, SP_KEY_SENSOR_2_SETTLED, sensor2Settled);
    }

    public static boolean getSensor1Settled() {
        return PrefsMgr.getBoolean(SETTING, SP_KEY_SENSOR_1_SETTLED, false);
    }

    public static void setSensor1Settled(boolean sensor1Settled) {
        PrefsMgr.putBoolean(SETTING, SP_KEY_SENSOR_1_SETTLED, sensor1Settled);
    }

    public static int getInitLoc() {
        return PrefsMgr.getInt(SETTING, SP_KEY_INIT_LOC, 0);
    }

    public static void setInitLoc(int backFront) {
        PrefsMgr.putInt(SETTING, SP_KEY_INIT_LOC, backFront);
    }

    public static int getBackFront() {
        return PrefsMgr.getInt(SETTING, SP_KEY_BACK_FRONT, 1);
    }

    public static void setBackFront(int backFront) {
        PrefsMgr.putInt(SETTING, SP_KEY_BACK_FRONT, backFront);
    }

    public static boolean getCam2Only() {
        return PrefsMgr.getBoolean(SETTING, P_CAM2_ONLY, false);
    }

    public static void setCam2Only(boolean cam2Only) {
        PrefsMgr.putBoolean(SETTING, P_CAM2_ONLY, cam2Only);
    }

    public static int getMachineId() {
        return PrefsMgr.getInt(SETTING, SP_KEY_MACHINE_ID, -1);
    }

    public static void setMachineId(int machineId) {
        PrefsMgr.putInt(SETTING, SP_KEY_MACHINE_ID, machineId);
    }

    public static int getRoomId() {
        return PrefsMgr.getInt(SETTING, SP_KEY_ROOM_ID, -1);
    }

    public static void setRoomId(int roomId) {
        PrefsMgr.putInt(SETTING, SP_KEY_ROOM_ID, roomId);
    }

}
