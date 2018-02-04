package renren.wawabox.manager.live;

import android.text.TextUtils;


import renren.wawabox.bean.LiveCheckResultDto;
import renren.wawabox.config.Constants;
import renren.wawabox.json.JSONToBeanHandler;
import renren.wawabox.net.IRequestCallback;
import renren.wawabox.net.RequestFactory;
import renren.wawabox.utils.LogUtil;
import renren.wawabox.utils.Md5Util;

import static renren.wawabox.config.Constants.TENCENT_API_KEY;
import static renren.wawabox.config.Constants.TENCENT_APP_ID;

/**
 * 每隔一段时间检测盒子是否断流
 * Created by xunwang on 2017/9/19.
 */

public class TencentLiveChecker {
    private static final String TAG = "TencentLiveChecker";

    private String mLiveId;

    private onLiveErrorListener mErrorListener;

    public TencentLiveChecker(String liveId, onLiveErrorListener liveErrorListener) {
        mLiveId = liveId;
        mErrorListener = liveErrorListener;
    }

    public void doCheck() {
        LogUtil.d(TAG, "TencentLiveChecker doCheck", LogUtil.LOG_FILE_NAME_LIVE);
        if (TextUtils.isEmpty(mLiveId)) {
            return;
        }
        String t = String.valueOf(System.currentTimeMillis() / 1000 + 5 * 60);
        String url = String.format("http://fcgi.video.qcloud.com/common_access?appid=" + TENCENT_APP_ID + "&interface=Live_Channel_GetStatus&Param.s.channel_id=%s&t=%s&sign=%s",
                mLiveId,
                t,
                Md5Util.md5(TENCENT_API_KEY + t));
        LogUtil.d(TAG, "url=" + url, LogUtil.LOG_FILE_NAME_LIVE);

        RequestFactory.getRequestManager(RequestFactory.RequestType.OKHTTP).get(url, new IRequestCallback() {
            @Override
            public void onSuccess(String response) {
                //获取结果成功
                LogUtil.d(TAG, "check result :" + response, LogUtil.LOG_FILE_NAME_LIVE);
                if (!TextUtils.isEmpty(response)) {
                    try {
                        LiveCheckResultDto liveCheckResultDto = JSONToBeanHandler.fromJsonString(response, LiveCheckResultDto.class);
                        if (liveCheckResultDto.getOutput() != null
                                && liveCheckResultDto.getOutput().size() > 0) {
                            int status = liveCheckResultDto.getOutput().get(0).getStatus();
                            if (status == 0) {
                                //断流
                                if (mErrorListener != null) mErrorListener.onLiveOff();
                            } else if (status == 2) {
                                //直播关闭
                                if (mErrorListener != null) mErrorListener.onLiveClose();
                            } else {
                                if (mErrorListener != null) {
                                    mErrorListener.onLiveSuc();
                                }
                            }
                        }
                    } catch (Exception e) {
                        LogUtil.e(TAG, "TencentLiveChecker doCheck json parse error:" + response, LogUtil.LOG_FILE_NAME_LIVE);
                    }

                }
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });

    }


    public interface onLiveErrorListener {
        void onLiveClose();

        void onLiveOff();

        void onLiveSuc();
    }
}
