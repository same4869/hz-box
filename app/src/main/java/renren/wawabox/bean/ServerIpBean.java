package renren.wawabox.bean;

/**
 * Created by xunwang on 2017/9/22.
 */

public class ServerIpBean {

    /**
     * code : 0
     * msg : ok
     * data : {"tcp":"115.159.39.145:9527"}
     */

    private int code;
    private String msg;
    private DataBean data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * tcp : 115.159.39.145:9527
         */

        private String socketUrl;

        public String getSocketUrl() {
            return socketUrl;
        }

        public void setSocketUrl(String socketUrl) {
            this.socketUrl = socketUrl;
        }
    }
}
