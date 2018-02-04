package renren.wawabox.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import renren.wawabox.bean.RoomStatusBean;
import renren.wawabox.bean.UpdateBean;
import renren.wawabox.config.Constants;
import renren.wawabox.json.JSONFormatExcetion;
import renren.wawabox.json.JSONToBeanHandler;
import renren.wawabox.net.IRequestCallback;
import renren.wawabox.net.RequestFactory;
import renren.wawabox.utils.AppUtil;
import renren.wawabox.utils.LogUtil;
import renren.wawabox.utils.PropertyUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static renren.wawabox.config.Constants.IS_ENABLE_UPDATE;
import static renren.wawabox.config.Constants.REQUEST_URL;
import static renren.wawabox.update.AppUpdateManager.REQUEST_INSTALL_LOCAL_ACTION;
import static renren.wawabox.update.AppUpdateManager.REQUEST_INSTALL_REMOTE_ACTION;
import static renren.wawabox.update.AppUpdateManager.REQUEST_UPDATE_LOCAL_ACTION;
import static renren.wawabox.update.AppUpdateManager.REQUEST_UPDATE_REMOTE_ACTION;

/**
 * 收到这个广播后，请求最新的APP更新
 * Created by xunwang on 2017/8/29.
 */

public class UpdateReceiver extends BroadcastReceiver {
    public static final String APK_PATH = Environment.getExternalStorageDirectory() + "/wawa-android.apk";
    private static final String REQUEST_BODY_KEY = "request_body_key";
    private static final String DOWNLOAD_URL_KEY = "download_url_key";
    private static final String INSTALL_PACKAGE_NAME = "com.xun.testinstall";
    public static final String STATUS_URL = Constants.BASE_URL + "wwj/status";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (IS_ENABLE_UPDATE) {
            if (REQUEST_UPDATE_LOCAL_ACTION.equals(intent.getAction())) {
                LogUtil.d(AppUpdateManager.TAG, "收到需要请求app更新的请求", LogUtil.LOG_FILE_NAME_NORMAL);
                OkHttpClient okHttpClient = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(REQUEST_URL)
                        .build();
                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        LogUtil.d(AppUpdateManager.TAG, "e.getMessage() --> " + e.getMessage(), LogUtil.LOG_FILE_NAME_NORMAL);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String bodyString = response.body().string();
                        LogUtil.d(AppUpdateManager.TAG, "response --> " + bodyString, LogUtil.LOG_FILE_NAME_NORMAL);

                        try {
                            UpdateBean updateBean = JSONToBeanHandler.fromJsonString(bodyString, UpdateBean.class);
                            if (updateBean == null || updateBean.getData() == null) {
                                return;
                            }
                            //需要mqtt服务器和对得上才下载
                            String downloadUrl = getUrlWithMqtt(updateBean.getData().getDownload());
                            LogUtil.d(AppUpdateManager.TAG, "downloadUrl --> " + downloadUrl, LogUtil.LOG_FILE_NAME_NORMAL);
                            if (downloadUrl == null) {
                                return;
                            }
                            //有指定Special字段
                            LogUtil.d(AppUpdateManager.TAG, "isSpecialCanContinue(updateBean.getData().getSpecial()) --> " + isSpecialCanContinue(updateBean.getData().getSpecial()), LogUtil.LOG_FILE_NAME_NORMAL);
                            if (!isSpecialCanContinue(updateBean.getData().getSpecial())) {
                                return;
                            }
                            //判断版本号再发出这个下载指令
                            LogUtil.d(AppUpdateManager.TAG, "AppInfoUtil.getVersionCode(context) --> " + AppUtil.getVersionCode(context) + " updateBean.getData().getVersionCode() --> " + updateBean.getData().getVersionCode() + " bodyString --> " + bodyString, LogUtil.LOG_FILE_NAME_NORMAL);
                            if (AppUtil.getVersionCode(context) < updateBean.getData().getVersionCode()) {
                                if(AppUtil.fileIsExists(APK_PATH)){
                                    AppUtil.deleteFile(APK_PATH);
                                }

                                LogUtil.d(AppUpdateManager.TAG, "发出下载指令 updateBean.getData().getSelfCode() + \",\" + updateBean.getData().getSelfDownload() --> " + (updateBean.getData().getSelfCode() + "," + updateBean.getData().getSelfDownload()), LogUtil.LOG_FILE_NAME_NORMAL);

                                startTestInstallApp(context);

                                Intent toRemoteIntent = new Intent();
                                toRemoteIntent.setAction(REQUEST_UPDATE_REMOTE_ACTION);
                                toRemoteIntent.putExtra(DOWNLOAD_URL_KEY, downloadUrl);
                                toRemoteIntent.putExtra(REQUEST_BODY_KEY, bodyString);
                                context.sendBroadcast(toRemoteIntent);

                            }
                        } catch (JSONFormatExcetion jsonFormatExcetion) {
                            jsonFormatExcetion.printStackTrace();
                        }
                    }
                });
            } else if (REQUEST_INSTALL_LOCAL_ACTION.equals(intent.getAction())) {
                LogUtil.d(AppUpdateManager.TAG, "收到安装指令 ", LogUtil.LOG_FILE_NAME_NORMAL);
                HashMap<String, String> params = new HashMap<>();
                params.put("token", PropertyUtil.TOKEN);
                params.put("room_id", String.valueOf(PropertyUtil.MQTT_CLAW_MACHINE_ID));
                RequestFactory.getRequestManager(RequestFactory.RequestType.OKHTTP).post(STATUS_URL, AppUtil.hashMapToJson(params), new IRequestCallback() {
                    @Override
                    public void onSuccess(String response) {
                        LogUtil.d(AppUpdateManager.TAG, "RoomStatusBean response --> " + response, LogUtil.LOG_FILE_NAME_NORMAL);

                        try {
                            RoomStatusBean roomStatusBean = JSONToBeanHandler.fromJsonString(response, RoomStatusBean.class);
                            if (roomStatusBean != null && roomStatusBean.getData() != null && roomStatusBean.getData().getStatus() == 0) {
                                if (AppUtil.fileIsExists(APK_PATH)) {
                                    startTestInstallApp(context);

                                    LogUtil.d(AppUpdateManager.TAG, "娃娃机未运行，发出安装指令");
                                    Intent toRemoteInstallIntent = new Intent();
                                    toRemoteInstallIntent.setAction(REQUEST_INSTALL_REMOTE_ACTION);
                                    context.sendBroadcast(toRemoteInstallIntent);
                                }
                            }
                        } catch (JSONFormatExcetion jsonFormatExcetion) {
                            jsonFormatExcetion.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LogUtil.d(AppUpdateManager.TAG, "requestLiveSignature onFailure", LogUtil.LOG_FILE_NAME_NORMAL);
                    }
                });
            }
        }
    }


    /**
     * 检查包是否存在
     *
     * @param packname
     * @return
     */
    private boolean checkPackInfo(Context context, String packname) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packname, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo != null;
    }


    /**
     * 打开辅助APP
     *
     * @param context
     */
    private void startTestInstallApp(Context context) {
        PackageManager packageManager = context.getPackageManager();
        if (checkPackInfo(context, INSTALL_PACKAGE_NAME)) {
            Intent toStartRemoteIntent = packageManager.getLaunchIntentForPackage(INSTALL_PACKAGE_NAME);
            context.startActivity(toStartRemoteIntent);
        }
    }

    /**
     * 如果special是空的则全部安装，如果special里面有值则只下载安装里面对应的机器
     *
     * @param special
     * @return
     */
    private boolean isSpecialCanContinue(List<Integer> special) {
        if (special == null || special.size() == 0) {
            return true;
        }
        for (int i = 0; i < special.size(); i++) {
            LogUtil.d(AppUpdateManager.TAG, "PropertyUtils.MQTT_CLAW_MACHINE_ID --> " + PropertyUtil.MQTT_CLAW_MACHINE_ID + " special.get(i) --> " + special.get(i), LogUtil.LOG_FILE_NAME_NORMAL);
            if (PropertyUtil.MQTT_CLAW_MACHINE_ID == special.get(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据服务器的返回值找出相应MQTT服务器对应的最新下载链接
     *
     * @param download
     * @return
     */
    private String getUrlWithMqtt(List<UpdateBean.DataBean.DownloadBean> download) {
        if (download != null) {
            for (int i = 0; i < download.size(); i++) {
                if (PropertyUtil.MQTT_SERVER_IP != null && PropertyUtil.MQTT_SERVER_IP.equals(download.get(i).getMqtt())) {
                    return download.get(i).getUrl();
                }
            }
        }
        return null;
    }

}
