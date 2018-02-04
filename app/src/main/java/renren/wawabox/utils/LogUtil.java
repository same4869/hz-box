package renren.wawabox.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import renren.wawabox.bean.RoomStatusBean;
import renren.wawabox.bean.UploadServerBean;
import renren.wawabox.config.Constants;
import renren.wawabox.json.JSONFormatExcetion;
import renren.wawabox.json.JSONToBeanHandler;
import renren.wawabox.net.IRequestCallback;
import renren.wawabox.net.RequestFactory;
import renren.wawabox.update.AppUpdateManager;

import static renren.wawabox.config.Constants.IS_UPLOAD_TO_SERVER;
import static renren.wawabox.config.Constants.UPLOAD_LOG_URL;
import static renren.wawabox.update.AppUpdateManager.REQUEST_INSTALL_REMOTE_ACTION;

/**
 * 日志输出系统，支持输出到文件中，删除N天前的日志
 * Created by xunwang on 2017/9/12.
 */

public class LogUtil {
    public static final int LOG_FILE_NAME_NORMAL = 0;
    public static final int LOG_FILE_NAME_MQTT = 1;
    public static final int LOG_FILE_NAME_MIND = 2;
    public static final int LOG_FILE_NAME_LIVE = 3;
    private static final String LOG_FILE_NAME_NORMAL_STRING = "-normal-log.txt";
    private static final String LOG_FILE_NAME_MQTT_STRING = "-mqtt-log.txt";
    private static final String LOG_FILE_NAME_MIND_STRING = "-mind-log.txt";
    private static final String LOG_FILE_NAME_LIVE_STRING = "-live-log.txt";

