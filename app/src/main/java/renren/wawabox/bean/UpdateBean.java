package renren.wawabox.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by xunwang on 2017/8/29.
 */

public class UpdateBean implements Serializable {

    /**
     * code : 0
     * data : {"versionCode":1000001,"download":[{"mqtt":"192.168.0.127","url":"http://s.same.com/test_install_100000127.apk"},{"mqtt":"192.168.1.240","url":"http://s.same.com/test_install_100000240.apk"}],"is_force":false,"special":[21,22,23,24],"selfCode":1000001,"selfDownload":"http://s.same.com/test_install_1000002.apk"}
     */

    private int code;
    private DataBean data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean implements Serializable {
        /**
         * versionCode : 1000001
         * download : [{"mqtt":"192.168.0.127","url":"http://s.same.com/test_install_100000127.apk"},{"mqtt":"192.168.1.240","url":"http://s.same.com/test_install_100000240.apk"}]
         * is_force : false
         * special : [21,22,23,24]
         * selfCode : 1000001
         * selfDownload : http://s.same.com/test_install_1000002.apk
         */

        private int versionCode;
        private boolean is_force;
        private int selfCode;
        private String selfDownload;
        private List<DownloadBean> download;
        private List<Integer> special;

        public int getVersionCode() {
            return versionCode;
        }

        public void setVersionCode(int versionCode) {
            this.versionCode = versionCode;
        }

        public boolean isIs_force() {
            return is_force;
        }

        public void setIs_force(boolean is_force) {
            this.is_force = is_force;
        }

        public int getSelfCode() {
            return selfCode;
        }

        public void setSelfCode(int selfCode) {
            this.selfCode = selfCode;
        }

        public String getSelfDownload() {
            return selfDownload;
        }

        public void setSelfDownload(String selfDownload) {
            this.selfDownload = selfDownload;
        }

        public List<DownloadBean> getDownload() {
            return download;
        }

        public void setDownload(List<DownloadBean> download) {
            this.download = download;
        }

        public List<Integer> getSpecial() {
            return special;
        }

        public void setSpecial(List<Integer> special) {
            this.special = special;
        }

        public static class DownloadBean implements Serializable {
            /**
             * mqtt : 192.168.0.127
             * url : http://s.same.com/test_install_100000127.apk
             */

            private String mqtt;
            private String url;

            public String getMqtt() {
                return mqtt;
            }

            public void setMqtt(String mqtt) {
                this.mqtt = mqtt;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }
        }
    }
}
