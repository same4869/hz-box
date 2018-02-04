package renren.wawabox.manager.mind;

import android.content.Context;
import android.text.TextUtils;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tencent.TIMManager;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveSDK;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

import commlib.xun.com.commlib.thread.CommThreadPool;
import renren.wawabox.app.WawaNewApplication;
import renren.wawabox.bean.LiveSignatureBean;
import renren.wawabox.bean.ServerIpBean;
import renren.wawabox.config.Constants;
import renren.wawabox.config.MqttConstants;
import renren.wawabox.json.JSONFormatExcetion;
import renren.wawabox.json.JSONToBeanHandler;
import renren.wawabox.manager.live.TencentCam2Liver;
import renren.wawabox.manager.live.TencentLiveManager;
import renren.wawabox.manager.mqtt.MqttManager;
import renren.wawabox.net.IRequestCallback;
import renren.wawabox.net.RequestFactory;
import renren.wawabox.utils.AppUtil;
import renren.wawabox.utils.ConvertUtil;
import renren.wawabox.utils.LogUtil;
import renren.wawabox.utils.PropertyUtil;
import renren.wawabox.utils.ServerUtil;
import renren.wawabox.utils.StreamLogUtil;
import wawa.protocol.Wawaji;

/**
 * 与服务器保持的长连接
 * Created by xunwang on 2017/9/18.
 */

public class MindControllerManager {
    public static final String TAG = "MindControllerManager";
    public static final String REQUEST_URL = Constants.BASE_URL + "room/live-signature";

    private static String STREAM_LIVE_URL = null;
    private static String STREAM_CAM2_LIVE_URL = null;

    private ServerStreamHandler sServerRWHandler = null;
    private Socket sServerSocket = null;
    private static MindControllerManager sInstance;

    private boolean mIsAuth = false;
    private int mBeatCount = 19;
    private boolean mIsStart = false;

    private static String SLAVE;
    private static String curId, curSig;//主盒子
    private static String curSid, curSsig;//副盒子
    private int mWaitBeatRespNum = 0;

    public static String getSlaveMsg() {
        LogUtil.d(TAG, "SLAVE --> " + SLAVE, LogUtil.LOG_FILE_NAME_MIND);
        return SLAVE;
    }


    public static void initInstance() {
        LogUtil.d(TAG, "init", LogUtil.LOG_FILE_NAME_MIND);
        sInstance = new MindControllerManager();
    }

    public static MindControllerManager getInstance() {
        return sInstance;
    }


    private MindControllerManager() {
        sServerRWHandler = new ServerStreamHandler(new ServerStreamHandler.RWListener() {
            @Override
            public void onBeatMsgWriteFail() {
                // 重连
                onDisconnect();
                connect();
                LogUtil.e(TAG, "onBeatMsgWriteFail reconnect", LogUtil.LOG_FILE_NAME_MIND);
            }
        });
    }

    public static void init(Context context) {

        // 初始化 mqtt
        MqttManager.init(context, new MqttManager.ResultListener() {
            @Override
            public void onGetResult(boolean success) {
                if (success) {//失败不告诉服务器，让服务器自己处理
                    sInstance.sendMsg2Server(ServerUtil.getTakeResultMsgInPb(success), ServerUtil.NOTICE_SERVER_REQ);
                }
            }
        });

        // 初始化各种连接
        if (MindControllerManager.getInstance() == null) {
            MindControllerManager.initInstance();
        }
    }

    public void connect() {
        resetBeatRespNum();
        if (!PropertyUtil.CAM2_ONLY) {
            // 作为第二个摄像头用
            // 不连接服务器
            requestServerIp();
        }
        MqttManager.getInstance().start();
    }

