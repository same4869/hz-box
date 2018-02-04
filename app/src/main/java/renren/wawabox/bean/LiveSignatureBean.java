package renren.wawabox.bean;

import java.io.Serializable;

/**
 * Created by xunwang on 2017/9/7.
 */

public class LiveSignatureBean implements Serializable {

    /**
     * code : 0
     * msg : ok
     * data : {"live_stream":{"appid":"1400030604","identifiers":{"master":{"identifier":"wawajimachine_master_41","signature":"eJxNjVtPgzAYhv8Ltxr3lQILJl4QIZ6QzYwZR5Y0te3mN8K5gw2z-y4hRL19nvfwbcTh6oYLURxzzfS5VMatAcb1iFGqXOMOVT3Ajnf8gBkXX5grlvFGq5pZZIryskTJuGa0lv8WGpmyUQ2MWABAwQFrkupUYq0Y3*nxgNi2bQ6RybaqbrDIB2ECsYlJAf6kxkyNFbAJNW36*4f7Ab8G6-sn-2UZ9aSKZVt4frqdRS48y76vNgFdW*4qiY7LRRGLcP6mhIeBt4crVxH85B-zYjuTrRMq-wElJO4p7Sp9XmCyaaP3gyMe4c64-ADJNF9A","expire_until":1520684530},"slave01":{"identifier":"wawajimachine_slave01_41","signature":"eJxNjVtPgzAYhv8LtxptKWXTZBeMooJjixkxXJg0BdrtcwwaKOxg-O8SQtTb53kPX1ay2t6JPK*7ynBz0dJ6tJB1O2IoZGVAgWwGeBIn8QlHke*hkrwtRS8R5g6eskJrKLgwnDTFv4m2OPBRDQw7CCGCXORMUp41NJILZcYHTCm1h8hke9m0UFeDsBGm2CYI-UkDRzlWEMXEpuT3D3YDjoM3P-TiKIh1F83KPWO9ZHO4PDETltk62ijYvNB1gJ*lupm7oQfLTLpJ5juebhjO0vNhx9pZ3SZh6j-k7*rjvnOuK7JUr9k29RYL6-sHWsBeBA__","expire_until":1520684530}}}}
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

    public static class DataBean implements Serializable {
        /**
         * live_stream : {"appid":"1400030604","identifiers":{"master":{"identifier":"wawajimachine_master_41","signature":"eJxNjVtPgzAYhv8Ltxr3lQILJl4QIZ6QzYwZR5Y0te3mN8K5gw2z-y4hRL19nvfwbcTh6oYLURxzzfS5VMatAcb1iFGqXOMOVT3Ajnf8gBkXX5grlvFGq5pZZIryskTJuGa0lv8WGpmyUQ2MWABAwQFrkupUYq0Y3*nxgNi2bQ6RybaqbrDIB2ECsYlJAf6kxkyNFbAJNW36*4f7Ab8G6-sn-2UZ9aSKZVt4frqdRS48y76vNgFdW*4qiY7LRRGLcP6mhIeBt4crVxH85B-zYjuTrRMq-wElJO4p7Sp9XmCyaaP3gyMe4c64-ADJNF9A","expire_until":1520684530},"slave01":{"identifier":"wawajimachine_slave01_41","signature":"eJxNjVtPgzAYhv8LtxptKWXTZBeMooJjixkxXJg0BdrtcwwaKOxg-O8SQtTb53kPX1ay2t6JPK*7ynBz0dJ6tJB1O2IoZGVAgWwGeBIn8QlHke*hkrwtRS8R5g6eskJrKLgwnDTFv4m2OPBRDQw7CCGCXORMUp41NJILZcYHTCm1h8hke9m0UFeDsBGm2CYI-UkDRzlWEMXEpuT3D3YDjoM3P-TiKIh1F83KPWO9ZHO4PDETltk62ijYvNB1gJ*lupm7oQfLTLpJ5juebhjO0vNhx9pZ3SZh6j-k7*rjvnOuK7JUr9k29RYL6-sHWsBeBA__","expire_until":1520684530}}}
         */

