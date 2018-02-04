package renren.wawabox.manager.live;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.Socket;

import renren.wawabox.utils.PropertyUtil;
import renren.wawabox.utils.StreamLogUtil;

/**
 * websocet 推流
 * Created by xunwang on 2017/9/19.
 */

public class FastLiveManager {
    private static final int BEAT_MSG_GAP = 5000;
    private static final int FORMAT_JPEG = 1;

    private static final String TAG = "FastLiveManager";

    private static final String SERVER_IP = "111.231.11.236";
    private static final int SERVER_PORT = 8889;

    private Socket mServerSocket = null;
    private Thread mServerThread;

    private static final int FLAG_CAM1 = 1;
    private static final int FLAG_CAM2 = 2;

    private long mLastBeatTime = 0;

    private boolean mIsAuthed = false;
    private boolean mIsAuthSuccess = false;
    private boolean mIsMetaed = false;

    private boolean mNeedSend = false;
    private int mFrameKeepCycle = 0;
    private int mFrameSkipCount = 0;

    private boolean mIsConnecting = false;

    private FastLiveManager() {

    }

    private static FastLiveManager sInstance = new FastLiveManager();


    public static FastLiveManager getInstance() {
        return sInstance;
    }


    public boolean sendFrame(byte[] data, int width, int height) {
        if (unable()) {
            return false;
        }
        boolean sent = doSendFrame(data, width, height);

        if (!sent && mHandler != null) {
            // 发送出错
            // 这样可以确保每一个写操作都必须成功
            onDisconnect();
        }
        return sent;
    }

    private boolean doSendFrame(byte[] data, int width, int height) {
        // 写只发生在这个线程

        if (mHandler == null) {
            // 没连上
            // 重连
            if (!mIsConnecting) {
                connect();
            }
            return true;
        }
        if (!mIsAuthed) {
            return sendAuth();
        }
        if (!mIsAuthSuccess) {
            // 等待认证成功
            return true;
        }
        if (!mIsMetaed) {
            // 发送meta
            return sendMetaMsg(width, height);
        }

        // 待发上行命令;
        sendRecordStart();
        sendRecordStop();

        if (mNeedSend) {
            if (mFrameSkipCount >= mFrameKeepCycle) {
                mFrameSkipCount = 0;
                return writeImageToBuffer(data, width, height);
            } else {
                mFrameSkipCount++;
                return true;
            }
            // send
        } else {
            if (System.currentTimeMillis() - mLastBeatTime < BEAT_MSG_GAP) {
                // do nothing
                return true;
            } else {
                mLastBeatTime = System.currentTimeMillis();
                return sendKeepAlive();
            }
        }
    }

    private FastLiveStreamHandler mHandler;

    public synchronized void connect() {
        if (unable())
            return;

        Log.i(TAG, "connect fast stream manager");
        mIsConnecting = true;
        mNeedSend = false;
        if (mServerThread == null || !mServerThread.isAlive()) {
            mServerThread = new Thread() {
                @Override
                public void run() {
                    try {
                        mServerSocket = new Socket(SERVER_IP, SERVER_PORT);
                        mServerSocket.setKeepAlive(true);
                        mHandler = new FastLiveStreamHandler(mServerSocket.getInputStream(), mServerSocket.getOutputStream());


                        onConnect();

                        // 读只发生在这个线程
                        while (mHandler.readOneMessageInToBuffer()) {
                            switch (mHandler.readResultType) {
                                case FastLiveStreamHandler.TYPE_DOWN_OP:
                                    JsonObject json = mHandler.readResultJson;
                                    Log.d(TAG, "read cmd  " + json.toString());
                                    String cmd = json.get("cmd").getAsString();
                                    if ("start".equals(cmd)) {
                                        mNeedSend = true;
                                        mFrameKeepCycle = json.get("frameKeepCycle").getAsInt();
                                        mFrameSkipCount = 0;
                                    } else if ("stop".equals(cmd)) {
                                        mNeedSend = false;
                                    }
                                    break;
                                case FastLiveStreamHandler.TYPE_DOWN_AUTH_PASS:
                                    mIsAuthSuccess = true;
                                    break;
                            }
                        }
                        Log.e(TAG, "cannot read message");
                    } catch (IOException e) {
                        Log.e(TAG, "server socket io", e);
                    } finally {
                        onDisconnect();
                    }

                }
            };
            mServerThread.start();
        }
    }

    // 上来就给他1M
    private TransparentByteArrayOutputStream mBOS = new TransparentByteArrayOutputStream(1024 * 1024);
    private Rect mRect = new Rect(0, 0, 0, 0);

