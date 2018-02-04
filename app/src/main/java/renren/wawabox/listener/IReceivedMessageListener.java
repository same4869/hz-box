package renren.wawabox.listener;

import renren.wawabox.bean.ReceivedMessage;

/**
 * Created by xunwang on 2017/9/18.
 */

public interface IReceivedMessageListener {
    void onMessageReceived(ReceivedMessage message);
}
