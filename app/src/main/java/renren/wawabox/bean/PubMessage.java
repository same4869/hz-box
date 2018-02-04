package renren.wawabox.bean;

/**
 * Created by xunwang on 2017/9/14.
 */

public class PubMessage {
    /**
     * 确保这条消息  (publish 成功了之后 || 收到ok) 再继续下一条消息处理
     */
    public boolean block = false;
    /**
     * 发送消息
     */
    public String msg;

    public long delay;

    /**
     * 触发条件1,2
     */
    public String trigger1;

    public String trigger2;


    /**
     * 离上一条间隔最低
     */
    public long minGip;
    /**
     * 离上一条间隔最大
     */
    public long limit;

    public long inTime;

    public boolean hasClawed = false;

    public boolean gameEnd = false;
}
