package renren.wawabox.net;

/**
 * Created by xunwang on 17/5/31.
 */

public class RequestFactory {
    public enum RequestType{
        OKHTTP
    }

    public static IRequestManager getRequestManager(RequestType requestType){
        switch (requestType){
            case OKHTTP:
                return OkHttpRequestManager.getInstance();
            default:
                return OkHttpRequestManager.getInstance();
        }
    }
}
