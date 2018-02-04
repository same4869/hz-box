package renren.wawabox.utils;

/**
 * Created by xunwang on 2017/9/19.
 */

public class TcpUtil {
    /**
     * @param b
     * @param start
     * @return
     */
    public static int byteArrayToInt(byte[] b, int start) {
        return b[start + 7] & 0xFF |
                (b[start + 6] & 0xFF) << 8 |
                (b[start + 5] & 0xFF) << 16 |
                (b[start + 4] & 0xFF) << 24;
    }

    /**
     * @param a
     * @param buffer
     * @param start
     * @param serverType
     */
    public static void intToByteArray(int a, byte[] buffer, int start, int serverType) {
        buffer[start] = (byte) 0;
        buffer[start + 1] = (byte) 0;
        buffer[start + 2] = (byte) 0;
        if (serverType == ServerUtil.ACK) {
            buffer[start + 3] = (byte) 0;
        } else if (serverType == ServerUtil.AUTH_REQ) {
            buffer[start + 3] = (byte) 1;
        } else if (serverType == ServerUtil.AUTH_RSP) {
            buffer[start + 3] = (byte) 2;
        } else if (serverType == ServerUtil.HEART_BEAT_REQ) {
            buffer[start + 3] = (byte) 3;
        } else if (serverType == ServerUtil.HEART_BEAT_RSP) {
            buffer[start + 3] = (byte) 4;
        } else if (serverType == ServerUtil.NOTICE_SERVER_REQ) {
            buffer[start + 3] = (byte) 5;
        } else if (serverType == ServerUtil.NOTICE_SERVER_RSP) {
            buffer[start + 3] = (byte) 6;
        } else if (serverType == ServerUtil.NOTICE_APP_REQ) {
            buffer[start + 3] = (byte) 7;
        } else if (serverType == ServerUtil.NOTICE_APP_RSP) {
            buffer[start + 3] = (byte) 8;
        }
        buffer[start + 4] = (byte) ((a >> 24) & 0xFF);
        buffer[start + 5] = (byte) ((a >> 16) & 0xFF);
        buffer[start + 6] = (byte) ((a >> 8) & 0xFF);
        buffer[start + 7] = (byte) (a & 0xFF);
    }
}
