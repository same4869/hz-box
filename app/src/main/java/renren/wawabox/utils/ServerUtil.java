package renren.wawabox.utils;

import renren.wawabox.manager.mind.MindControllerManager;
import renren.wawabox.sp.CommSetting;
import wawa.protocol.Wawaji;

/**
 * Created by xunwang on 2017/9/19.
 */

public class ServerUtil {
    public static int ACK = 0X0000;
    public static int AUTH_REQ = 0x0001;
    public static int AUTH_RSP = 0x0002;
    public static int HEART_BEAT_REQ = 0x0003;
    public static int HEART_BEAT_RSP = 0x0004;
    public static int NOTICE_SERVER_REQ = 0x0005; //app抓取结果通知娃娃机
    public static int NOTICE_SERVER_RSP = 0x0006;
    public static int NOTICE_APP_REQ = 0x0007;//娃娃机通知app
    public static int NOTICE_APP_RSP = 0X0008;

    /**
     * 后台新架构中根据前4位来判断命令类型
     *
     * @param header
     * @return
     */
    public static int getCommandType(byte[] header) {
        if (header[3] == 0) {
            return ACK;
        } else if (header[3] == 1) {
            return AUTH_REQ;
        } else if (header[3] == 2) {
            return AUTH_RSP;
        } else if (header[3] == 3) {
            return HEART_BEAT_REQ;
        } else if (header[3] == 4) {
            return HEART_BEAT_RSP;
        } else if (header[3] == 5) {
            return NOTICE_SERVER_REQ;
        } else if (header[3] == 6) {
            return NOTICE_SERVER_RSP;
        } else if (header[3] == 7) {
            return NOTICE_APP_REQ;
        } else if (header[3] == 8) {
            return NOTICE_APP_RSP;
        }
        return 0;
    }

    public static byte[] getBeatMsgInPb() {
        int id = CommSetting.getMachineId();
        LogUtil.d(MindControllerManager.TAG, "getBeatMsgInPb id --> " + id, LogUtil.LOG_FILE_NAME_MIND);
        Wawaji.HeartbeatReq heartbeatReq = Wawaji.HeartbeatReq.newBuilder()
                .setWwjID(id).build();
        return heartbeatReq.toByteArray();
    }

    public static byte[] getAuthMsgInPb() {
        int id = CommSetting.getMachineId();
        LogUtil.d(MindControllerManager.TAG, "getAuthMsgInPb id --> " + id);
        Wawaji.AuthReq authReq = Wawaji.AuthReq.newBuilder()
                .setToken(PropertyUtil.TOKEN).setWwjID(id).build();
        return authReq.toByteArray();
    }

    public static byte[] getTakeResultMsgInPb(boolean success) {
        int id = CommSetting.getMachineId();
        String state;
        if (!success) {
            state = "1";//"crawl.failed";
        } else {
            state = "0";//"crawl.succeed";
        }
        LogUtil.d(MindControllerManager.TAG, "getTakeResultMsgInPb id --> " + id + " state --> " + state, LogUtil.LOG_FILE_NAME_MIND);
        Wawaji.NoticeServerReq noticeServerReq = Wawaji.NoticeServerReq.newBuilder()
                .setData(state).setWwjID(id).build();
        return noticeServerReq.toByteArray();
    }
}