    private void requestServerIp() {
        StreamLogUtil.putLog("requestServerIp room_id --> " + PropertyUtil.ROOM_NUM + " token --> " + PropertyUtil.TOKEN);
        LogUtil.d(TAG, "requestServerIp room_id --> " + PropertyUtil.ROOM_NUM + " token --> " + PropertyUtil.TOKEN, LogUtil.LOG_FILE_NAME_MIND);
        HashMap<String, String> params = new HashMap<>();
        params.put("room_id", String.valueOf(PropertyUtil.ROOM_NUM));
        params.put("token", PropertyUtil.TOKEN);
        String requestUrl = Constants.BASE_URL + "wwj/server";
        RequestFactory.getRequestManager(RequestFactory.RequestType.OKHTTP).post(requestUrl, AppUtil.hashMapToJson(params), new IRequestCallback() {
            @Override
            public void onSuccess(String response) {
                StreamLogUtil.putLog("requestServerIp --> " + response);
                try {
                    ServerIpBean serverIpBean = JSONToBeanHandler.fromJsonString(response, ServerIpBean.class);
                    if (serverIpBean != null && serverIpBean.getData() != null && serverIpBean.getData().getSocketUrl() != null) {
                        MqttConstants.STATUS_MIND_REQUEST_SERVER_IP_CAM1 = true;
                        String[] ips = serverIpBean.getData().getSocketUrl().split(":");
                        if (ips.length > 1) {
                            connectServer(ips[0], ConvertUtil.convertToInt(ips[1], 9527));
                        }
                    }
                } catch (JSONFormatExcetion jsonFormatExcetion) {
                    jsonFormatExcetion.printStackTrace();
                }

            }

            @Override
            public void onFailure(Throwable throwable) {
                StreamLogUtil.putLog("获取wawaji-server ip失败");
            }
        });
    }

    private void connectServer(final String ip, final int port) {
        LogUtil.d(TAG, "connect wawaji-server", LogUtil.LOG_FILE_NAME_MIND);
        StreamLogUtil.putLog("connect wawaji-server");
        CommThreadPool.poolExecute(new Runnable() {
            @Override
            public void run() {
                try {
                    sServerSocket = new Socket(ip, port);
                    sServerSocket.setKeepAlive(true);
                    sServerRWHandler.updateStream(sServerSocket.getInputStream(), sServerSocket.getOutputStream());

                    LogUtil.d(TAG, "send auth on connect", LogUtil.LOG_FILE_NAME_MIND);
                    sendAuthMsg();
                    readServerSocket();

                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtil.e(TAG, "server socket io:" + e.getMessage(), LogUtil.LOG_FILE_NAME_MIND);
                }

                onDisconnect();
            }
        });
    }

    private synchronized void onDisconnect() {
        try {
            if (sServerSocket != null && sServerSocket.getInputStream() != null) {
                sServerSocket.getInputStream().close();
            }
        } catch (IOException e1) {
            LogUtil.e(TAG, "server socket close int io e1 --> " + e1.getMessage(), LogUtil.LOG_FILE_NAME_MIND);
        }
        try {
            if (sServerSocket != null && sServerSocket.getOutputStream() != null) {
                sServerSocket.getOutputStream().close();
            }
        } catch (IOException e1) {
            LogUtil.e(TAG, "server socket close out io e1 --> " + e1.getMessage(), LogUtil.LOG_FILE_NAME_MIND);
        }
        try {
            if (sServerSocket != null) {
                sServerSocket.close();
            }
        } catch (IOException e1) {
            LogUtil.e(TAG, "server socket close io e1 --> " + e1.getMessage());
        }

        sServerSocket = null;
        mBeatCount = 0;
        mIsAuth = false;
        sServerRWHandler.updateStream(null, null);

//        TencentLiveManager.quitRoomAndLogout();

        LogUtil.d(TAG, "disconnect server", LogUtil.LOG_FILE_NAME_MIND);
        StreamLogUtil.putLog("disconnect server");
    }

    public void sendAuthMsg() {
        LogUtil.d(TAG, "new String(ServerHelper.getAuthMsgInPb()) --> " + new String(ServerUtil.getAuthMsgInPb()), LogUtil.LOG_FILE_NAME_MIND);
        sendMsg2Server(ServerUtil.getAuthMsgInPb(), ServerUtil.AUTH_REQ);
    }

    public void sendMsg2Server(byte[] msg, int serverType) {
        LogUtil.d("kkkkkkkk", "SEND handleServerMsg sendMsg2Server msg --> " + msg + " serverType --> " + serverType, LogUtil.LOG_FILE_NAME_MIND);
        sServerRWHandler.sendMsg(msg, serverType);
    }

