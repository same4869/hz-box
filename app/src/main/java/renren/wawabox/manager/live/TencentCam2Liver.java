package renren.wawabox.manager.live;

import com.tencent.av.sdk.AVRoomMulti;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveMemStatusLisenter;
import com.tencent.ilivesdk.core.ILiveCameraListener;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.core.ILiveRoomOption;
import com.tencent.livesdk.ILVChangeRoleRes;
import com.tencent.livesdk.ILVLiveManager;
import com.tencent.livesdk.ILVLiveRoomOption;

import java.util.Arrays;

import renren.wawabox.config.Constants;
import renren.wawabox.config.MqttConstants;
import renren.wawabox.manager.mqtt.MqttManager;
import renren.wawabox.utils.DeviceUtil;
import renren.wawabox.utils.LogUtil;
import renren.wawabox.utils.PropertyUtil;
import renren.wawabox.utils.StreamLogUtil;

/**
 * 管理副摄像头一系列操作
 * <p>
 * Created by xunwang on 2017/9/19.
 */

public class TencentCam2Liver {
    private static final String TAG = "TencentCam2Liver";

    private static final TencentCam2Liver sInstance = new TencentCam2Liver();

    private static final String ROLE_GUEST = "Guest";
    private static final String ROLE_LIVE_GUEST = "LiveGuest";

    private boolean mIsCam2InRoom = false;
    private boolean mIsCam2Login = false;
    private boolean mIsCameraOpen = true;

    private int errCodeRebootTimes = 3; //3次出现1007错误重启

    /**
     * 房间2 是否在
     */
    private boolean mIsCam2Push = false;

    public static TencentCam2Liver getInstance() {
        return sInstance;
    }

    public boolean isPushing() {
        return mIsCam2InRoom && mIsCam2Push && mIsCameraOpen;
    }

    public void startPush(final int roomNum, String id, String sig) {
        if (!mIsCam2Login) {
            LogUtil.d(Constants.TAG, "cam2 startPush login joinroom uptovideo");
            TencentLiveManager.login(id, sig, new ILiveCallBack() {
                @Override
                public void onSuccess(Object data) {
                    StreamLogUtil.putLog("cam2 login success");
                    MqttConstants.STATUS_LIVE_LOGIN_SUC_CAM2 = true;
                    mIsCam2Login = true;
                    joinRoomNoRecv(roomNum, new ILiveCallBack() {
                        @Override
                        public void onSuccess(Object data) {
                            mIsCam2InRoom = true;
                            upToVideoMember(null);
                        }

                        @Override
                        public void onError(String module, int errCode, String errMsg) {
                            if (errCode != 1003) {
                                mIsCam2InRoom = false;
                            }
                        }
                    });
                }

                @Override
                public void onError(String module, int errCode, String errMsg) {
                    StreamLogUtil.putLog("cam2 login error " + errCode + " : " + errMsg);
                    mIsCam2Login = false;
                }
            });
        } else if (!mIsCam2InRoom) {
            mIsCam2Login = true;
            LogUtil.d(Constants.TAG, "cam2 startPush joinroom uptovideo");
            joinRoomNoRecv(roomNum, new ILiveCallBack() {
                @Override
                public void onSuccess(Object data) {
                    mIsCam2InRoom = true;
                    upToVideoMember(null);
                }

                @Override
                public void onError(String module, int errCode, String errMsg) {
                    mIsCam2InRoom = false;
                }
            });
        } else if (!mIsCam2Push) {
            mIsCam2Login = true;
            mIsCam2InRoom = true;
            LogUtil.d(Constants.TAG, "cam2 startPush uptovideo");
            upToVideoMember(null);
        } else {
            StreamLogUtil.putLog("cam2 pushing skip ");
        }

    }

    public void endPush() {
        downToVideoMember(new ILiveCallBack<ILVChangeRoleRes>() {
            @Override
            public void onSuccess(ILVChangeRoleRes data) {
                StreamLogUtil.putLog("cam2 downToVideoMember success");
//                UsbCameraManager.getInstance().endLoopGetFrame();
                mIsCam2Push = false;
                quitRoomAndLogout();
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                StreamLogUtil.putLog("cam2 downToVideoMember error " + module + ", " + errCode + " : " + errMsg);
//                UsbCameraManager.getInstance().endLoopGetFrame();
            }
        });
    }

    /**
     * 连麦
     *
     * @param callBack
     */
    public void upToVideoMember(ILiveCallBack<ILVChangeRoleRes> callBack) {
        ILVLiveManager.getInstance().upToVideoMember(ROLE_LIVE_GUEST, true, false, new TencentLiveManager.WrapperILiveCallBack<ILVChangeRoleRes>(callBack) {
            @Override
            public void onSuccess(ILVChangeRoleRes data) {
                StreamLogUtil.putLog("cam2 upToVideoMember room success");
                LogUtil.d(Constants.TAG, "cam2 upToVideoMember room success", LogUtil.LOG_FILE_NAME_LIVE);

                MqttConstants.STATUS_LIVE_UP_TO_VIDEO_SUC_CAM2 = true;

                MqttManager.getInstance().publishCam2On();
                super.onSuccess(data);
                mIsCam2InRoom = true;
                mIsCam2Push = true;
                TencentLiveManager.pushExtraStream(PropertyUtil.MQTT_CLAW_MACHINE_ID);

            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                StreamLogUtil.putLog("cam2 upToVideoMember fail " + module + ", " + errCode + " : " + errMsg);
                LogUtil.d(Constants.TAG, "cam2 upToVideoMember fail " + module + ", " + errCode + " : " + errMsg, LogUtil.LOG_FILE_NAME_LIVE);
                super.onError(module, errCode, errMsg);
                if (errCode != 1003) {
                    mIsCam2InRoom = false;
                }
            }
        });
    }


