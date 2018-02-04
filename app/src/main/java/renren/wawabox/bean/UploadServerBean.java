package renren.wawabox.bean;

import java.io.Serializable;

public class UploadServerBean implements Serializable {
    private String id;//盒子id
    private boolean isCam2;//正面侧面盒子
    private int type;//日志类型
    private String msg;//日志内容

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isCam2() {
        return isCam2;
    }

    public void setCam2(boolean cam2) {
        isCam2 = cam2;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