    private boolean writeImageToBuffer(byte[] inputYuvI420, int width, int height) {
        YuvImage yuvImage = new YuvImage(i420tonv21(inputYuvI420, width, height), ImageFormat.NV21, width, height, null);
        mBOS.reset();
        if (mRect.width() != width || mRect.height() != height) {
            mRect = new Rect(0, 0, width, height);
        }
        yuvImage.compressToJpeg(mRect, 60, mBOS);
        return mHandler.writeOneMessage(FastLiveStreamHandler.TYPE_UP_FRAME, mBOS.getBuffer(), 0, mBOS.getBufferSize());
    }


    private byte[] mPreviousNV21Buffer = new byte[0];

    private byte[] i420tonv21(byte[] data, int width, int height) {
        int ySize = width * height;


        if (mPreviousNV21Buffer.length != data.length) {
            mPreviousNV21Buffer = new byte[data.length];
        }

        System.arraycopy(data, 0, mPreviousNV21Buffer, 0, ySize);
        int nvInd = ySize;
        int iIndV = ySize;
        int iIndU = ySize * 5 / 4;

        while (nvInd < mPreviousNV21Buffer.length) {
            mPreviousNV21Buffer[nvInd] = data[iIndU];
            nvInd++;
            mPreviousNV21Buffer[nvInd] = data[iIndV];
            nvInd++;

            iIndU++;
            iIndV++;
        }
        return mPreviousNV21Buffer;
    }


    private synchronized void onDisconnect() {
        try {
            if (mServerSocket != null && mServerSocket.getInputStream() != null) {
                mServerSocket.getInputStream().close();
            }
        } catch (IOException e1) {
            Log.e(TAG, "server socket close int io", e1);
        }
        try {
            if (mServerSocket != null && mServerSocket.getOutputStream() != null) {
                mServerSocket.getOutputStream().close();
            }
        } catch (IOException e1) {
            Log.e(TAG, "server socket close out io", e1);
        }
        try {
            if (mServerSocket != null) {
                mServerSocket.close();
            }
        } catch (IOException e1) {
            Log.e(TAG, "server socket close io", e1);
        }
        mIsConnecting = false;
        mServerThread = null;
        mServerSocket = null;
        mHandler = null;
        mIsAuthed = false;
        mIsAuthSuccess = false;
        mIsMetaed = false;
        StreamLogUtil.putLog("disconnect fast stream server");
    }

    private void onConnect() {
        StreamLogUtil.putLog("connect to fast stream server succ");
    }


    private boolean sendAuth() {
        JsonObject json = new JsonObject();
        json.addProperty("auth", "alala");

        if (mHandler == null) {
            mIsAuthed = false;
        } else {
            mIsAuthed = mHandler.writeOneMessage(FastLiveStreamHandler.TYPE_UP_AUTH, json);
        }
        return mIsAuthed;
    }

    private boolean sendKeepAlive() {
        if (mHandler == null) {
            return false;
        }
        return mHandler.writeOneMessage(FastLiveStreamHandler.TYPE_UP_KEEPALIVE, null);
    }

    private static final Object mRecordLock = new Object();
    private JsonObject mWaitStartJson = null;
    private JsonObject mWaitEndJson = null;


    private boolean sendRecordStart() {
        if (mHandler == null || mWaitStartJson == null) {
            return false;
        }
        JsonObject start;
        synchronized (mRecordLock) {
            start = mWaitStartJson;
            mWaitStartJson = null;
        }
        return mHandler.writeOneMessage(FastLiveStreamHandler.TYPE_UP_RECORD_START, start);
    }

    private boolean sendRecordStop() {
        if (mHandler == null || mWaitEndJson == null) {
            return false;
        }
        JsonObject end;
        synchronized (mRecordLock) {
            end = mWaitEndJson;
            mWaitEndJson = null;
        }
        boolean ret = mHandler.writeOneMessage(FastLiveStreamHandler.TYPE_UP_RECORD_STOP, end);
        mWaitEndJson = null;
        return ret;
    }

    private boolean sendMetaMsg(int w, int h) {
        JsonObject json = new JsonObject();
        json.addProperty("width", w);
        json.addProperty("height", h);
        json.addProperty("format", FORMAT_JPEG);
        json.addProperty("camera", PropertyUtil.CAM2_ONLY ? FLAG_CAM2 : FLAG_CAM1);
        json.addProperty("channel", PropertyUtil.TOKEN);


        Log.d(TAG, "send meta " + w + ", " + h);

        if (mHandler == null) {
            mIsMetaed = false;
        } else {
            mIsMetaed = mHandler.writeOneMessage(FastLiveStreamHandler.TYPE_UP_META, json);
        }
        return mIsMetaed;
    }


    private boolean unable() {
        return !PropertyUtil.ENABLE_FAST_LIVE;
    }

}
