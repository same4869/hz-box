package renren.wawabox.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 打印日志到屏幕上
 * Created by xunwang on 2017/9/12.
 */

public class StreamLogUtil {
    private static final int MAX_LOG_LENGTH = 3000;
    private static StringBuilder stringBuilder = new StringBuilder();

    public interface OnRecvLog {
        void onRecvLog(String log);

        void onStateChange(String state);
    }

    private Handler mHandler;
    private OnRecvLog mL;

    private StreamLogUtil(Looper looper, OnRecvLog l) {
        mHandler = new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.what == 1) {
                    String log = (String) msg.obj;
                    if (mL != null) {
                        stringBuilder.append(getCurTime()).append(log).append("\n");
                        mL.onRecvLog(log);
                    }
                } else if (msg.what == 2) {
                    if (mL != null) {
                        mL.onStateChange((String) msg.obj);
                    }
                }


            }
        };
        mL = l;
    }

    private static StreamLogUtil sLog;


    public static StreamLogUtil getLog() {
        return sLog;
    }

    public static StreamLogUtil initLog(Looper looper, OnRecvLog l) {
        sLog = new StreamLogUtil(looper, l);
        return sLog;
    }


    public static void putLog(String log) {
        if (sLog != null) {
            sLog.mHandler.sendMessage(sLog.mHandler.obtainMessage(1, log));
        }
    }

    public static void changeState(String state) {
        if (sLog != null) {
            sLog.mHandler.sendMessage(sLog.mHandler.obtainMessage(2, state));
        }
    }

    public static StringBuilder getStringBuilder() {
        return stringBuilder;
    }

    /**
     * 如果stringBuilder超过最大限制，截取
     *
     * @return
     */
    public static String rebuildOutLog() {
        if (stringBuilder.toString().length() > MAX_LOG_LENGTH) {
            String rebuildString = stringBuilder.toString().substring(stringBuilder.toString().length() - MAX_LOG_LENGTH, stringBuilder.toString().length());
            stringBuilder.setLength(0);
            stringBuilder.append(rebuildString);
            return stringBuilder.toString();
        } else {
            return stringBuilder.toString();
        }
    }

    private String getCurTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss ");
        Date curDate = new Date(System.currentTimeMillis());
        String str = formatter.format(curDate);
        return str;
    }
}
