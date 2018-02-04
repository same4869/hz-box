package renren.wawabox.manager.mind;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import renren.wawabox.config.Constants;
import renren.wawabox.utils.LogUtil;
import renren.wawabox.utils.ServerUtil;
import renren.wawabox.utils.TcpUtil;

/**
 * 和wawaji-server保持的长连接消息发送与接收
 * Created by xunwang on 2017/9/18.
 */

public class ServerStreamHandler {

    private static final int MSG_SEND_TCP = 10001;
    private static final int MSG_SEND_BEAT = 10002;
    private static int HEAD_SIZE = 8;

    private byte[] mHeadBuffer;
    private byte[] mReadBuffer;
    private String mReadResult;
    private int mCommandType;
    private byte[] mData;

    private RWListener mRWListenr = null;
    private Handler mHandler;
    private DataInputStream mIs;
    private OutputStream mOs;

    public interface RWListener {
        void onBeatMsgWriteFail();
    }

    public ServerStreamHandler(RWListener listener) {
        mHeadBuffer = new byte[HEAD_SIZE];
        mReadBuffer = new byte[1024 * 1024];
        mRWListenr = listener;

        HandlerThread mWriteThread = new HandlerThread("serverstream");
        mWriteThread.start();
        mHandler = new Handler(mWriteThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                int type = msg.arg1;
//                LogUtil.d(Constants.TAG, "ServerStreamHandler handleMessage (String) msg.obj --> " + (String) msg.obj + " type --> " + type + " what --> " + what, LogUtil.LOG_FILE_NAME_MIND);

                if (what == MSG_SEND_TCP) {
                    writeOneMessage((byte[]) msg.obj, type);
                } else if (what == MSG_SEND_BEAT) {
                    boolean ret = writeOneMessageInPb(ServerUtil.getBeatMsgInPb(), ServerUtil.HEART_BEAT_REQ);
                    if (!ret && mRWListenr != null) {
                        mRWListenr.onBeatMsgWriteFail();
                    }
                }
            }
        };
    }

    public void updateStream(InputStream is, OutputStream os) {
        mIs = new DataInputStream(is);
        mOs = os;
    }

    public void sendMsg(byte[] msg, int serverType) {
        Message message = Message.obtain();
        message.what = MSG_SEND_TCP;
        message.arg1 = serverType;
        message.obj = msg;
        mHandler.sendMessage(message);
    }

    public boolean sendBeatMsg() {
        if (mIs != null && mOs != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SEND_BEAT));
            return true;
        } else {
            return false;
        }
    }

    public String getReadResult() {
        return mReadResult;
    }

    public int getCommandType() {
        return mCommandType;
    }

    public byte[] getReadData(){
        return mData;
    }

    public boolean readMsg() {
        try {
            mIs.readFully(mReadBuffer, 0, HEAD_SIZE);
            int length = TcpUtil.byteArrayToInt(mReadBuffer, 0);

            mCommandType = ServerUtil.getCommandType(mReadBuffer);

            mIs.readFully(mReadBuffer, HEAD_SIZE, length);

            byte[] data = new byte[length];
            System.arraycopy(mReadBuffer, HEAD_SIZE, data, 0, length);

            mData = data;

            String msg = new String(mReadBuffer, HEAD_SIZE, length, "utf-8");
            LogUtil.d(Constants.TAG, "readMsg --> msg --> " + msg, LogUtil.LOG_FILE_NAME_MIND);
            mReadResult = msg;
            return true;
        } catch (IOException e) {
            LogUtil.e(Constants.TAG, "server socket read io " + e.getMessage(), LogUtil.LOG_FILE_NAME_MIND);
        }
        return false;
    }

    private boolean writeOneMessageInPb(byte[] msg, int serverType) {
        LogUtil.d(Constants.TAG, "writeOneMessageInPb msg --> " + msg + " serverType --> " + serverType, LogUtil.LOG_FILE_NAME_MIND);
        return writeOneMessage(msg, 0, msg.length, serverType);
    }

    private boolean writeOneMessage(byte[] msg, int serverType) {
        byte[] data = msg;
        return writeOneMessage(data, 0, data.length, serverType);
    }


    private boolean writeOneMessage(byte[] buffer, int bufferStart, int bufferSize, int serverType) {
        TcpUtil.intToByteArray(bufferSize, mHeadBuffer, 0, serverType);
        try {
            if (mOs != null) {
                mOs.write(mHeadBuffer);
                if (buffer != null) {
                    LogUtil.d(Constants.TAG, "writeOneMessage buffer --> " + new String(buffer), LogUtil.LOG_FILE_NAME_MIND);

                    mOs.write(buffer, bufferStart, bufferSize);
                }
                return true;
            }
        } catch (Exception e) {
            LogUtil.e(Constants.TAG, "writeOneMessage server write error --> " + e.getMessage(), LogUtil.LOG_FILE_NAME_MIND);
            return false;
        }
        return false;
    }

}