        private LiveStreamBean live_stream;

        public LiveStreamBean getLive_stream() {
            return live_stream;
        }

        public void setLive_stream(LiveStreamBean live_stream) {
            this.live_stream = live_stream;
        }

        public static class LiveStreamBean implements Serializable {
            /**
             * appid : 1400030604
             * identifiers : {"master":{"identifier":"wawajimachine_master_41","signature":"eJxNjVtPgzAYhv8Ltxr3lQILJl4QIZ6QzYwZR5Y0te3mN8K5gw2z-y4hRL19nvfwbcTh6oYLURxzzfS5VMatAcb1iFGqXOMOVT3Ajnf8gBkXX5grlvFGq5pZZIryskTJuGa0lv8WGpmyUQ2MWABAwQFrkupUYq0Y3*nxgNi2bQ6RybaqbrDIB2ECsYlJAf6kxkyNFbAJNW36*4f7Ab8G6-sn-2UZ9aSKZVt4frqdRS48y76vNgFdW*4qiY7LRRGLcP6mhIeBt4crVxH85B-zYjuTrRMq-wElJO4p7Sp9XmCyaaP3gyMe4c64-ADJNF9A","expire_until":1520684530},"slave01":{"identifier":"wawajimachine_slave01_41","signature":"eJxNjVtPgzAYhv8LtxptKWXTZBeMooJjixkxXJg0BdrtcwwaKOxg-O8SQtTb53kPX1ay2t6JPK*7ynBz0dJ6tJB1O2IoZGVAgWwGeBIn8QlHke*hkrwtRS8R5g6eskJrKLgwnDTFv4m2OPBRDQw7CCGCXORMUp41NJILZcYHTCm1h8hke9m0UFeDsBGm2CYI-UkDRzlWEMXEpuT3D3YDjoM3P-TiKIh1F83KPWO9ZHO4PDETltk62ijYvNB1gJ*lupm7oQfLTLpJ5juebhjO0vNhx9pZ3SZh6j-k7*rjvnOuK7JUr9k29RYL6-sHWsBeBA__","expire_until":1520684530}}
             */

            private String appid;
            private IdentifiersBean identifiers;

            public String getAppid() {
                return appid;
            }

            public void setAppid(String appid) {
                this.appid = appid;
            }

            public IdentifiersBean getIdentifiers() {
                return identifiers;
            }

            public void setIdentifiers(IdentifiersBean identifiers) {
                this.identifiers = identifiers;
            }

            public static class IdentifiersBean implements Serializable {
                /**
                 * master : {"identifier":"wawajimachine_master_41","signature":"eJxNjVtPgzAYhv8Ltxr3lQILJl4QIZ6QzYwZR5Y0te3mN8K5gw2z-y4hRL19nvfwbcTh6oYLURxzzfS5VMatAcb1iFGqXOMOVT3Ajnf8gBkXX5grlvFGq5pZZIryskTJuGa0lv8WGpmyUQ2MWABAwQFrkupUYq0Y3*nxgNi2bQ6RybaqbrDIB2ECsYlJAf6kxkyNFbAJNW36*4f7Ab8G6-sn-2UZ9aSKZVt4frqdRS48y76vNgFdW*4qiY7LRRGLcP6mhIeBt4crVxH85B-zYjuTrRMq-wElJO4p7Sp9XmCyaaP3gyMe4c64-ADJNF9A","expire_until":1520684530}
                 * slave01 : {"identifier":"wawajimachine_slave01_41","signature":"eJxNjVtPgzAYhv8LtxptKWXTZBeMooJjixkxXJg0BdrtcwwaKOxg-O8SQtTb53kPX1ay2t6JPK*7ynBz0dJ6tJB1O2IoZGVAgWwGeBIn8QlHke*hkrwtRS8R5g6eskJrKLgwnDTFv4m2OPBRDQw7CCGCXORMUp41NJILZcYHTCm1h8hke9m0UFeDsBGm2CYI-UkDRzlWEMXEpuT3D3YDjoM3P-TiKIh1F83KPWO9ZHO4PDETltk62ijYvNB1gJ*lupm7oQfLTLpJ5juebhjO0vNhx9pZ3SZh6j-k7*rjvnOuK7JUr9k29RYL6-sHWsBeBA__","expire_until":1520684530}
                 */

