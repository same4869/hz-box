package renren.wawabox.manager.mqtt;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tencent.ilivesdk.ILiveSDK;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import commlib.xun.com.commlib.thread.CommThreadPool;
import renren.wawabox.app.WawaNewApplication;
import renren.wawabox.bean.PubMessage;
import renren.wawabox.bean.ReceivedMessage;
import renren.wawabox.bean.Subscription;
import renren.wawabox.config.MachineProperties;
import renren.wawabox.config.MqttConstants;
import renren.wawabox.listener.ActionListener;
import renren.wawabox.listener.IReceivedMessageListener;
import renren.wawabox.manager.live.TencentLiveManager;
import renren.wawabox.manager.mind.MindControllerManager;
import renren.wawabox.sp.CommSetting;
import renren.wawabox.utils.AppUtil;
import renren.wawabox.utils.DeviceUtil;
import renren.wawabox.utils.LogUtil;
import renren.wawabox.utils.NetworkKeeperUtil;
import renren.wawabox.utils.PropertyUtil;
import renren.wawabox.utils.StreamLogUtil;

import static renren.wawabox.config.MqttConstants.CMD_CAM_END;
import static renren.wawabox.config.MqttConstants.CMD_CAM_ON;
import static renren.wawabox.config.MqttConstants.CMD_CAM_REBOOT;
import static renren.wawabox.config.MqttConstants.CMD_CAM_REQUEST_LIVE_URL;
import static renren.wawabox.config.MqttConstants.CMD_CAM_START;
import static renren.wawabox.config.MqttConstants.CMD_CAM_WAIT;
import static renren.wawabox.config.MqttConstants.CMD_STREAM_ON;
import static renren.wawabox.config.MqttConstants.CONTROL_BACKWARD_END_NEW;
import static renren.wawabox.config.MqttConstants.CONTROL_BACKWARD_START_NEW;
import static renren.wawabox.config.MqttConstants.CONTROL_FORWARD_END_NEW;
import static renren.wawabox.config.MqttConstants.CONTROL_FORWARD_START_NEW;
import static renren.wawabox.config.MqttConstants.CONTROL_LEFT_END_NEW;
import static renren.wawabox.config.MqttConstants.CONTROL_LEFT_START_NEW;
import static renren.wawabox.config.MqttConstants.CONTROL_RIGHT_END_NEW;
import static renren.wawabox.config.MqttConstants.CONTROL_RIGHT_START_NEW;
import static renren.wawabox.config.MqttConstants.CONTROL_TAKE_NEW;
import static renren.wawabox.config.MqttConstants.G_LENGTH;
import static renren.wawabox.config.MqttConstants.G_RAZ_1;
import static renren.wawabox.config.MqttConstants.G_RAZ_2;
import static renren.wawabox.config.MqttConstants.HANDLE_CONNECTION;
import static renren.wawabox.config.MqttConstants.HANDLE_SUBSCRIPTION;
import static renren.wawabox.config.MqttConstants.MAX_CLAW;
import static renren.wawabox.config.MqttConstants.MAX_DOWN_CLAWTIME;
import static renren.wawabox.config.MqttConstants.MIN_DOWN_CLAWTIME;
import static renren.wawabox.config.MqttConstants.MSG_ADD_SUB;
import static renren.wawabox.config.MqttConstants.MSG_CAM2_HANDLE;
import static renren.wawabox.config.MqttConstants.MSG_CAM2_WAITING;
import static renren.wawabox.config.MqttConstants.MSG_GET_RESULT_OVERTIME;
import static renren.wawabox.config.MqttConstants.MSG_PREPARE_PUB;
import static renren.wawabox.config.MqttConstants.MSG_PUB_NOW;
import static renren.wawabox.config.MqttConstants.MSG_R1_CMD;
import static renren.wawabox.config.MqttConstants.MSG_SETTING_TRIG;
import static renren.wawabox.config.MqttConstants.MSG_WAIT_RESULT;
import static renren.wawabox.config.MqttConstants.ORI_X_BACK;
import static renren.wawabox.config.MqttConstants.ORI_X_FRONT;
import static renren.wawabox.config.MqttConstants.ORI_Y_BACK;
import static renren.wawabox.config.MqttConstants.ORI_Y_FRONT;
import static renren.wawabox.config.MqttConstants.SENSE_1_RESULT;
import static renren.wawabox.config.MqttConstants.SENSE_2_RESULT;
import static renren.wawabox.config.MqttConstants.SENSOR_1_GOT;
import static renren.wawabox.config.MqttConstants.SENSOR_2_GOT;
import static renren.wawabox.config.MqttConstants.SPEED;
import static renren.wawabox.config.MqttConstants.SWITCH_Y_BACK;
import static renren.wawabox.config.MqttConstants.SWITCH_Y_FRONT;
import static renren.wawabox.config.MqttConstants.TOPIC_ANDROID_DEBUG;
import static renren.wawabox.config.MqttConstants.TOPIC_ANDROID_IN;
import static renren.wawabox.config.MqttConstants.TOPIC_ANDROID_REBOOT;
import static renren.wawabox.config.MqttConstants.TOPIC_ANDROID_REBOOT_REASON;
import static renren.wawabox.config.MqttConstants.TOPIC_ANDROID_STATUS;
import static renren.wawabox.config.MqttConstants.TOPIC_ANDROID_VERSION;
import static renren.wawabox.config.MqttConstants.TOPIC_CAM;
import static renren.wawabox.config.MqttConstants.TOPIC_IP_WATCH;
import static renren.wawabox.config.MqttConstants.TOPIC_PUB;
import static renren.wawabox.config.MqttConstants.TOPIC_SETTING;
import static renren.wawabox.config.MqttConstants.TOPIC_SETTING_ID;
import static renren.wawabox.config.MqttConstants.TOPIC_SUB;
import static renren.wawabox.config.MqttConstants.TOPIC_WATCH_DOG;

/**
 * 管理和MQTT主板的相关通信与逻辑处理
 * Created by xunwang on 2017/9/13.
 */

public class MqttManager {
    public static final String TAG = "MqttManager";
    private static final int GET_RESULT_OVERTIME = 9000;

    public static final int TIME_TRIP = 6;
    private ResultListener mListener = null;
    private HandlerThread mThread;
    private Handler mHandler;
    private static MqttManager sInstance;
    //    private static int mqttReconnect = 10;//如果这么多次连接mqtt服务器都失败，则重启
    private static int publishFailedCount;

    /**
     * 检测是不是满仓
     */
    private boolean mIsCheckStuck = false;
    private int mLastForce;
    private Connection sConnection = null;
    private boolean mIsWaitResult = false;

    private static boolean isAdded = false;

    private static String[] G_SWITCH_NAMES = new String[]{
            "x",
            "y",
            "z",
            "c",
            SWITCH_Y_BACK,
            SWITCH_Y_FRONT,
            "x_2",  // r
            "x_1",  // l
            "z_1",
            "z_2",
            "s_1",
            "s_2"
    };

    /**
     * 上次g命令结果;
     */
    private int[] mLastGresult = null;
    private long mLastCamHandleTime = 0;
    private String mLastCamMsg = null;
    private long mLastTryConncetTime = 0;

