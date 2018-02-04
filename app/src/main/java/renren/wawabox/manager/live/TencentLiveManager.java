package renren.wawabox.manager.live;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.tencent.TIMLogLevel;
import com.tencent.TIMManager;
import com.tencent.TIMMessage;
import com.tencent.TIMUserProfile;
import com.tencent.av.sdk.AVAudioCtrl;
import com.tencent.av.sdk.AVError;
import com.tencent.av.sdk.AVRoomMulti;
import com.tencent.av.sdk.AVVideoCtrl;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.core.ILiveCameraListener;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILivePushOption;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.core.ILiveRoomOption;
import com.tencent.ilivesdk.data.ILivePushRes;
import com.tencent.ilivesdk.data.ILivePushUrl;
import com.tencent.ilivesdk.tools.quality.ILiveQualityData;
import com.tencent.ilivesdk.view.AVRootView;
import com.tencent.livesdk.ILVCustomCmd;
import com.tencent.livesdk.ILVLiveConfig;
import com.tencent.livesdk.ILVLiveManager;
import com.tencent.livesdk.ILVLiveRoomOption;
import com.tencent.livesdk.ILVText;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import renren.wawabox.app.WawaNewApplication;
import renren.wawabox.config.Constants;
import renren.wawabox.config.MqttConstants;
import renren.wawabox.manager.mind.MindControllerManager;
import renren.wawabox.manager.mqtt.MqttManager;
import renren.wawabox.net.IRequestCallback;
import renren.wawabox.net.RequestFactory;
import renren.wawabox.sp.CommSetting;
import renren.wawabox.utils.AppUtil;
import renren.wawabox.utils.DeviceUtil;
import renren.wawabox.utils.LogUtil;
import renren.wawabox.utils.PropertyUtil;
import renren.wawabox.utils.StreamLogUtil;

import static renren.wawabox.config.Constants.ACCOUNT_TYPE;
import static renren.wawabox.config.Constants.APP_ID;

/**
 * 腾讯云直播相关
 * Created by xunwang on 2017/9/19.
 */

public class TencentLiveManager {
    private static final String TAG = "TencentLiveManager";

    private static final int REQUEST_PHONE_PERMISSIONS = 0;
    private static final int MSG_STREAM_PUSH = 1;

    private static Handler sHandler = null;

    /**
     * 摄像头打开成功, 这时候推了东西上去, 正面,侧面都用这个值,  判断是否在推流:  (cam1 pushing || cam2 pushing) && camera open
     */
    private static boolean sIsCameraOpen = false;
    /**
     * 是否在推流, 正面摄像头专用
     */
    private static boolean sIsSteamPushing = false;

    private static TencentLiveChecker mChecker = null;