                private MasterBean master;
                private Slave01Bean slave01;

                public MasterBean getMaster() {
                    return master;
                }

                public void setMaster(MasterBean master) {
                    this.master = master;
                }

                public Slave01Bean getSlave01() {
                    return slave01;
                }

                public void setSlave01(Slave01Bean slave01) {
                    this.slave01 = slave01;
                }

                public static class MasterBean implements Serializable {
                    /**
                     * identifier : wawajimachine_master_41
                     * signature : eJxNjVtPgzAYhv8Ltxr3lQILJl4QIZ6QzYwZR5Y0te3mN8K5gw2z-y4hRL19nvfwbcTh6oYLURxzzfS5VMatAcb1iFGqXOMOVT3Ajnf8gBkXX5grlvFGq5pZZIryskTJuGa0lv8WGpmyUQ2MWABAwQFrkupUYq0Y3*nxgNi2bQ6RybaqbrDIB2ECsYlJAf6kxkyNFbAJNW36*4f7Ab8G6-sn-2UZ9aSKZVt4frqdRS48y76vNgFdW*4qiY7LRRGLcP6mhIeBt4crVxH85B-zYjuTrRMq-wElJO4p7Sp9XmCyaaP3gyMe4c64-ADJNF9A
                     * expire_until : 1520684530
                     */

                    private String identifier;
                    private String signature;
                    private int expire_until;

                    public String getIdentifier() {
                        return identifier;
                    }

                    public void setIdentifier(String identifier) {
                        this.identifier = identifier;
                    }

                    public String getSignature() {
                        return signature;
                    }

                    public void setSignature(String signature) {
                        this.signature = signature;
                    }

                    public int getExpire_until() {
                        return expire_until;
                    }

                    public void setExpire_until(int expire_until) {
                        this.expire_until = expire_until;
                    }
                }

                public static class Slave01Bean implements Serializable {
                    /**
                     * identifier : wawajimachine_slave01_41
                     * signature : eJxNjVtPgzAYhv8LtxptKWXTZBeMooJjixkxXJg0BdrtcwwaKOxg-O8SQtTb53kPX1ay2t6JPK*7ynBz0dJ6tJB1O2IoZGVAgWwGeBIn8QlHke*hkrwtRS8R5g6eskJrKLgwnDTFv4m2OPBRDQw7CCGCXORMUp41NJILZcYHTCm1h8hke9m0UFeDsBGm2CYI-UkDRzlWEMXEpuT3D3YDjoM3P-TiKIh1F83KPWO9ZHO4PDETltk62ijYvNB1gJ*lupm7oQfLTLpJ5juebhjO0vNhx9pZ3SZh6j-k7*rjvnOuK7JUr9k29RYL6-sHWsBeBA__
                     * expire_until : 1520684530
                     */

                    private String identifier;
                    private String signature;
                    private int expire_until;

                    public String getIdentifier() {
                        return identifier;
                    }

                    public void setIdentifier(String identifier) {
                        this.identifier = identifier;
                    }

                    public String getSignature() {
                        return signature;
                    }

                    public void setSignature(String signature) {
                        this.signature = signature;
                    }

                    public int getExpire_until() {
                        return expire_until;
                    }

                    public void setExpire_until(int expire_until) {
                        this.expire_until = expire_until;
                    }
                }
            }
        }
    }
}