    /**
     * 待发送的消息队列, 不设上限;
     */
    private LinkedBlockingQueue<PubMessage> mPubMessages = new LinkedBlockingQueue<>();

    public interface ResultListener {
        void onGetResult(boolean success);
    }

    public interface ClawPublishListener {
        void onPublishSuccess(String msg);

        void onRecvClawOutOk(String msg);
    }

    private MqttManager(Context context, ResultListener listener) {
        // create connection
        mListener = listener;

        // create handler thread
        mThread = new HandlerThread("mqtt");
        mThread.start();

        mHandler = new Handler(mThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                handMessage(msg);
            }
        };
    }

    public static MqttManager getInstance() {
        return sInstance;
    }

    public static void init(Context context, ResultListener listener) {
//        // 初始化日志
//        MqttKeeper.init();
//        NetworkKeeper.init();
        G_SWITCH_NAMES = new String[]{
                "x",
                "y",
                "z",
                "c",
                SWITCH_Y_BACK,
                SWITCH_Y_FRONT,
                "x_2",  // r
                "x_1",  // l
                "z_1",
                "z_2",
                "s_2",
                "s_1"
        };

        sInstance = new MqttManager(context, listener);

    }

    public boolean isConnected() {
        return sConnection.isConnected();
    }

    public void start() {
        if (!isAdded) {
            TOPIC_SUB += PropertyUtil.MQTT_CLAW_MACHINE_ID;
            TOPIC_PUB += PropertyUtil.MQTT_CLAW_MACHINE_ID;
            TOPIC_CAM += PropertyUtil.MQTT_CLAW_MACHINE_ID;
            TOPIC_SETTING_ID += PropertyUtil.MQTT_CLAW_MACHINE_ID;
            TOPIC_WATCH_DOG += PropertyUtil.MQTT_CLAW_MACHINE_ID;

            TOPIC_ANDROID_IN += PropertyUtil.MQTT_CLAW_MACHINE_ID;
            MqttConstants.TOPIC_ANDROID_OUT += PropertyUtil.MQTT_CLAW_MACHINE_ID;
            isAdded = true;
        }

        if (MachineProperties.MACHINE_BACK_FRONT == 1) {
            ORI_Y_FRONT = "";
            ORI_Y_BACK = "-";
            SWITCH_Y_FRONT = "y_2";
            SWITCH_Y_BACK = "y_1";
        } else {
            ORI_Y_FRONT = "-";
            ORI_Y_BACK = "";
            SWITCH_Y_FRONT = "y_1";
            SWITCH_Y_BACK = "y_2";
        }


        if (sConnection != null && sConnection.isConnected()) {
            // 已经连接上服务器了
            mHandler.sendEmptyMessage(MSG_ADD_SUB);
        } else {
            connect();
        }
    }

    public void connect() {
        if (sConnection != null) {

            try {
                sConnection.disconnect();
            } catch (RuntimeException e) {
                LogUtil.d(TAG, "disconnect last error " + e.getMessage(), LogUtil.LOG_FILE_NAME_MQTT);
            }

        }

        LogUtil.d(TAG, "PropertyUtils.MQTT_SERVER_IP --> " + PropertyUtil.MQTT_SERVER_IP + " PropertyUtils.MQTT_SERVER_PORT --> " + PropertyUtil.MQTT_SERVER_PORT, LogUtil.LOG_FILE_NAME_MQTT);
        sConnection = Connection.createConnection(HANDLE_CONNECTION, PropertyUtil.MQTT_CONNECTION_ID, PropertyUtil.MQTT_SERVER_IP, PropertyUtil.MQTT_SERVER_PORT, WawaNewApplication.getInstance(), false);

        sConnection.addReceivedMessageListner(new IReceivedMessageListener() {
            @Override
            public void onMessageReceived(ReceivedMessage message) {
                LogUtil.d(TAG, "mqtt addReceivedMessageListner onMessageReceived message.getTopic() --> " + message.getTopic() + " message.getMessage() --> " + message.getMessage().toString() + " TOPIC_SUB --> " + TOPIC_SUB, LogUtil.LOG_FILE_NAME_MQTT);

                if (TOPIC_SUB.equals(message.getTopic())) {
                    String msg = message.getMessage().toString().toLowerCase();
                    LogUtil.d(TAG, "msg --> " + msg + "addReceivedMessageListner mIsWaitResult --> " + mIsWaitResult, LogUtil.LOG_FILE_NAME_MQTT);
                    // 结果传感器消息
                    if (mIsWaitResult && (msg.contains(SENSE_1_RESULT) || msg.contains(SENSE_2_RESULT))) {
                        if (msg.contains(SENSE_1_RESULT + SENSOR_1_GOT) || msg.contains(SENSE_2_RESULT + SENSOR_2_GOT)) {
                            // 传感器消息
                            if (mListener != null) {
                                LogUtil.d(TAG, "addReceivedMessageListner enter suc ", LogUtil.LOG_FILE_NAME_MQTT);
                                //抓取成功，忽略爆仓
                                mListener.onGetResult(true);
                                sendMsgPushR1();
                            }
                            mIsWaitResult = false;
                            mHandler.removeMessages(MSG_GET_RESULT_OVERTIME);

                        }

                    } else {

                        boolean triggerd = false;
                        PubMessage pubMessage = mPubMessages.peek();
                        int[] gr = parseGcmd(msg);
                        if (gr != null) {
                            // g 消息

                            // 也处理一下trigger消息
                            mLastGresult = gr;
                            if (pubMessage != null) {
                                if (checkSwitchWithG(pubMessage.trigger1)) {
                                    pubMessage.trigger1 = null;
                                    triggerd = true;
                                }
                                if (checkSwitchWithG(pubMessage.trigger2)) {
                                    pubMessage.trigger2 = null;
                                    triggerd = true;
                                }
                            }

                        } else {
                            // 限位开关状态 消息
                            if (pubMessage != null) {
                                if (pubMessage.trigger1 != null && msg.contains(pubMessage.trigger1)) {
                                    pubMessage.trigger1 = null;
                                    triggerd = true;
                                }

                                if (pubMessage.trigger2 != null && msg.contains(pubMessage.trigger2)) {
                                    pubMessage.trigger2 = null;
                                    triggerd = true;
                                }
                            }

                        }

                        // 触发了
                        if (triggerd) {
                            sendMsgPreparePub(0, msg, "switch triggered");
                        }
                    }

                } else if (TOPIC_CAM.equals(message.getTopic())) {
                    // 辅助相机消息
                    String msg = message.getMessage().toString();

                    if (System.currentTimeMillis() - mLastCamHandleTime > 1000 || !msg.equals(mLastCamMsg)) {
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_CAM2_HANDLE, message));
                        mLastCamHandleTime = System.currentTimeMillis();
                        mLastCamMsg = msg;
                    } else {
                        LogUtil.d(TAG, "cam miss", LogUtil.LOG_FILE_NAME_MQTT);
                    }

                } else if (TOPIC_SETTING.equals(message.getTopic())) {
                    handleTopicSetting(message);
                } else if (TOPIC_IP_WATCH.equals(message.getTopic())) {
                    handleTopicIpBroad(message);
                } else if (TOPIC_SETTING_ID.equals(message.getTopic())) {
                    handleTopicSettingWithId(message);
                } else if (TOPIC_WATCH_DOG.equals(message.getTopic())) {
                    handleTopicWatchDog(message);
                } else {
                    MqttMsgHandler.getInstance().handlerMsg(
                            message.getTopic(),
                            message.getMessage().toString());
                }
            }
        });

        String[] actionArgs = new String[1];
        actionArgs[0] = sConnection.getId();
        final ActionListener callback = new ActionListener(WawaNewApplication.getInstance(), ActionListener.Action.CONNECT, sConnection, actionArgs) {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                super.onSuccess(asyncActionToken);
                LogUtil.d(TAG, "connect mqtt success", LogUtil.LOG_FILE_NAME_MQTT);
                StreamLogUtil.putLog("connect mqtt success");
                // add subscription
                mHandler.sendEmptyMessage(MSG_ADD_SUB);
//                sendSettingTrigMsg(MqttConstants.TIME_TRIP);
//                mqttReconnect = 0;
            }

            @Override
            public void onFailure(IMqttToken token, Throwable exception) {
                super.onFailure(token, exception);
                LogUtil.d(TAG, "connect mqtt failed exception", LogUtil.LOG_FILE_NAME_MQTT);
                StreamLogUtil.putLog("connect mqtt failed exception");
//                mqttReconnect++;
//                if (mqttReconnect > 10) {
//                    DeviceUtil.reboot("mqtt 连续连接10次失败");
//                }
            }
        };
        sConnection.registerChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                LogUtil.d(TAG, "mqtt propertyChange event.getPropertyName() --> " + event.getPropertyName(), LogUtil.LOG_FILE_NAME_MQTT);
                if (event != null && MqttConstants.ConnectionStatusProperty.equals(event.getPropertyName())) {
                    Connection.ConnectionStatus cstatus = (Connection.ConnectionStatus) event.getNewValue();
                    LogUtil.d(TAG, "cstatus --> " + cstatus, LogUtil.LOG_FILE_NAME_MQTT);
                    if (cstatus == Connection.ConnectionStatus.DISCONNECTED) {
                        // 丢失连接啦
                        // 重连
                        if (System.currentTimeMillis() - mLastTryConncetTime > 1000) {
                            mLastTryConncetTime = System.currentTimeMillis();
                            LogUtil.d(TAG, "disconnect, try to reconnect", LogUtil.LOG_FILE_NAME_MQTT);
                            connect();
                        } else {
                            LogUtil.d(TAG, "disconnect, has tried in 1s", LogUtil.LOG_FILE_NAME_MQTT);
                        }
                    }

                }
            }
        });

        sConnection.getClient().setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                LogUtil.d(TAG, "connection lost", LogUtil.LOG_FILE_NAME_MQTT);

                sConnection.addAction("Connection lost", cause);
                sConnection.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTED);

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                LogUtil.d(TAG, "mqtt messageArrived topic --> " + topic + " message --> " + message, LogUtil.LOG_FILE_NAME_MQTT);
                sConnection.messageArrived(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // do nothing

            }
        });
        try {
            MqttConnectOptions options = new MqttConnectOptions();
//            options.setConnectionTimeout(240000);

            // 30条消息塞着好了...
            options.setMaxInflight(30);
            sConnection.addConnectionOptions(options);
            sConnection.getClient().connect(sConnection.getConnectionOptions(), null, callback);
        } catch (MqttException e) {
            LogUtil.d(TAG, "connection exception" + e.getMessage(), LogUtil.LOG_FILE_NAME_MQTT);
        }
    }

    public void addSubs() {
        CommThreadPool.poolExecute(new Runnable() {
            @Override
            public void run() {
                boolean ret = false;

                ArrayList<Subscription> subs = new ArrayList<Subscription>(0);

                if (PropertyUtil.WAIT_ID) {
                    Subscription sub = new Subscription(TOPIC_SETTING, 0, HANDLE_SUBSCRIPTION, false);
                    subs.add(sub);

                } else {

                    if (!PropertyUtil.CAM2_ONLY) {
                        // 主板子, 连接wawaji board
                        Subscription sub = new Subscription(TOPIC_SUB, 0, HANDLE_SUBSCRIPTION, false);
                        subs.add(sub);
                        Subscription sub4 = new Subscription(TOPIC_SETTING_ID, 0, HANDLE_SUBSCRIPTION, false);
                        subs.add(sub4);
                        Subscription sub5 = new Subscription(TOPIC_WATCH_DOG, 0, HANDLE_SUBSCRIPTION, false);
                        subs.add(sub5);
                    }

                    Subscription sub = new Subscription(TOPIC_CAM, 0, HANDLE_SUBSCRIPTION, false);
                    subs.add(sub);

                    // ips
                    Subscription sub3 = new Subscription(TOPIC_IP_WATCH, 0, HANDLE_SUBSCRIPTION, false);
                    subs.add(sub3);
                }
                subs.add(new Subscription(TOPIC_ANDROID_DEBUG, 0, HANDLE_SUBSCRIPTION, false));
                subs.add(new Subscription(TOPIC_ANDROID_IN, 0, HANDLE_SUBSCRIPTION, false));
                if (!subs.isEmpty()) {
                    ret = true;
                    for (Subscription sub : subs) {
                        boolean sr = false;
                        try {
                            if (TOPIC_CAM.equals(sub.getTopic())) {
                                sConnection.addNewSubscription(sub, new IMqttActionListener() {
                                    @Override
                                    public void onSuccess(IMqttToken asyncActionToken) {
                                        LogUtil.d(TAG, "add sub suc", LogUtil.LOG_FILE_NAME_MQTT);
                                        if (PropertyUtil.CAM2_ONLY) {
                                            mHandler.sendEmptyMessage(MSG_CAM2_WAITING);
                                        } else {
                                            publishCam2Start();
                                        }
                                    }

                                    @Override
                                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                        String msg = null;
                                        if (exception != null) {
                                            msg = exception.getMessage();
                                        }
                                        LogUtil.d(TAG, "add sub failed : " + msg, LogUtil.LOG_FILE_NAME_MQTT);
                                    }
                                });
                            } else {
                                sConnection.addNewSubscription(sub, new IMqttActionListener() {

                                    @Override
                                    public void onSuccess(IMqttToken asyncActionToken) {
                                        LogUtil.d(TAG, "add sub suc", LogUtil.LOG_FILE_NAME_MQTT);
                                    }

                                    @Override
                                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                        String msg = null;
                                        if (exception != null) {
                                            msg = exception.getMessage();
                                        }
                                        LogUtil.d(TAG, "add sub failed " + msg, LogUtil.LOG_FILE_NAME_MQTT);
                                    }
                                });
                            }


                            sr = true;
                        } catch (MqttException e) {
                            LogUtil.d(TAG, "add sub " + sub.getTopic() + " " + e.getMessage(), LogUtil.LOG_FILE_NAME_MQTT);
                            try {
                                sConnection.unsubscribe(sub);
                            } catch (MqttException e2) {
                                e2.printStackTrace();
                            }
                        } catch (NullPointerException e1) {
                            LogUtil.d(TAG, "error" + e1.getMessage(), LogUtil.LOG_FILE_NAME_MQTT);
                            try {
                                sConnection.unsubscribe(sub);
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                        } catch (IllegalArgumentException ex) {
                            //目前什么都不做等待mqtt重连
                        }
                        LogUtil.d(TAG, "add sub " + sub.getTopic() + " = " + sr, LogUtil.LOG_FILE_NAME_MQTT);
                        ret = ret && sr;
                    }
                }


                if (!ret) {
                    mHandler.sendEmptyMessageDelayed(MSG_ADD_SUB, 1000);
                }
            }
        });
    }

    public void publishPropertyInfo() {
        StringBuilder ipInfo = new StringBuilder();
        ipInfo.append("id ")
                .append(PropertyUtil.MQTT_CLAW_MACHINE_ID)
                .append(",cam2 ")
                .append(PropertyUtil.CAM2_ONLY)
                .append(",room ")
                .append(PropertyUtil.ROOM_NUM)
                .append(",ip ")
                .append(NetworkKeeperUtil.getIPAddress(true))
                .append(",live ")
                .append(TencentLiveManager.isStreamOk() ? 1 : 0)
                .append(",version_code ")
                .append(WawaNewApplication.VERSION_CODE)
                .append(",version_name ")
                .append(WawaNewApplication.VERSION_NAME);

        publish(sConnection, MqttConstants.TOPIC_IP_OUT, ipInfo.toString(), 1, false);
    }

    public void publishClawCmd(String cmd, boolean block) {
        ClawPublishListener listener = null;
        if (block) {
            listener = new ClawPublishListener() {
                @Override
                public void onPublishSuccess(String msg) {
                    // publish 成功就行了
                    sendMsgPreparePub(0, "next msg", "block msg success " + msg);
                }

                @Override
                public void onRecvClawOutOk(String msg) {
                    //TODO
                }
            };
        }
        publish(sConnection, TOPIC_PUB, cmd, 1, false, listener);
    }

    public void publishCam2Start() {
        if (MindControllerManager.getSlaveMsg() != null) {
            publish(sConnection, TOPIC_CAM, CMD_CAM_START + ";" + PropertyUtil.ROOM_NUM + ";" + MindControllerManager.getSlaveMsg(), 1, false);
        } else {
            LogUtil.e(TAG, "publish f camera error on slave json = null", LogUtil.LOG_FILE_NAME_MQTT);
        }
    }

    public void publishCam2End() {
        publish(sConnection, TOPIC_CAM, CMD_CAM_END + ";" + MindControllerManager.getSlaveMsg(), 1, false);
    }

    public void publishCam2Wait() {
        publish(sConnection, TOPIC_CAM, CMD_CAM_WAIT + ";", 0, false);
    }

    //一旦一个盒子挂了需要重启，通知另一个也重启，提高恢复成功的概率
    public void publishCamReboot() {
        publish(sConnection, TOPIC_CAM, CMD_CAM_REBOOT + ";", 1, false);
    }

    public void publishCam2On() {
        publish(sConnection, TOPIC_CAM, CMD_CAM_ON + ";", 1, false);
    }

    public void publishRequestCam2LiveUrl() {
        publish(sConnection, TOPIC_CAM, CMD_CAM_REQUEST_LIVE_URL + ";", 1, false);
    }

    public void publishCam2StreamOn(String streamUrl) {
        publish(sConnection, TOPIC_CAM, CMD_STREAM_ON + ";" + streamUrl, 1, false);
    }

    public void publistVersionMsg() {
        publish(sConnection,
                TOPIC_ANDROID_VERSION,
                String.format("%s,version=%s,isCam2=%s,groupid=%s", PropertyUtil.ROOM_NUM + "", AppUtil.getVersionCode(WawaNewApplication.getAppContext()), PropertyUtil.CAM2_ONLY + "", CommSetting.getGroupId()),
                0,
                false);
    }

    public void publistRebootMsg() {
        String cam2 = PropertyUtil.CAM2_ONLY ? "-2" : "-1";
        publish(sConnection,
                TOPIC_ANDROID_REBOOT,
                PropertyUtil.ROOM_NUM + cam2 + ",reboot",
                0,
                false);
    }

    public void publishTiancheWarn(String errorMsg) {
//        publish(sConnection, TOPIC_DOG_REPORT,
//                "machine_warn," + PropertyUtil.MQTT_CLAW_MACHINE_ID + "," + StateHelper.getInstance().getSessionId() + " " + StateHelper.getInstance().getCurUserName() + "," + errorMsg,
//                0, false);
    }

    public void publishStatusChecker(String msg) {
        LogUtil.d(TAG, TOPIC_ANDROID_STATUS + ":" + msg, LogUtil.LOG_FILE_NAME_MQTT);
        if (sConnection != null && sConnection.isConnected()) {
            publish(sConnection, TOPIC_ANDROID_STATUS, msg, 0, false);
        }
    }

    public void publishRebootChecker(String msg) {
        LogUtil.d(TAG, TOPIC_ANDROID_REBOOT_REASON + ":" + msg, LogUtil.LOG_FILE_NAME_MQTT);
        if (sConnection != null && sConnection.isConnected()) {
            publish(sConnection, TOPIC_ANDROID_REBOOT_REASON, msg, 0, false);
        }
    }

    /**
     * 爪子向下断电保护时间
     *
     * @param time
     */
    public void publishSettingZp(int time) {
        StreamLogUtil.putLog(TOPIC_PUB + " publishSettingZp:" + time);
        LogUtil.d(TAG, "publishSettingZp:" + time);
        if (sConnection != null && sConnection.isConnected())
            publish(sConnection, TOPIC_PUB, "zp," + time, 1, false);
    }


    private void publish(Connection connection, String topic, final String message, int qos, boolean retain) {
        publish(connection, topic, message, qos, retain, null);
    }

    private void publish(Connection connection, String topic, final String message, int qos, boolean retain, final ClawPublishListener listener) {

        LogUtil.d(TAG, String.format("publish topic = %s , message = %s , qos = %s , retain = %b", topic, message, qos, retain));

        if (connection == null || connection.getClient() == null || message == null) {
            LogUtil.e(TAG, "mqtt connection = null on publish : " + message, LogUtil.LOG_FILE_NAME_MQTT);
        } else if (!connection.isConnected()) {
            LogUtil.e(TAG, "mqtt publish fail on unconnected: " + message, LogUtil.LOG_FILE_NAME_MQTT);
        } else {
            try {
                String[] actionArgs = new String[2];
                actionArgs[0] = message;
                actionArgs[1] = topic;
                final ActionListener callback = new ActionListener(WawaNewApplication.getInstance(),
                        ActionListener.Action.PUBLISH, connection, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        if (listener != null) {
                            listener.onPublishSuccess(message);
                        }
                        LogUtil.d(TAG, "publish mqtt msg success:" + message, LogUtil.LOG_FILE_NAME_MQTT);
                        publishFailedCount = 0;
                        if (message != null && (message.startsWith("x") || message.startsWith("y") || message.startsWith("p"))) {
                            StreamLogUtil.putLog("msg --> " + message + " suc");
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        String throwable = exception == null ? "throwable is null" : exception.getMessage();
                        sInstance.publishTiancheWarn("publish fail " + message + " <" + throwable + ">");
                        LogUtil.d(TAG, "publish mqtt fail : " + message + " <" + throwable + ">", LogUtil.LOG_FILE_NAME_MQTT);
                        if (message != null && (message.startsWith("x") || message.startsWith("y") || message.startsWith("p"))) {
                            StreamLogUtil.putLog("msg --> " + message + " fail");
                        }
                        publishFailedCount++;
                        if (publishFailedCount > 15) {
                            DeviceUtil.reboot("连续15个mqtt消息发送失败，重启");
                        }
                    }
                }, actionArgs);

                connection.getClient().publish(topic, message.getBytes(), qos, retain, null, callback);
            } catch (MqttException ex) {
                LogUtil.e(TAG, "Exception occurred during publish " + ex.getMessage(), LogUtil.LOG_FILE_NAME_MQTT);
            } catch (NullPointerException ex) {
                LogUtil.e(TAG, "Exception occurred during publish " + ex.getMessage(), LogUtil.LOG_FILE_NAME_MQTT);
            } catch (IllegalArgumentException ex) {
                LogUtil.e(TAG, "Exception occurred during publish " + ex.getMessage(), LogUtil.LOG_FILE_NAME_MQTT);
            }
        }
    }

    private void handleTopicIpBroad(ReceivedMessage message) {
        String msg = message.getMessage().toString();
        StreamLogUtil.putLog("ip broadcast: " + msg);
        if (msg.startsWith("all")) {
            publishPropertyInfo();
        }

        if (msg.startsWith("m,")) {
            int id = Integer.valueOf(msg.substring(2));
            if (PropertyUtil.MQTT_CLAW_MACHINE_ID == id) {
                publishPropertyInfo();
            }
        }
    }

    private void handleTopicCamMessage(ReceivedMessage message) {
        String msg = message.getMessage().toString();
        StreamLogUtil.putLog("topic cam: " + msg);
        String[] ss = msg.split(";");
        String cmd = ss[0];
        if (PropertyUtil.CAM2_ONLY) {
            // 消息
            if (CMD_CAM_START.equals(cmd)) {
                if (ss.length != 3) {
                    LogUtil.e(TAG, "cam2 message error" + msg, LogUtil.LOG_FILE_NAME_MQTT);
                } else {
                    if (PropertyUtil.CAM2_ONLY) {
                        PropertyUtil.ROOM_NUM = Integer.valueOf(ss[1]);
                        StreamLogUtil.putLog("cam2 topic read room " + PropertyUtil.ROOM_NUM);
                    }

                    JsonObject ob = new JsonParser().parse(ss[2]).getAsJsonObject();

                    final String id = ob.get("identifier").getAsString();
                    final String sig = ob.get("signature").getAsString();

                    if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(sig)) {
                        MqttConstants.STATUS_LIVE_ID_GET_SUC_CAM2 = true;
                    }

                    ILiveSDK.getInstance().runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            TencentLiveManager.startCam2Push(PropertyUtil.ROOM_NUM, id, sig);
                        }
                    }, 0);

                }

            } else if (CMD_CAM_END.equals(cmd)) {
                TencentLiveManager.endCam2Push();
            } else if (CMD_CAM_REQUEST_LIVE_URL.equals(cmd)) {
                String url = MindControllerManager.getCurStreamLiveUrl();
                if (!TextUtils.isEmpty(url)) {
                    publishCam2StreamOn(MindControllerManager.getCurStreamLiveUrl());
                } else {
                    LogUtil.d("cam2", "CMD_CAM_REQUEST_LIVE_URL but url not ready", LogUtil.LOG_FILE_NAME_MQTT);
                }
            }
        } else {
            // cam1 wait cmd
            if (CMD_CAM_WAIT.equals(ss[0])) {
                LogUtil.d("kkkkkkkk", "cam1 publishCam2Start", LogUtil.LOG_FILE_NAME_MQTT);
                publishCam2Start();
            } else if (CMD_CAM_ON.equals(ss[0])) {
//                StateHelper.getInstance().onCam2On();
            } else if (CMD_STREAM_ON.equals(ss[0])) {
                MindControllerManager.setCam2StreamLiveUrl(ss[1]);
            }

        }

        if (CMD_CAM_REBOOT.equals(ss[0])) {
            DeviceUtil.reboot("另一个盒子重启了");
        }
    }

    private void handleTopicSetting(ReceivedMessage message) {
        String msg = message.getMessage().toString();
        StreamLogUtil.putLog("topic setting: " + msg);

        if (msg.startsWith("id,")) {
            int id = Integer.valueOf(msg.substring(3));
            if (id == 21) {
                //id== 21 一直在发这个setting消息 21有毒
                StreamLogUtil.putLog("id,21 直接return");
                return;
            }
            CommSetting.setMachineId(id);
        } else if (msg.startsWith("cam2,")) {
            int flag = Integer.valueOf(msg.substring(5));
            StreamLogUtil.putLog("cam2 set to " + flag);
            CommSetting.setCam2Only(flag == 1);
        } else if (msg.startsWith("done")) {
            PropertyUtil.writeDone();
        }

    }

    private void handleTopicSettingWithId(ReceivedMessage message) {
        String msg = message.getMessage().toString();
        StreamLogUtil.putLog("topic setting: " + msg);

        if (msg.startsWith("id,")) {
            int id = Integer.valueOf(msg.substring(3));
            CommSetting.setMachineId(id);
        } else if (msg.startsWith("cam2,")) {
            int flag = Integer.valueOf(msg.substring(5));
            StreamLogUtil.putLog("cam2 set to " + flag);
            CommSetting.setCam2Only(flag == 1);
        } else if (msg.startsWith("bf,")) {
            int flag = Integer.valueOf(msg.substring(3));
            CommSetting.setBackFront(flag);
        } else if (msg.startsWith("il,")) {
            int flag = Integer.valueOf(msg.substring(3));
            CommSetting.setInitLoc(flag);
        } else if (msg.startsWith("s1,")) {
            int flag = Integer.valueOf(msg.substring(3));
            CommSetting.setSensor1Settled(flag == 1);
        } else if (msg.startsWith("s2,")) {
            int flag = Integer.valueOf(msg.substring(3));
            CommSetting.setSensor2Settled(flag == 1);
        } else if (msg.startsWith("sd,")) {
            int flag = Integer.valueOf(msg.substring(3));
            CommSetting.setDownDown(flag == 1);
        } else if (msg.startsWith("s1f,")) {
            int flag = Integer.valueOf(msg.substring(4));
            CommSetting.setSensor1Reverse(flag == 1);
        } else if (msg.startsWith("s2f,")) {
            int flag = Integer.valueOf(msg.substring(4));
            CommSetting.setSensor2Reverse(flag == 1);
        } else if (msg.startsWith("sb,")) {
            int flag = Integer.valueOf(msg.substring(3));
            CommSetting.setSandBox(flag == 1);
        } else if (msg.startsWith("flive,")) {
            int flag = Integer.valueOf(msg.substring(6));
            CommSetting.setEnableFastLive(flag == 1);
        }

    }

    private void handleTopicWatchDog(ReceivedMessage message) {
        String msg = message.getMessage().toString();
        StreamLogUtil.putLog("topic watch dog: " + msg);
        String[] ss = msg.split(",");
        if (msg.startsWith("warn")) {
            // cam2 restarting
            if ("cam2 restarting".equals(ss[1])) {
//                StateHelper.getInstance().onCam2Restarting();
            }

        } else if (msg.startsWith("error")) {
            if ("cam2 missing".equals(ss[1])) {
//                StateHelper.getInstance().onCam2Missing();
            }
        }

    }

    public Connection getConnection() {
        return sConnection;
    }

    public boolean checkConnect() {
        if (!MqttManager.getInstance().isConnected()) {
            LogUtil.d(TAG, "beat check connect : disconnect, try to reconnect", LogUtil.LOG_FILE_NAME_MQTT);
            MqttManager.getInstance().connect();
        }

        return MqttManager.getInstance().isConnected();
    }

    private void handMessage(Message message) {
        int what = message.what;
        LogUtil.d(TAG, "mqtt handMessage what --> " + what);

        boolean newHead = false;
        int nextMsgDelay = 0;
        if (what == MSG_PREPARE_PUB) {
            // 找个头
            PubMessage pm = mPubMessages.peek();

            if (pm != null) {
                boolean islimithand = false;
                if (pm.minGip > 0 && pm.inTime == 0) {
                    pm.inTime = System.currentTimeMillis();
                    LogUtil.d(TAG, "gap to set intime = " + pm.inTime + ", " + pm.msg, LogUtil.LOG_FILE_NAME_MQTT);
                }

                if (pm.limit > 0) {
                    if (pm.inTime == 0) {
                        pm.inTime = System.currentTimeMillis();
                        // 这里是不是有bug
                        sendMsgPreparePub(pm.limit, pm.msg, "limit to set in time = " + pm.inTime);
//                        SessionFileLog.log(StateHelper.getInstance().getSessionId(), "GAP", "limit to set intime = " + pm.inTime + ", " + pm.msg);

                    } else {
                        long gip = pm.limit - (System.currentTimeMillis() - pm.inTime);
                        if (gip <= 0) {
                            sendMsgPubNow(pm.msg);
                            LogUtil.d(TAG, "limit chu fa" + ", " + pm.msg, LogUtil.LOG_FILE_NAME_MQTT);
                            sInstance.publishTiancheWarn("time limit touch " + pm.limit + " of " + pm.msg);
                            islimithand = true;
                        } else {
                            sendMsgPreparePub(gip, pm.msg, "limit send delay " + gip);
//                            SessionFileLog.log(StateHelper.getInstance().getSessionId(), "GAP", "limit send delay " + gip + ", " + pm.msg);
                        }
                    }
                }

                if (!islimithand) {
                    if (pm.trigger1 != null || pm.trigger2 != null) {
                        // do nothing, 由 trigger 触发处理
                        // 触发后, trigger 只为null, 然后处理delay
                    } else if (pm.delay > 0) {
                        // 延时消息
                        sendMsgPreparePub(pm.delay, pm.msg, "delay msg in " + pm.delay);
                        pm.delay = 0;
//                        SessionFileLog.log(StateHelper.getInstance().getSessionId(), "GAP", "delay msg in " + pm.delay + ", " + pm.msg);
                    } else if (pm.minGip > 0) {
                        long gip = pm.minGip - (System.currentTimeMillis() - pm.inTime);
                        if (gip <= 0) {
                            // 直接处理这条消息
                            sendMsgPubNow(pm.msg);
                            LogUtil.d(TAG, "min gap legal " + pm.minGip + ", " + pm.msg, LogUtil.LOG_FILE_NAME_MQTT);
                        } else {
                            sendMsgPreparePub(gip, pm.msg, "min gap, chu fa, delay " + gip);
//                            SessionFileLog.log(StateHelper.getInstance().getSessionId(), "GAP", "min gap, chu fa, delay " + gip + ", " + pm.msg);
                        }
                    } else {
                        sendMsgPubNow(pm.msg);
                        LogUtil.d(TAG, "gogogogo " + pm.msg, LogUtil.LOG_FILE_NAME_MQTT);
                    }
                }

            }
        } else if (what == MSG_PUB_NOW) {
            PubMessage pm = mPubMessages.poll();
            if (pm != null) {
                String msg = pm.msg;

                LogUtil.d(TAG, "poll msg " + pm.msg + ", should be " + message.obj, LogUtil.LOG_FILE_NAME_MQTT);

                publishClawCmd(msg, pm.block);
                newHead = true;
                // 最多3s, 处理下一条消息
                if (pm.block) {
                    nextMsgDelay = 3000;
                }

                if (pm.gameEnd) {
                    LogUtil.d(TAG, "MSG_GET_RESULT_OVERTIME false", LogUtil.LOG_FILE_NAME_MQTT);
                    mHandler.sendEmptyMessageDelayed(MSG_GET_RESULT_OVERTIME, GET_RESULT_OVERTIME);
                }
                if (pm.hasClawed) {
                    // 抓了500ms后开始统计结果
                    mHandler.sendEmptyMessageDelayed(MSG_WAIT_RESULT, 100);
                }
            }

        } else if (what == MSG_GET_RESULT_OVERTIME) {
            if (mIsWaitResult) {
                mIsWaitResult = false;
                if (mListener != null) {
                    mListener.onGetResult(false);
                }
            }
        } else if (what == MSG_ADD_SUB) {
            addSubs();
        } else if (what == MSG_WAIT_RESULT) {
            mIsWaitResult = true;
        } else if (what == MSG_CAM2_HANDLE) {
            // cam2 用
            ReceivedMessage msg = (ReceivedMessage) message.obj;
            handleTopicCamMessage(msg);
        } else if (what == MSG_CAM2_WAITING) {

            if (TencentLiveManager.isCam2Pushing()) {
                // is pushing, stop reboot
            } else {
                publishCam2Wait();
                mHandler.sendEmptyMessageDelayed(MSG_CAM2_WAITING, 5000);
            }
        } else if (what == MSG_R1_CMD) {
            //
            int yiorling = message.arg1;
            publishClawCmd("r,1," + yiorling, false);

        } else if (what == MSG_SETTING_TRIG) {
            int time = message.arg1;
            publishSettingZp(time);
        }
        if (newHead) {
            // 处理完了一条消息; 立马开始第二条
            sendMsgPreparePub(nextMsgDelay, "next msg", "last pubed");

        }
    }

    public void startWaitResult() {
        mHandler.sendEmptyMessageDelayed(MSG_WAIT_RESULT, 100);
    }

    private void sendMsgPreparePub(long delay, String msg, String reason) {
        LogUtil.d(TAG, "sendMsgPreparePub prepare " + msg + " in " + delay + "ms, " + reason, LogUtil.LOG_FILE_NAME_MQTT);
        mHandler.removeMessages(MSG_PREPARE_PUB);
        if (delay > 0) {
            mHandler.sendEmptyMessageDelayed(MSG_PREPARE_PUB, delay);
        } else {
            mHandler.sendEmptyMessage(MSG_PREPARE_PUB);
        }
    }

    private void sendMsgPushR1() {
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_R1_CMD, 1, 0), 1000);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_R1_CMD, 0, 0), 8000);
    }

    private void sendMsgPubNow(String msg) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_PUB_NOW, msg));
    }


    public boolean handleNewControlMessage(String control, long force) {
        boolean ret = false;
        LogUtil.d(TAG, "control " + control + ", connected " + sConnection.isConnected(), LogUtil.LOG_FILE_NAME_MQTT);

        StreamLogUtil.putLog("收到控制指令 control --> " + control + " force --> " + force + " connected --> " + sConnection.isConnected());
        if (sConnection.isConnected()) {
            //
            boolean isCombine = false;

            PubMessage msg = new PubMessage();


            switch (control) {
                case CONTROL_FORWARD_START_NEW:
                    msg.msg = "y," + ORI_Y_FRONT + SPEED;
                    break;
                case CONTROL_FORWARD_END_NEW:
                    msg.msg = "y,0";
                    break;
                case CONTROL_BACKWARD_START_NEW:
                    msg.msg = "y," + ORI_Y_BACK + SPEED;
                    break;
                case CONTROL_BACKWARD_END_NEW:
                    msg.msg = "y,0";
                    break;
                case CONTROL_LEFT_START_NEW:
                    msg.msg = "x," + ORI_X_BACK + SPEED;
                    break;
                case CONTROL_LEFT_END_NEW:
                    msg.msg = "x,0";
                    break;
                case CONTROL_RIGHT_START_NEW:
                    msg.msg = "x," + ORI_X_FRONT + SPEED;
                    break;
                case CONTROL_RIGHT_END_NEW:
                    msg.msg = "x,0";
                    break;
                case CONTROL_TAKE_NEW:
//                    msg = null;

                    LogUtil.d(TAG, "clawForce --> " + force, LogUtil.LOG_FILE_NAME_MQTT);
//                    createTakeCmds(MqttConstants.getClawMode(), (int) force);
                    StreamLogUtil.putLog("clawForce --> " + force);

                    mHandler.sendEmptyMessageDelayed(MSG_GET_RESULT_OVERTIME, GET_RESULT_OVERTIME);
                    msg.msg = "p," + force;

                    break;

                default:
                    msg = null;
                    break;
            }

            if (msg != null) {
                LogUtil.d(TAG, "offer msg " + control + " to " + msg.msg, LogUtil.LOG_FILE_NAME_MQTT);
                mPubMessages.offer(msg);
            }
            ret = true;
            sendMsgPreparePub(0, msg == null ? "claw msgs" : msg.msg, "new tcp in");
        }

        return ret;
    }


    private static int[] parseGcmd(String msg) {
//        MqttKeeper.log("parse g cmd " + msg);
//        g;0,-100,0,100,1,0,0,0,1,0,0,0,
        int[] ret = null;
        if (msg != null) {
            int s = msg.toLowerCase().indexOf("g;");
//            MqttKeeper.log("parse g cmd s = " + s);
            if (s >= 0) {
                String rets = msg.substring(s + 2);
                String[] ss = rets.split(",");
//                MqttKeeper.log("parse g cmd ss = " + Arrays.toString(ss));
                if (ss.length == G_LENGTH) {
                    ret = new int[G_LENGTH];

                    for (int i = 0; i < G_LENGTH; i++) {
                        ret[i] = Integer.valueOf(ss[i]);
                    }
//                    MqttKeeper.log(msg + " ->" + Arrays.toString(ret));
                }
            }
        }
        return ret;
    }


    /**
     * @param reg
     * @return
     */
    private boolean checkSwitchWithG(String reg) {
//        MqttKeeper.log("checkSwitchWithG ->" + reg);
        LogUtil.d(TAG, "checkSwitchWithG -> " + reg, LogUtil.LOG_FILE_NAME_MQTT);

        // x_1;1
        boolean match = false;
        String name = null;
        int flag = -1;
        if (reg != null) {
            String[] args = reg.split(";");
            if (args.length == 2) {
                name = args[0];
                flag = Integer.valueOf(args[1]);
            }
        }
        if (name != null) {
//            MqttKeeper.log("checkSwitchWithG ->" + name + ", " + flag);
            for (int i = 0; i < G_LENGTH; i++) {
//                MqttKeeper.log("checkSwitchWithG ->" + G_SWITCH_NAMES[i] + ", " + mLastGresult[i]);
                if (name.equals(G_SWITCH_NAMES[i])) {
                    // 匹配
                    if (flag == mLastGresult[i]) {
                        match = true;
                        break;
                    }
                }
            }
        }
        return match;
    }

    private boolean checkIsStackFull(int gresult[]) {
        boolean ret = false;

        if (PropertyUtil.SENSOR_1_SETTERED && gresult[G_RAZ_1] == SENSOR_1_GOT) {
            ret = true;
        } else if (PropertyUtil.SENSOR_2_SETTERED && gresult[G_RAZ_2] == SENSOR_2_GOT) {
            ret = true;
        }

        return ret;
    }

    public int getLastForce() {
        return mLastForce;
    }

    public void clearCmds() {
        mPubMessages.clear();
    }

    public void checkStuckFull() {
        mIsCheckStuck = true;
        runG();
    }

    public void runG() {
        LogUtil.d(TAG, "run g", LogUtil.LOG_FILE_NAME_MQTT);

        // 松爪
        PubMessage pm = new PubMessage();
        pm.msg = "g";
        mPubMessages.offer(pm);

        sendMsgPreparePub(0, pm.msg, "run g");
    }

    public void sendSettingTrigMsg(int time) {
        Message msg = mHandler.obtainMessage(MSG_SETTING_TRIG);
        msg.arg1 = time;
        mHandler.sendMessage(msg);
    }

    /**
     * 归位命令
     */
    public void runResetCmds() {
        LogUtil.d("mqtt", "create reset cmds", LogUtil.LOG_FILE_NAME_MQTT);

        // 松爪
        PubMessage pm = new PubMessage();
        pm.msg = "c,0";
        mPubMessages.offer(pm);

        // 提爪
        pm = new PubMessage();
        pm.msg = "z,-" + SPEED;
        mPubMessages.offer(pm);

        // 归位
        PubMessage pm1 = new PubMessage();
        PubMessage pm2 = new PubMessage();
        pm1.msg = "x,";
        pm2.msg = "y,";

        int initLoc = MachineProperties.MACHINE_INIT_LOC;

        if (initLoc / 2 == 0) {
            // left
            pm1.msg += "-" + SPEED;
        } else {
            // right
            pm1.msg += SPEED;
        }

        if (initLoc % 2 == 0) {
            // backword
            pm2.msg += ORI_Y_BACK + SPEED;

        } else {
            // foreword
            pm2.msg += ORI_Y_FRONT + SPEED;
        }

        mPubMessages.offer(pm1);
        mPubMessages.offer(pm2);

        sendMsgPreparePub(0, "reset messages", "run reset msg");
    }

    private void createTakeCmds(int mode, int f) {
        LogUtil.d(TAG, "take with mode " + mode + ", force = " + f, LogUtil.LOG_FILE_NAME_MQTT);
        if (mode == MqttConstants.CLAW_MODE_TAKE_RELEASE) {
            createClawReleaseTakeCmds(f);
        } else if (mode == MqttConstants.CLAW_MODE_TOP_RELEASE) {
            createTopReleaseTakeCmds(f);
        }
    }

    /**
     * 100爪,到顶了松
     *
     * @param f
     */
    private void createTopReleaseTakeCmds(int f) {
        mLastForce = f;
        StreamLogUtil.putLog("createTopReleaseTakeCmds take with force " + f);
        // x,y 清零
        PubMessage pm = new PubMessage();
        pm.msg = "x,0";
        mPubMessages.offer(pm);

        pm = new PubMessage();
        pm.msg = "y,0";
        mPubMessages.offer(pm);


        // 下爪
        pm = new PubMessage();
        pm.msg = "z," + SPEED;
        pm.block = true;
        mPubMessages.offer(pm);

        // 到底,爪,100
        pm = new PubMessage();
        pm.trigger1 = "z_2;1";
        pm.msg = "c," + MAX_CLAW;
        pm.delay = 200; // 下爪后,500ms后抓
        pm.minGip = MIN_DOWN_CLAWTIME;
        pm.limit = MAX_DOWN_CLAWTIME; // 下爪后最多等待3s就抓
        mPubMessages.offer(pm);

        // 提爪
        pm = new PubMessage();
        pm.msg = "z,-" + SPEED;
        pm.hasClawed = true;    // 题抓后500ms开始统计结果
        mPubMessages.offer(pm);


        // 到顶了,挪回去
        PubMessage pm1 = new PubMessage();
        PubMessage pm2 = new PubMessage();

        // 移动的时候发个g命令,看看最后要触发几个限位开关
        PubMessage g = new PubMessage();
        g.msg = "g";
        // 假松,弱抓力
        PubMessage fake = null;
        if (f < 100) {
            fake = new PubMessage();
            fake.msg = "c," + f;
        }

        pm1.msg = "x,";
        pm2.msg = "y,";
        pm1.trigger1 = "z_1;1";
        pm1.minGip = 2000;
        pm1.limit = 2500;

        // 松爪, 最后一个命令
        PubMessage pm3 = new PubMessage();
        pm3.limit = 4000;

        int initLoc = MachineProperties.MACHINE_INIT_LOC;

        int backSpeed = SPEED * 100 / 100;
        if (initLoc / 2 == 0) {
            // left
            pm1.msg += "-" + backSpeed;
            pm3.trigger1 = "x_1;1";
        } else {
            // right
            pm1.msg += backSpeed;
            pm3.trigger1 = "x_2;1";
        }

        if (initLoc % 2 == 0) {
            // backword
            pm2.msg += ORI_Y_BACK + backSpeed;
            pm3.trigger2 = SWITCH_Y_BACK + ";1";

        } else {
            // foreword
            pm2.msg += ORI_Y_FRONT + backSpeed;
            pm3.trigger2 = SWITCH_Y_FRONT + ";1";
        }

        mPubMessages.offer(pm1);
        mPubMessages.offer(pm2);

        // 松隔爪
        if (fake != null) {
            mPubMessages.offer(fake);
        }

        // 更假了
//        if (f < 20) {
//            // 恢复抓力
//            PubMessage back = new PubMessage();
//            back.msg = "c,100";
//            back.delay = 400;
//            mPubMessages.offer(back);
//        }
        // 发个g命令
        mPubMessages.offer(g);


        if (PropertyUtil.RELEASE_DOWN_DOWN) {
            pm3.msg = "z,100";
            mPubMessages.offer(pm3);


            PubMessage pmm = new PubMessage();
            pmm.msg = "z,-100";
            pmm.delay = MIN_DOWN_CLAWTIME;
            mPubMessages.offer(pmm);

            pmm = new PubMessage();
            pmm.msg = "c,0";
            pmm.gameEnd = true;
            mPubMessages.offer(pmm);

        } else {
            pm3.msg = "c,0";
            pm3.gameEnd = true;
            // 挪回到原位, 松爪
            mPubMessages.offer(pm3);
        }
    }


    /**
     * 100抓,抓完隔几百毫秒松,然后再紧
     *
     * @param f
     */
    private void createClawReleaseTakeCmds(int f) {
        mLastForce = f;
        StreamLogUtil.putLog("createClawReleaseTakeCmds take with force " + f);
        // 下爪
        PubMessage pm = new PubMessage();
        pm.msg = "z," + SPEED;
        mPubMessages.offer(pm);

        // 到底,爪,100
        pm = new PubMessage();
        pm.trigger1 = "z_2;1";
        pm.msg = "c," + MAX_CLAW;
        pm.minGip = MIN_DOWN_CLAWTIME; // 下爪后最多等待3s就抓
        pm.limit = MAX_DOWN_CLAWTIME; // 下爪后最多等待3s就抓
        mPubMessages.offer(pm);

        // 提爪
        pm = new PubMessage();
        pm.msg = "z,-" + SPEED;
        pm.hasClawed = true;
        mPubMessages.offer(pm);

        // 松松哒
        if (f < MAX_CLAW) {
            pm = new PubMessage();
            pm.delay = MachineProperties.CLAW_TO_FAKE_TIME;
            pm.msg = "c," + f;
            mPubMessages.offer(pm);
            // 恢复抓力
            pm = new PubMessage();
            pm.delay = MachineProperties.CLAW_FAKE_TIME;
            pm.msg = "c," + MAX_CLAW;
            mPubMessages.offer(pm);
        }

        // 到顶了,挪回去
        PubMessage pm1 = new PubMessage();
        PubMessage pm2 = new PubMessage();

        // 移动的时候发个g命令,看看最后要触发几个限位开关
        PubMessage g = new PubMessage();
        g.msg = "g";

        // 松爪, 最后一个命令
        PubMessage pm3 = new PubMessage();
        pm1.msg = "x,";
        pm2.msg = "y,";
        pm1.trigger1 = "z_1;1";
        pm1.limit = 2000;
        pm3.msg = "c,0";
        pm3.limit = 4000;
        pm3.gameEnd = true;
        int initLoc = MachineProperties.MACHINE_INIT_LOC;
        if (initLoc / 2 == 0) {
            // left
            pm1.msg += "-" + SPEED;
            pm3.trigger1 = "x_1;1";
        } else {
            // right
            pm1.msg += SPEED;
            pm3.trigger1 = "x_2;1";
        }
        if (initLoc % 2 == 0) {
            // backword
            pm2.msg += ORI_Y_BACK + SPEED;
            pm3.trigger2 = SWITCH_Y_BACK + ";1";
        } else {
            // foreword
            pm2.msg += ORI_Y_FRONT + SPEED;
            pm3.trigger2 = SWITCH_Y_FRONT + ";1";
        }
        mPubMessages.offer(pm1);
        mPubMessages.offer(pm2);

        // 发个g命令
        mPubMessages.offer(g);

        // 挪回到原位
        mPubMessages.offer(pm3);
    }

}