    private static void initHandler() {
        HandlerThread sHandlerThread = new HandlerThread("tencentLive");
        sHandlerThread.start();

        sHandler = new Handler(sHandlerThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                int what = msg.what;
                if (what == MSG_STREAM_PUSH) {
                    //pushPanglu(msg.arg1);
                }
            }
        };
    }

    public static void init(final Context context, final AVRootView avRootView) {

        initHandler();
        //iLiveSDK初始化
        ILiveSDK.getInstance().initSdk(context, APP_ID, ACCOUNT_TYPE);


        ILVLiveManager.getInstance().setAvVideoView(avRootView);
        TIMManager.getInstance().setLogLevel(TIMLogLevel.ERROR);

        //初始化直播场景
        ILVLiveConfig liveConfig = new ILVLiveConfig();
        ILVLiveManager.getInstance().init(liveConfig);

        //设置小窗口初始位置
        avRootView.setGravity(AVRootView.LAYOUT_GRAVITY_RIGHT);
        avRootView.setSubMarginX(12);
        avRootView.setSubMarginY(100);

        //配置拖拽
        avRootView.setSubCreatedListener(new AVRootView.onSubViewCreatedListener() {
            @Override
            public void onSubViewCreated() {
                for (int i = 1; i < 3; i++) {
                    avRootView.getViewByIndex(i).setDragable(true);
                }
            }
        });

        liveConfig.setLiveMsgListener(new ILVLiveConfig.ILVLiveMsgListener() {
            @Override
            public void onNewTextMsg(ILVText text, String SenderId, TIMUserProfile userProfile) {
                Toast.makeText(context, "onNewTextMsg : " + text.getText(), Toast.LENGTH_SHORT).show();
                LogUtil.d(TAG, "onNewTextMsg : " + text.getText(), LogUtil.LOG_FILE_NAME_LIVE);
            }

            @Override
            public void onNewCustomMsg(ILVCustomCmd cmd, String id, TIMUserProfile userProfile) {
                Toast.makeText(context, "cmd " + cmd, Toast.LENGTH_SHORT).show();
                LogUtil.d(TAG, "cmd " + cmd, LogUtil.LOG_FILE_NAME_LIVE);
            }

            @Override
            public void onNewOtherMsg(TIMMessage message) {
                Toast.makeText(context, "onNewOtherMsg " + message, Toast.LENGTH_SHORT).show();

            }
        });

    }

    public static boolean isCam2Pushing() {
        return TencentCam2Liver.getInstance().isPushing();
    }

    public static boolean isStreamOk() {
        if (PropertyUtil.CAM2_ONLY) {
            return isCam2Pushing();
        } else {
            return sIsSteamPushing && sIsCameraOpen;
        }
    }

    public static void login(final String id, final String sig, final ILiveCallBack listener) {
        ILiveLoginManager.getInstance().iLiveLogin(id, sig, new WrapperILiveCallBack(listener));
    }

    public static void startCam2Push(int roomNum, String id, String sig) {
        TencentCam2Liver.getInstance().startPush(roomNum, id, sig);
    }

    public static void endCam2Push() {
        TencentCam2Liver.getInstance().endPush();
    }

    private static AVAudioCtrl.RegistAudioDataCompleteCallbackWithByteBuffer mAudioDataCompleteCallbackWithByffer =
            new AVAudioCtrl.RegistAudioDataCompleteCallbackWithByteBuffer() {
                @Override
                public int onComplete(AVAudioCtrl.AudioFrameWithByteBuffer audioFrameWithByteBuffer, int srcType) {
                    StreamLogUtil.putLog("自定义塞入音频 onComplete");
                    if (srcType == AVAudioCtrl.AudioDataSourceType.AUDIO_DATA_SOURCE_MIXTOSEND) {
                        synchronized (TencentLiveManager.class) {
                            try {
                                InputStream is = WawaNewApplication.getAppContext().getAssets().open("txaudio.wav");
                                int lenght = is.available();
                                byte[] buffer = new byte[lenght];
                                is.read(buffer);
                                ByteBuffer buf = ByteBuffer.wrap(buffer);
                                audioFrameWithByteBuffer.data = buf;
                                StreamLogUtil.putLog("自定义塞入音频 完成");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return AVError.AV_OK;
                }
            };

    public static void createInnerRoom(final Activity context, int roomNum) {
        final int room = roomNum;
        LogUtil.d(TAG, "create inner room auto bits = " + AVRoomMulti.AUTH_BITS_DEFAULT + ", camera " + (AVRoomMulti.AUTH_BITS_DEFAULT & AVRoomMulti.AUTH_BITS_SEND_CAMERA_VIDEO), LogUtil.LOG_FILE_NAME_LIVE);
        String groupId = String.valueOf(roomNum);
        if (CommSetting.getGroupId() != null) {
            groupId = CommSetting.getGroupId();
        }
        LogUtil.d(TAG, "groupId --> " + groupId, LogUtil.LOG_FILE_NAME_LIVE);
        //创建房间配置项
        final ILVLiveRoomOption hostOption = new ILVLiveRoomOption(ILiveLoginManager.getInstance().getMyUserId()).
                controlRole("LiveMaster")//角色设置
                .autoFocus(false)
                .autoRender(false)
                .autoCamera(true)
                .imsupport(true)
                .imGroupId(groupId)
                .autoMic(false)
                .autoSpeaker(false)
                .authBits(AVRoomMulti.AUTH_BITS_CREATE_ROOM
                        | AVRoomMulti.AUTH_BITS_SEND_CAMERA_VIDEO
                        | AVRoomMulti.AUTH_BITS_JOIN_ROOM
                        | AVRoomMulti.AUTH_BITS_SEND_SCREEN_VIDEO
                        | AVRoomMulti.AUDIO_CATEGORY_MEDIA_PLAY_AND_RECORD
                        | AVRoomMulti.AUTH_BITS_RECV_CAMERA_VIDEO
                        | AVRoomMulti.AUTH_BITS_RECV_SCREEN_VIDEO)//权限设置
                .cameraId(ILiveConstants.FRONT_CAMERA)//摄像头前置后置
                .cameraListener(new ILiveCameraListener() {
                    @Override
                    public void onCameraEnable(int cameraId) {
                        LogUtil.d(TAG, "onCameraEnable " + cameraId, LogUtil.LOG_FILE_NAME_LIVE);
                        sIsCameraOpen = true;
                    }

                    @Override
                    public void onCameraDisable(int cameraId) {
                        LogUtil.d(TAG, "onCameraDisable " + cameraId, LogUtil.LOG_FILE_NAME_LIVE);
                        sIsCameraOpen = false;
                    }

                    @Override
                    public void onCameraPreviewChanged(int cameraId) {
                        LogUtil.d(TAG, "onCameraPreviewChanged " + cameraId, LogUtil.LOG_FILE_NAME_LIVE);
                    }
                })
                .roomDisconnectListener(new ILiveRoomOption.onRoomDisconnectListener() {
                    @Override
                    public void onRoomDisconnect(int errCode, String errMsg) {
                        LogUtil.d(TAG, "onRoomDisconnect " + errCode + ", " + errMsg, LogUtil.LOG_FILE_NAME_LIVE);
                        sIsSteamPushing = false;
                    }
                })
                .exceptionListener(new ILiveRoomOption.onExceptionListener() {
                    @Override
                    public void onException(int exceptionId, int errCode, String errMsg) {
                        LogUtil.d(TAG, "onException " + exceptionId + ", " + errCode + ", " + errMsg, LogUtil.LOG_FILE_NAME_LIVE);
                        sIsSteamPushing = false;
                    }
                })
                .videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_MANUAL);//是否开始半自动接收

        StreamLogUtil.putLog("prepare room " + room + ", group id = " + hostOption.getIMGroupId() + ", " + hostOption.getGroupType());
        //创建房间
        ILVLiveManager.getInstance().createRoom(room, hostOption, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                LogUtil.d(TAG, "create room ok " + room, LogUtil.LOG_FILE_NAME_LIVE);
                StreamLogUtil.putLog("create room ok roomId --> " + room);

                MqttConstants.STATUS_LIVE_CREATE_ROOM_SUC_CAM1 = true;

                LogUtil.d(TAG, "Create group " + ILiveRoomManager.getInstance().getIMGroupId(), LogUtil.LOG_FILE_NAME_LIVE);

                CommSetting.setRoomId(room);

                // 创建房间成功
                sIsSteamPushing = true;

                pushExtraStream(room);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                if (errCode == 10021) {//如果创建房间失败切返回10021时尝试再创建一次,groupId被占用

                    String erroLog = String.format("errorCode = %s, msg= %s", errCode + "", errMsg);
                    StreamLogUtil.putLog(erroLog);
                    LogUtil.d(TAG, erroLog, LogUtil.LOG_FILE_NAME_LIVE);
                    CommSetting.setGroupId(hostOption.getIMGroupId() + "g");
                    LogUtil.d(TAG, "createRoom again hostOption.getIMGroupId() --> " + hostOption.getIMGroupId(), LogUtil.LOG_FILE_NAME_LIVE);
                    restartLive("group Id 被占用，刷新并重启");

                    return;
                } else if (errCode == 1003) {//创建房间失败
                    restartLive("创建房间出现 1003 错误，重启");
                }
                String msg = module + "|create fail " + errCode + " " + errMsg;
                LogUtil.d(TAG, "create room error " + msg, LogUtil.LOG_FILE_NAME_LIVE);
                StreamLogUtil.putLog("create room error " + msg);

                if (errCode == AVError.AV_ERR_HAS_IN_THE_STATE) {
                    // 已经上过房间了,不管!
                    sIsSteamPushing = true;
                } else {
                    sIsSteamPushing = false;
                }
            }

        });
    }


    private static void pushPanglu(int room) {
//        StreamLogUtil.putLog("自定义塞入音频");
//        ILiveSDK.getInstance().getAvAudioCtrl().registAudioDataCallbackWithByteBuffer(
//                AVAudioCtrl.AudioDataSourceType.AUDIO_DATA_SOURCE_MIXTOSEND, mAudioDataCompleteCallbackWithByffer);

        final String curStreamLiveUrl = MindControllerManager.getCurStreamLiveUrl();
        LogUtil.d(TAG, "start push stream on cur url = " + curStreamLiveUrl, LogUtil.LOG_FILE_NAME_LIVE);
        if (TextUtils.isEmpty(curStreamLiveUrl)) {
            // 推旁路直播
            ILivePushOption option = new ILivePushOption();
            option.channelName("room " + room);
            option.encode(ILivePushOption.Encode.HLS_AND_RTMP);

            ILiveRoomManager.getInstance().startPushStream(option, new ILiveCallBack<ILivePushRes>() {

                @Override
                public void onSuccess(ILivePushRes data) {
//                    MqttConstants.STATUS_LIVE_PANGLU_SUC = true;
                    List<ILivePushUrl> urls = data.getUrls();
                    long streamId = data.getChnlId();
                    StringBuilder sb = new StringBuilder();

                    sb.append("stream id = ").append(streamId).append("\n");
                    String getUrl = null;
                    if (urls != null && !urls.isEmpty()) {
                        for (ILivePushUrl pushUrl : urls) {
                            if (pushUrl.getEncode() == ILivePushOption.Encode.RTMP.getEncode()) {
                                getUrl = pushUrl.getUrl().split("://")[1];
                            }
                            sb.append("url: ").append(pushUrl.getUrl()).append(", encode ").
                                    append(pushUrl.getEncode()).append(", rate ").append(pushUrl.getRateType()).append("\n");
                            if (pushUrl.getUrl() != null && pushUrl.getUrl().startsWith("rtmp")) {
                                if (PropertyUtil.CAM2_ONLY) {
                                    requestServerWithUrl(pushUrl.getUrl(), 1);
                                } else {
                                    requestServerWithUrl(pushUrl.getUrl(), 0);
                                }
                            }
                        }
                    } else {
                        sb.append("null urls");
                    }

                    if (getUrl != null) {
                        String[] temp = getUrl.split("/");
                        if (temp.length > 0) {
                            String liveId = temp[temp.length - 1];
                            mChecker = new TencentLiveChecker(liveId, new TencentLiveChecker.onLiveErrorListener() {
                                @Override
                                public void onLiveClose() {
                                    //restartLive("直播流挂了");
                                }

                                @Override
                                public void onLiveOff() {
                                    //restartLive("直播流挂了");
                                }

                                @Override
                                public void onLiveSuc() {
//                                    MqttConstants.STATUS_LIVE_CHECK_LIVE = true;
                                }
                            });
                        }
                        MindControllerManager.setCurStreamLiveUrl(getUrl);
                        if (PropertyUtil.CAM2_ONLY) {
                            MqttManager.getInstance().publishCam2StreamOn(getUrl);
                        }

                    }

                    LogUtil.d(TAG, "start push stream " + sb.toString(), LogUtil.LOG_FILE_NAME_LIVE);
                    StreamLogUtil.putLog("start push stream " + sb.toString());

                }

                @Override
                public void onError(String module, int errCode, String errMsg) {
                    StreamLogUtil.putLog("push stream error " + module + ", " + errCode + ", " + errMsg);
                    MindControllerManager.setCurStreamLiveUrl(null);
                    if (errCode == 40000403) {
                        DeviceUtil.reboot("40000403  查询房间拉取grocery不存在");
                    }
                }
            });
            sHandler.removeMessages(MSG_STREAM_PUSH);
            sHandler.sendMessageDelayed(sHandler.obtainMessage(MSG_STREAM_PUSH, room, 0), 5000);
        } else {
            // SUCCESS DO nothing
        }
    }

    /**
     * 将推流的url给服务器
     *
     * @param url
     */
    private static void requestServerWithUrl(String url, int type) {
        LogUtil.d(TAG, "requestServerWithUrl url --> " + url, LogUtil.LOG_FILE_NAME_MIND);
        StreamLogUtil.putLog("requestServerWithUrl url --> " + url + " type --> " + type);
        HashMap<String, String> params = new HashMap<>();
        params.put("room_id", String.valueOf(PropertyUtil.ROOM_NUM));
        if (type == 0) {
            params.put("stream_url", url);
        } else {
            params.put("stream_url_slave", url);
        }
        String requestUrl = Constants.BASE_URL + "internal/room/live-stream";
        RequestFactory.getRequestManager(RequestFactory.RequestType.OKHTTP).post(requestUrl, AppUtil.hashMapToJson(params), new IRequestCallback() {
            @Override
            public void onSuccess(String response) {
                StreamLogUtil.putLog("requestServerWithUrl --> " + response);
            }

            @Override
            public void onFailure(Throwable throwable) {
                StreamLogUtil.putLog("requestServerWithUrl 失败");
            }
        });

    }

    public static void pushExtraStream(int room) {
        StreamLogUtil.putLog("is enable fast live --> " + PropertyUtil.ENABLE_FAST_LIVE);
        if (PropertyUtil.ENABLE_FAST_LIVE) {
            boolean bRet = ILiveSDK.getInstance().getAvVideoCtrl().setLocalVideoPreProcessCallback(new AVVideoCtrl.LocalVideoPreProcessCallback() {
                @Override
                public void onFrameReceive(AVVideoCtrl.VideoFrame var1) {
                    // 回调的数据，传递给 ilivefilter processData接口处理
                    FastLiveManager.getInstance().sendFrame(var1.data, var1.width, var1.height);
                    LogUtil.d(TAG, "onFrameReceive " + var1.dataLen + ", " + var1.width + "x" + var1.height + ", format = " + var1.videoFormat + ", " + var1.identifier, LogUtil.LOG_FILE_NAME_LIVE);
                }
            });
            LogUtil.d(TAG, "setLocalVideoPreProcessCallback = " + bRet, LogUtil.LOG_FILE_NAME_LIVE);
        }
        sHandler.sendMessageDelayed(sHandler.obtainMessage(MSG_STREAM_PUSH, room, 0), 3000);
    }

    public static ILiveQualityData getLiveQuality() {
        if (ILiveRoomManager.getInstance() != null) {
            ILiveQualityData data = ILiveRoomManager.getInstance().getQualityData();
            return data;
        }
        return null;
    }

    public static void quitRoomAndLogout() {
        LogUtil.d(TAG, "quitRoomAndLogout", LogUtil.LOG_FILE_NAME_LIVE);
        if (PropertyUtil.CAM2_ONLY)
            TencentCam2Liver.getInstance().quitRoomAndLogout();
        else
            ILiveRoomManager.getInstance().quitRoom(new ILiveCallBack() {
                @Override
                public void onSuccess(Object data) {
                    mainLivelogout();
                }

                @Override
                public void onError(String module, int errCode, String errMsg) {
                    mainLivelogout();
                }
            });
    }

    /**
     * 主摄像头退出
     */
    public static void mainLivelogout() {
        LogUtil.d(TAG, "mainLivelogout", LogUtil.LOG_FILE_NAME_LIVE);
        ILiveLoginManager.getInstance().iLiveLogout(new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
            }
        });
    }


    public static void checkPermission(Activity context) {
        final List<String> permissionsList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.CAMERA);
            if ((context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.RECORD_AUDIO);
            if ((context.checkSelfPermission(Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.WAKE_LOCK);
            if ((context.checkSelfPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
            if ((context.checkSelfPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
            if ((context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if ((context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permissionsList.size() != 0) {
                context.requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_PHONE_PERMISSIONS);
            }
        }
    }

    public static class WrapperILiveCallBack<T> implements ILiveCallBack<T> {

        private ILiveCallBack<T> mIliveCallback;

        public WrapperILiveCallBack(ILiveCallBack<T> liveCallBack) {
            mIliveCallback = liveCallBack;
        }

        @Override
        public void onSuccess(T data) {
            if (mIliveCallback != null) {
                mIliveCallback.onSuccess(data);
            }
        }

        @Override
        public void onError(String module, int errCode, String errMsg) {
            if (mIliveCallback != null) {
                mIliveCallback.onError(module, errCode, errMsg);
            }
        }
    }

    public static void checkAndKeep() {
        LogUtil.d(TAG, "checkAndKeep", LogUtil.LOG_FILE_NAME_LIVE);
        if (mChecker != null)
            mChecker.doCheck();
    }

    /**
     * 重启直播
     */
    public static void restartLive(String reason) {
        LogUtil.d(TAG, "restartLive reboot", LogUtil.LOG_FILE_NAME_LIVE);
        //直接重启
        DeviceUtil.reboot(reason);
    }
}