    public static void downToVideoMember(ILiveCallBack<ILVChangeRoleRes> callBack) {
        ILVLiveManager.getInstance().downToNorMember(ROLE_GUEST, callBack);
    }


    private void joinRoomNoRecv(int roomNum, ILiveCallBack callback) {
        ILVLiveRoomOption memberOption = new ILVLiveRoomOption(ILiveLoginManager.getInstance().getMyUserId())
                .autoCamera(true)
                .controlRole(ROLE_GUEST)
                .cameraId(ILiveConstants.FRONT_CAMERA)
                .autoFocus(false)
                .autoRender(false)
                .autoSpeaker(false)
                .authBits(AVRoomMulti.AUTH_BITS_JOIN_ROOM
                        | AVRoomMulti.AUTH_BITS_SEND_CAMERA_VIDEO
                        | AVRoomMulti.AUTH_BITS_SEND_SCREEN_VIDEO
                |AVRoomMulti.AUTH_BITS_CREATE_ROOM)//权限设置

                .videoRecvMode(AVRoomMulti.VIDEO_RECV_MODE_MANUAL)
                .autoMic(false)
                .setRoomMemberStatusLisenter(new ILiveMemStatusLisenter() {
                    @Override
                    public boolean onEndpointsUpdateInfo(int eventid, String[] updateList) {
                        LogUtil.d(Constants.TAG, "cam2 onEndpointsUpdateInfo " + eventid + ", " + Arrays.toString(updateList), LogUtil.LOG_FILE_NAME_LIVE);
                        //TODO ?
                        return false;
                    }
                })
                .exceptionListener(new ILiveRoomOption.onExceptionListener() {
                    @Override
                    public void onException(int exceptionId, int errCode, String errMsg) {
                        LogUtil.e(TAG, "cam2 room exception " + errMsg + ", " + errMsg + ", " + errMsg, LogUtil.LOG_FILE_NAME_LIVE);
                        mIsCam2InRoom = false;
                    }
                })
                .cameraListener(new ILiveCameraListener() {
                    @Override
                    public void onCameraEnable(int cameraId) {
                        LogUtil.i(TAG, "cam2 camera open");
                        mIsCameraOpen = true;
                    }

                    @Override
                    public void onCameraDisable(int cameraId) {
                        LogUtil.d(Constants.TAG, "cam2 camera disable", LogUtil.LOG_FILE_NAME_LIVE);
                        mIsCameraOpen = false;
                    }

                    @Override
                    public void onCameraPreviewChanged(int cameraId) {

                    }
                })
                .roomDisconnectListener(new ILiveRoomOption.onRoomDisconnectListener() {
                    @Override
                    public void onRoomDisconnect(int errCode, String errMsg) {
                        LogUtil.e(TAG, "cam2 room disconnect " + errCode + ", " + errMsg, LogUtil.LOG_FILE_NAME_LIVE);
                        mIsCam2InRoom = false;
                        mIsCam2Push = false;
                    }
                });

        ILVLiveManager.getInstance().joinRoom(roomNum, memberOption, new TencentLiveManager.WrapperILiveCallBack(callback) {
            @Override
            public void onSuccess(Object data) {
                StreamLogUtil.putLog("cam2 join room success");
                LogUtil.i(TAG, "cam2 join room success");

                MqttConstants.STATUS_LIVE_JOIN_ROOM_SUC_CAM2 = true;
                mIsCam2InRoom = true;

                super.onSuccess(data);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                super.onError(module, errCode, errMsg);
                StreamLogUtil.putLog("cam2 joinRoomNoRecv error " + module + ", " + errCode + " : " + errMsg + " errCodeRebootTimes --> " + errCodeRebootTimes);
                LogUtil.d(Constants.TAG, "cam2 joinRoomNoRecv error " + module + ", " + errCode + " : " + errMsg, LogUtil.LOG_FILE_NAME_LIVE);
                mIsCam2InRoom = false;
                if (errCode == 1007) {
                    errCodeRebootTimes--;
                    if (errCodeRebootTimes <= 0) {
                        DeviceUtil.reboot("腾讯1007错误，重启");
                    }
                }
            }
        });

    }


    public void quitRoomAndLogout() {
        ILiveRoomManager.getInstance().quitRoom(new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                mIsCam2InRoom = false;
                logout();
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                logout();
            }
        });
    }

    public void logout() {
        ILiveLoginManager.getInstance().iLiveLogout(new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                mIsCam2Login = false;
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                mIsCam2Login = false;
            }
        });
    }

    public void setmIsCam2Login(boolean mIsCam2Login) {
        this.mIsCam2Login = mIsCam2Login;
    }
}