    /**
     * 死循环接收wawaji-server消息，除非断开
     */
    private void readServerSocket() {
        while (sServerRWHandler.readMsg()) {
            String msg = sServerRWHandler.getReadResult();
            int cmdType = sServerRWHandler.getCommandType();
            handleServerMsg(cmdType, sServerRWHandler.getReadData());
        }
    }

    private void handleServerMsg(int cmdType, byte[] msg) {
        LogUtil.d(TAG, " handleServerMsg:" + msg + " cmdType --> " + cmdType, LogUtil.LOG_FILE_NAME_MIND);
        if (cmdType == ServerUtil.AUTH_RSP) {//认证
            try {
                Wawaji.AuthRsp authRsp = Wawaji.AuthRsp.parseFrom(msg);
                LogUtil.d(TAG, "authRsp.getWwjID() --> " + authRsp.getWwjID() + " authRsp.getCode() --> " + authRsp.getCode() + " authRsp.getMsg() --> " + authRsp.getMsg(), LogUtil.LOG_FILE_NAME_MIND);
                if (authRsp.getCode() == 0) {
                    MqttConstants.STATUS_WAWAJI_SERVER_AUTH_CAM1 = true;
                    StreamLogUtil.putLog("wawaji-server auth成功，请求直播账号");
                    mIsAuth = true;
                    requestLiveSignature(PropertyUtil.TOKEN);
//                    MqttManager.getInstance().sendSettingTrigMsg(MqttManager.TIME_TRIP);
                }
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        } else if (cmdType == ServerUtil.NOTICE_APP_REQ) {//游戏中
            try {
                Wawaji.NoticeAppReq noticeAppReq = Wawaji.NoticeAppReq.parseFrom(msg);
                LogUtil.d(TAG, "noticeAppReq.getWwjID() --> " + noticeAppReq.getWwjID() + " noticeAppReq.getData() --> " + noticeAppReq.getData() + " noticeAppReq.getForce() --> " + noticeAppReq.getForce(), LogUtil.LOG_FILE_NAME_MIND);
                if (MqttManager.getInstance().isConnected()) {
                    MqttManager.getInstance().startWaitResult();
                    MqttManager.getInstance().handleNewControlMessage(noticeAppReq.getData(), noticeAppReq.getForce());
                } else {
                    StreamLogUtil.putLog("un handle msg:" + msg);
                    LogUtil.e(TAG, "un handle msg " + msg, LogUtil.LOG_FILE_NAME_MIND);
                }
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        } else if (cmdType == ServerUtil.HEART_BEAT_RSP) {
            resetBeatRespNum();
        }
    }

    private void requestLiveSignature(String token) {
        LogUtil.d(TAG, "requestLiveSignature token --> " + token, LogUtil.LOG_FILE_NAME_MIND);
        HashMap<String, String> params = new HashMap<>();
        params.put("token", token);
        RequestFactory.getRequestManager(RequestFactory.RequestType.OKHTTP).post(REQUEST_URL, AppUtil.hashMapToJson(params), new IRequestCallback() {
            @Override
            public void onSuccess(String response) {
                LogUtil.d(TAG, "requestLiveSignature response --> " + response, LogUtil.LOG_FILE_NAME_MIND);

                try {
                    LiveSignatureBean liveSignatureBean = JSONToBeanHandler.fromJsonString(response, LiveSignatureBean.class);
                    handleAuthMsg(liveSignatureBean, liveSignatureBean.getData().getLive_stream().getIdentifiers().getMaster().getIdentifier(), liveSignatureBean.getData().getLive_stream().getIdentifiers().getMaster().getSignature());

                } catch (JSONFormatExcetion jsonFormatExcetion) {
                    jsonFormatExcetion.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                LogUtil.d(TAG, "requestLiveSignature throwable.getMessage() --> " + throwable.getMessage(), LogUtil.LOG_FILE_NAME_MIND);
            }
        });
    }

    private void handleAuthMsg(LiveSignatureBean liveSignatureBean, final String id, final String sig) {
        if (liveSignatureBean == null) {
            return;
        }

        StreamLogUtil.putLog("获得直播账号密码 id --> " + id + ", sig --> " + sig);
        LogUtil.d(TAG, "auth ok, get account: " + id + ", sig = " + sig, LogUtil.LOG_FILE_NAME_MIND);
        LogUtil.d(TAG, "auth ok, get slave account: " + SLAVE);

        curSid = liveSignatureBean.getData().getLive_stream().getIdentifiers().getSlave01().getIdentifier();
        curSsig = liveSignatureBean.getData().getLive_stream().getIdentifiers().getSlave01().getSignature();

        try {
            SLAVE = JSONToBeanHandler.toJsonString(liveSignatureBean.getData().getLive_stream().getIdentifiers().getSlave01());
        } catch (JSONFormatExcetion jsonFormatExcetion) {
            jsonFormatExcetion.printStackTrace();
        }

        if (!PropertyUtil.CAM2_ONLY) {
            if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(sig)) {
                curId = id;
                curSig = sig;
                MqttConstants.STATUS_REQUEST_LIVE_ID_CAM1 = true;
                ILiveSDK.getInstance().runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        TencentLiveManager.login(id, sig, new ILiveCallBack() {
                            @Override
                            public void onSuccess(Object data) {
                                LogUtil.d(TAG, "TencentLiveCore cam1 login onSuccess", LogUtil.LOG_FILE_NAME_MIND);
                                StreamLogUtil.putLog("cam1 login success");
                                MqttConstants.STATUS_LIVE_LOGIN_SUC_CAM1 = true;
                                if (!PropertyUtil.CAM2_ONLY) {
                                    TencentLiveManager.createInnerRoom(WawaNewApplication.sActivity, PropertyUtil.ROOM_NUM);
                                }
                            }

                            @Override
                            public void onError(String module, int errCode, String errMsg) {
                                LogUtil.d(TAG, "TencentLiveCore login onError", LogUtil.LOG_FILE_NAME_MIND);
                                StreamLogUtil.putLog("cam1 login fail " + errCode + ", " + errMsg);
                            }
                        });
                    }
                }, 0);
            } else {
                StreamLogUtil.putLog("cam1 login fail: auth ok do not return valid id&sig");
            }
        }
    }

    public static String getCurStreamLiveUrl() {
        return PropertyUtil.CAM2_ONLY ? STREAM_CAM2_LIVE_URL : STREAM_LIVE_URL;
    }

    public static void setCurStreamLiveUrl(String url) {
        if (PropertyUtil.CAM2_ONLY) {
            STREAM_CAM2_LIVE_URL = url;
        } else {
            STREAM_LIVE_URL = url;
            if (!TextUtils.isEmpty(STREAM_LIVE_URL) && TextUtils.isEmpty(STREAM_CAM2_LIVE_URL)) {
                // 侧面摄像头没有,可能已经在推流了, 请求侧边url
                MqttManager.getInstance().publishRequestCam2LiveUrl();
            }
        }
    }

    public boolean checkMQTTConnect() {
        return MqttManager.getInstance().checkConnect();
    }

    private int checkUserTime = 20; //每1分钟检查一次登录状态

    public void beatCheck() {
        if (mIsStart) {
            checkServerConnect();
            boolean iscon = checkMQTTConnect();
            if (!iscon) {
                LogUtil.d(TAG, "beatCheck mqtt disconnect ", LogUtil.LOG_FILE_NAME_MIND);
            }
            checkUserTime--;
            if (checkUserTime <= 0) {
                ILiveSDK.getInstance().runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        checkUserTime = 20;
                        checkLoginStatus();
                    }
                }, 0);
            }
        } else {
            // do nothing
            LogUtil.d(TAG, "beat check do nothing, in setting", LogUtil.LOG_FILE_NAME_MIND);
        }
    }


    int i = 0;

    //如果检测出当前的登录用户为空，则重新登录
    public void checkLoginStatus() {
        String loginUser = TIMManager.getInstance().getLoginUser();
        StreamLogUtil.putLog("getLoginUser --> " + loginUser);
        LogUtil.d("kkkkkkkk", "checkLoginStatus() TIMManager.getInstance().getLoginUser() --> " + loginUser, LogUtil.LOG_FILE_NAME_LIVE);
        if (TextUtils.isEmpty(loginUser)) {
            MqttConstants.resetCheckStatusToUnlogin();
            if (!PropertyUtil.CAM2_ONLY) {
                if (!TextUtils.isEmpty(curId) && !TextUtils.isEmpty(curSig)) {
                    StreamLogUtil.putLog("cam1 reLogin");
                    TencentLiveManager.login(curId, curSig, new ILiveCallBack() {
                        @Override
                        public void onSuccess(Object data) {
                            LogUtil.d(TAG, "TencentLiveCore cam1 relogin onSuccess", LogUtil.LOG_FILE_NAME_MIND);
                            StreamLogUtil.putLog("cam1 relogin success");
                            MqttConstants.STATUS_LIVE_LOGIN_SUC_CAM1 = true;
                            if (!PropertyUtil.CAM2_ONLY) {
                                TencentLiveManager.createInnerRoom(WawaNewApplication.sActivity, PropertyUtil.ROOM_NUM);
                            }
                        }

                        @Override
                        public void onError(String module, int errCode, String errMsg) {
                            LogUtil.d(TAG, "TencentLiveCore relogin onError", LogUtil.LOG_FILE_NAME_MIND);
                            StreamLogUtil.putLog("cam1 login refail " + errCode + ", " + errMsg);
                        }
                    });
                } else {
                    StreamLogUtil.putLog("cam1 relogin fail: auth ok do not return valid id&sig");
                }
            } else {
                StreamLogUtil.putLog("cam2 relogin curSid --> " + curSid + " curSsig --> " + curSsig);
                LogUtil.d(TAG, "cam2 relogin curSid --> " + curSid + " curSsig --> " + curSsig, LogUtil.LOG_FILE_NAME_MIND);
                i++;
                if (PropertyUtil.ROOM_NUM != 0 && TextUtils.isEmpty(curSsig) && TextUtils.isEmpty(curSid) && i % 4 == 0) {
                    i = 0;
                    TencentCam2Liver.getInstance().setmIsCam2Login(false);
                    TencentCam2Liver.getInstance().startPush(PropertyUtil.ROOM_NUM, curSid, curSsig);
                }
            }
        }
    }

    public void checkServerConnect() {
        if (PropertyUtil.CAM2_ONLY) {
            // 作为第二个辅助摄像头也不开;
            return;
        }

        boolean ret = false;
        if (sServerSocket != null) {
            ret = sServerRWHandler.sendBeatMsg();

            if (!mIsAuth) {
                mBeatCount++;
                if (mBeatCount == 20) {
                    LogUtil.d(TAG, "send auth on beat", LogUtil.LOG_FILE_NAME_MIND);
                    sendAuthMsg();
                    mBeatCount = 0;
                }
            }
        }
        if (!ret || getWaitBeatRespNum() > 3) {
            // 已经挂了, 心跳包都发布出来
            connect();
        } else {
            beatRespNumAdd();
        }
    }

    public void start() {
        mIsStart = true;
        connect();
    }

    public static void setCam2StreamLiveUrl(String cam2Url) {
        STREAM_CAM2_LIVE_URL = cam2Url;
    }

    private synchronized void resetBeatRespNum() {
        LogUtil.d(TAG, "resetBeatRespNum", LogUtil.LOG_FILE_NAME_MIND);
        mWaitBeatRespNum = 0;
    }

    private synchronized void beatRespNumAdd() {
        LogUtil.d(TAG, "beatRespNumAdd:" + mWaitBeatRespNum, LogUtil.LOG_FILE_NAME_MIND);
        mWaitBeatRespNum++;
    }

    private synchronized int getWaitBeatRespNum() {
        LogUtil.d(TAG, "getWaitBeatRespNum:" + mWaitBeatRespNum, LogUtil.LOG_FILE_NAME_MIND);
        return mWaitBeatRespNum;
    }
}
