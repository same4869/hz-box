package renren.wawabox.config;

/**
 * Created by xunwang on 2017/9/12.
 */

public class Constants {
    public static final String TAG = "kkkkkkkk"; //全局TAG
    public static final boolean IS_LOG = true; //是否输出日志到控制台
    public static final boolean IS_LOG_IN_FILE = false; //是否输出日志到文件
    //全局HTTP请求
    public static final String BASE_URL = "https://renrenzww.com/renren/";//"http://182.254.221.82/api/v1/"; //online
    //自动更新
    public static final boolean IS_ENABLE_UPDATE = true; //是否开启自动更新检查
    public static final String REQUEST_URL = BASE_URL + "wawaji/update";
    //互动直播
    public static final int APP_ID = 1400059084;//1400045378;//1400049020;//1400044149;//1400045378
    public static final int ACCOUNT_TYPE = 21129;//18060;//19071;
    //检查直播流
    public static final String TENCENT_API_KEY = "01e8e3b461df1e7242933bfc160b1d96";
    public static final int TENCENT_APP_ID = 1254357563;
    //上传日志
    public static final String UPLOAD_LOG_URL = "http://115.159.201.100/tv/log";
    public static final boolean IS_UPLOAD_TO_SERVER = false;
}
