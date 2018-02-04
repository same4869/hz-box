package renren.wawabox.manager.live;

import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import renren.wawabox.json.JSONFormatExcetion;
import renren.wawabox.json.JSONToBeanHandler;
import renren.wawabox.utils.ServerUtil;
import renren.wawabox.utils.TcpUtil;

/**
 * Created by xunwang on 2017/9/19.
 */

public class FastLiveStreamHandler {
    private static final String TAG = FastLiveStreamHandler.class.getSimpleName();
    public static final int TYPE_UP_AUTH = 1;
    public static final int TYPE_UP_META = 2;
    public static final int TYPE_UP_FRAME = 3;
    public static final int TYPE_UP_KEEPALIVE = 4;
    public static final int TYPE_UP_RECORD_START = 5;
    public static final int TYPE_UP_RECORD_STOP = 6;
    public static final int TYPE_DOWN_OP = 64;
    public static final int TYPE_DOWN_AUTH_PASS = 128;

    // timestamp, type, size
    private static final int MESSAGE_HEADER_POSITION_TIMESTAMP = 0;
    private static final int MESSAGE_HEADER_POSITION_TYPE = MESSAGE_HEADER_POSITION_TIMESTAMP + 4;
    private static final int MESSAGE_HEADER_POSITION_LENGTH = MESSAGE_HEADER_POSITION_TYPE + 4;
    private static final int MESSAGE_HEADER_SIZE = MESSAGE_HEADER_POSITION_LENGTH + 8;

    private static final int MAX_MESSAGE_SIZE = 1024 * 1024;

    public byte[] readResultBuffer = new byte[MAX_MESSAGE_SIZE];
    public int readResultTimestamp;
    public int readResultType;
    public int readResultBytesLength;
    public JsonObject readResultJson;

    private DataInputStream is;
    private OutputStream os;

    public FastLiveStreamHandler(InputStream is, OutputStream os) {
        this.is = new DataInputStream(is);
        this.os = os;
    }

    public boolean readOneMessageInToBuffer() {
        // timestamp
        try {
            this.is.readFully(readResultBuffer, 0, MESSAGE_HEADER_SIZE);
            readResultTimestamp = TcpUtil.byteArrayToInt(readResultBuffer, MESSAGE_HEADER_POSITION_TIMESTAMP);
            readResultType = TcpUtil.byteArrayToInt(readResultBuffer, MESSAGE_HEADER_POSITION_TYPE);
            readResultBytesLength = TcpUtil.byteArrayToInt(readResultBuffer, MESSAGE_HEADER_POSITION_LENGTH);
            if (readResultBytesLength > 0) {
                this.is.readFully(readResultBuffer, MESSAGE_HEADER_SIZE, readResultBytesLength);
                readResultJson = new JsonParser().parse(new String(readResultBuffer, MESSAGE_HEADER_SIZE, readResultBytesLength)).getAsJsonObject();
            }
            // 读完整了
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean writeOneMessage(int type, JsonObject data) {
        try {
            byte[] payload = JSONToBeanHandler.toJsonString(data).getBytes();
            return writeOneMessage(type, payload, 0, payload.length);
        } catch (JSONFormatExcetion jsonFormatExcetion) {
            jsonFormatExcetion.printStackTrace();
        }
        return false;
    }

    private byte[] writeHeaderBuffer = new byte[MESSAGE_HEADER_SIZE];
    private long metaTimestamp = 0;

    public boolean writeOneMessage(int type, byte[] buffer, int bufferStart, int bufferSize) {
        int timeStamp = 0;
        if (type == TYPE_UP_META) {
            metaTimestamp = System.currentTimeMillis();
        } else if (type == TYPE_UP_FRAME) {
            timeStamp = (int) (System.currentTimeMillis() - metaTimestamp);
        }
        TcpUtil.intToByteArray(timeStamp, writeHeaderBuffer, MESSAGE_HEADER_POSITION_TIMESTAMP, ServerUtil.ACK);
        TcpUtil.intToByteArray(type, writeHeaderBuffer, MESSAGE_HEADER_POSITION_TYPE, ServerUtil.ACK);
        TcpUtil.intToByteArray(bufferSize, writeHeaderBuffer, MESSAGE_HEADER_POSITION_LENGTH, ServerUtil.ACK);
        try {
            this.os.write(writeHeaderBuffer);
            if (buffer != null) {
                this.os.write(buffer, bufferStart, bufferSize);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "write error", e);
            return false;
        }
    }
}