    private static Boolean MYLOG_SWITCH = true; // 日志文件总开关
    private static Boolean MYLOG_WRITE_TO_FILE = false;// 日志写入文件开关
    private static char MYLOG_TYPE = 'v';// 输入日志类型，w代表只输出告警信息等，v代表输出所有信息
    private static String MYLOG_PATH_SDCARD_DIR;// 日志文件在sdcard中的路径
    private static int SDCARD_LOG_FILE_SAVE_DAYS = 1;// sd卡中日志文件的最多保存天数
    private static String MYLOGFILEName = "-normal-log.txt";// 本类输出的日志文件名称
    private static SimpleDateFormat myLogSdf = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.CHINA);// 日志的输出格式
    private static SimpleDateFormat logfile = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);// 日志文件格式
    private static List<String> logList = new ArrayList<>();

    public static void init(Context context, boolean isLog, boolean isLogToFile) {
        MYLOG_PATH_SDCARD_DIR = getLog(context).getAbsolutePath();
        MYLOG_SWITCH = isLog;
        MYLOG_WRITE_TO_FILE = isLogToFile;
    }

    private static void changeLogName(int type) {
        if (type == LOG_FILE_NAME_NORMAL) {
            MYLOGFILEName = LOG_FILE_NAME_NORMAL_STRING;
        } else if (type == LOG_FILE_NAME_MQTT) {
            MYLOGFILEName = LOG_FILE_NAME_MQTT_STRING;
        } else if (type == LOG_FILE_NAME_MIND) {
            MYLOGFILEName = LOG_FILE_NAME_MIND_STRING;
        } else if (type == LOG_FILE_NAME_LIVE) {
            MYLOGFILEName = LOG_FILE_NAME_LIVE_STRING;
        }
    }

    public static void e(String tag, Object msg, int type) { // 错误信息
        changeLogName(type);
        log(tag, msg.toString(), 'e');
    }

    public static void d(String tag, Object msg, int type) {// 调试信息
        changeLogName(type);
        log(tag, msg.toString(), 'd');
    }

    public static void w(String tag, Object msg) { // 警告信息
        changeLogName(LOG_FILE_NAME_NORMAL);
        log(tag, msg.toString(), 'w');
    }

    public static void e(String tag, Object msg) { // 错误信息
        changeLogName(LOG_FILE_NAME_NORMAL);
        log(tag, msg.toString(), 'e');
    }

    public static void d(String tag, Object msg) {// 调试信息
        changeLogName(LOG_FILE_NAME_NORMAL);
        log(tag, msg.toString(), 'd');
    }

    public static void d(String tag, Object msg, boolean isWrite) {
        boolean temp = MYLOG_WRITE_TO_FILE;
        if (!isWrite) {
            MYLOG_WRITE_TO_FILE = false;
        } else {
            MYLOG_WRITE_TO_FILE = true;
            changeLogName(LOG_FILE_NAME_NORMAL);
        }
        log(tag, msg.toString(), 'd');
        MYLOG_WRITE_TO_FILE = temp;
    }

    public static void i(String tag, Object msg) {//
        changeLogName(LOG_FILE_NAME_NORMAL);
        log(tag, msg.toString(), 'i');
    }

    public static void v(String tag, Object msg) {
        changeLogName(LOG_FILE_NAME_NORMAL);
        log(tag, msg.toString(), 'v');
    }

    public static void w(String tag, String text) {
        changeLogName(LOG_FILE_NAME_NORMAL);
        log(tag, text, 'w');
    }

    public static void e(String tag, String text) {
        changeLogName(LOG_FILE_NAME_NORMAL);
        log(tag, text, 'e');
    }

    public static void d(String tag, String text) {
        changeLogName(LOG_FILE_NAME_NORMAL);
        log(tag, text, 'd');
    }

    public static void i(String tag, String text) {
        changeLogName(LOG_FILE_NAME_NORMAL);
        log(tag, text, 'i');
    }

    public static void v(String tag, String text) {
        changeLogName(LOG_FILE_NAME_NORMAL);
        log(tag, text, 'v');
    }

    /**
     * 根据tag, msg和等级，输出日志
     *
     * @param tag
     * @param msg
     * @param level
     * @return void
     * @since v 1.0
     */
    private static void log(String tag, String msg, char level) {
        if (MYLOG_SWITCH) {
            if ('e' == level && ('e' == MYLOG_TYPE || 'v' == MYLOG_TYPE)) { // 输出错误信息
                Log.e(tag, msg);
            } else if ('w' == level && ('w' == MYLOG_TYPE || 'v' == MYLOG_TYPE)) {
                Log.w(tag, msg);
            } else if ('d' == level && ('d' == MYLOG_TYPE || 'v' == MYLOG_TYPE)) {
                Log.d(tag, msg);
            } else if ('i' == level && ('d' == MYLOG_TYPE || 'v' == MYLOG_TYPE)) {
                Log.i(tag, msg);
            } else {
                Log.v(tag, msg);
            }
            if (MYLOG_WRITE_TO_FILE)
                writeLogtoFile(String.valueOf(level), tag, msg);
        }
    }

    /**
     * 打开日志文件并写入日志
     *
     * @return
     **/
    private static void writeLogtoFile(String mylogtype, String tag, String text) {// 新建或打开日志文件
        Date nowtime = new Date();
        String needWriteFiel = logfile.format(nowtime);
        String needWriteMessage = myLogSdf.format(nowtime) + "    " + mylogtype
                + "    " + tag + "    " + text;
        File file = new File(MYLOG_PATH_SDCARD_DIR, needWriteFiel
                + MYLOGFILEName);
        try {
            FileWriter filerWriter = new FileWriter(file, true);//后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
            BufferedWriter bufWriter = new BufferedWriter(filerWriter);
            bufWriter.write(needWriteMessage);
            bufWriter.newLine();
            bufWriter.close();
            filerWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除制定的日志文件
     */
    public static void delFile() {// 删除日志文件
        String needDelFiel = logfile.format(getDateBefore());
        File file = new File(MYLOG_PATH_SDCARD_DIR, needDelFiel + MYLOGFILEName);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 得到现在时间前的几天日期，用来得到需要删除的日志文件名
     */
    private static Date getDateBefore() {
        Date nowtime = new Date();
        Calendar now = Calendar.getInstance();
        now.setTime(nowtime);
        now.set(Calendar.DATE, now.get(Calendar.DATE)
                - SDCARD_LOG_FILE_SAVE_DAYS);
        return now.getTime();
    }

    public static void deleteFileLog() {
        File f = new File(MYLOG_PATH_SDCARD_DIR);
        File[] files = f.listFiles();// 列出所有文件
        if (files != null) {
            int count = files.length;// 文件个数
            for (int i = 0; i < count; i++) {
                File file = files[i];
                if (file != null && file.getName() != null) {
                    LogUtil.d("kkkkkkkk", "file.getName() --> " + file.getName());
                    String[] fileLogNames = file.getName().split("-");

                    String needDelFiel = logfile.format(getDateBefore());
                    String[] needDeletefileLogNames = needDelFiel.split("-");

                    LogUtil.d("kkkkkkkk", "fileLogNames[0] --> " + fileLogNames[0] + " fileLogNames[1] --> " + fileLogNames[1] + " fileLogNames[2] --> " + fileLogNames[2]);
                    LogUtil.d("kkkkkkkk", "needDeletefileLogNames[0] --> " + needDeletefileLogNames[0] + " needDeletefileLogNames[1] --> " + needDeletefileLogNames[1] + " fileLogNames[2] --> " + needDeletefileLogNames[2]);

                    if (ConvertUtil.convertToInt(fileLogNames[0], 0) < ConvertUtil.convertToInt(needDeletefileLogNames[0], 0)) {
                        //年份小于，直接删除
                        LogUtil.d("kkkkkkkk", "年份小于，直接删除", LOG_FILE_NAME_NORMAL);
                        deleteFile(file.getAbsolutePath());
                    } else if (ConvertUtil.convertToInt(fileLogNames[1], 0) < ConvertUtil.convertToInt(needDeletefileLogNames[1], 0) && ConvertUtil.convertToInt(fileLogNames[0], 0) <= ConvertUtil.convertToInt(needDeletefileLogNames[0], 0)) {
                        //月份小于，且年份小于等于，删除
                        LogUtil.d("kkkkkkkk", "月份小于，且年份小于等于，删除", LOG_FILE_NAME_NORMAL);
                        deleteFile(file.getAbsolutePath());
                    } else if (ConvertUtil.convertToInt(fileLogNames[2], 0) < ConvertUtil.convertToInt(needDeletefileLogNames[2], 0) && ConvertUtil.convertToInt(fileLogNames[1], 0) <= ConvertUtil.convertToInt(needDeletefileLogNames[1], 0) && ConvertUtil.convertToInt(fileLogNames[0], 0) <= ConvertUtil.convertToInt(needDeletefileLogNames[0], 0)) {
                        //日期小于，且月份和年份小于等于，删除
                        LogUtil.d("kkkkkkkk", "日期小于，且月份和年份小于等于，删除", LOG_FILE_NAME_NORMAL);
                        deleteFile(file.getAbsolutePath());
                    }
                }
            }
        }
    }

    private static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    public static File getLog(Context context) {
        File dir = new File(context.getFilesDir().getPath(), "wlog");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static void startUploadToServer(String id, boolean isCam2, int type, String msg, boolean isRightUpload) {
        UploadServerBean uploadServerBean = new UploadServerBean();
        uploadServerBean.setId(id);
        uploadServerBean.setCam2(isCam2);
        uploadServerBean.setType(type);
        uploadServerBean.setMsg(msg);
        try {
            String jsonLogStr = JSONToBeanHandler.toJsonString(uploadServerBean);
            if (logList.size() < 15) {
                logList.add(jsonLogStr);
                if (!isRightUpload) {
                    return;
                }
            }
            uploadToServer(setupLogString(logList));
        } catch (JSONFormatExcetion jsonFormatExcetion) {
            jsonFormatExcetion.printStackTrace();
        }
    }

    private static String setupLogString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            sb.append("\n");
        }
        return sb.toString();
    }

    public static void uploadToServer(String jsonLog) {
        logList.clear();
        HashMap<String, String> params = new HashMap<>();
        params.put("jsonLog", jsonLog);
        RequestFactory.getRequestManager(RequestFactory.RequestType.OKHTTP).post(UPLOAD_LOG_URL, AppUtil.hashMapToJson(params), new IRequestCallback() {
            @Override
            public void onSuccess(String response) {
            }

            @Override
            public void onFailure(Throwable throwable) {
            }
        });
    }
}
