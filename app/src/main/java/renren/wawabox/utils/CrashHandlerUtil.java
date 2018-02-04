package renren.wawabox.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by xunwang on 2017/9/20.
 */

public class CrashHandlerUtil implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";

    private static String PATH;
    private static final String FILE_NAME = "crash";

    //log文件的后缀名
    private static final String FILE_NAME_SUFFIX = ".txt";

    private static CrashHandlerUtil sInstance = new CrashHandlerUtil();

    //系统默认的异常处理（默认情况下，系统会终止当前的异常程序）
    private Thread.UncaughtExceptionHandler mDefaultCrashHandler;

    private Context mContext;

    //构造方法私有，防止外部构造多个实例，即采用单例模式
    private CrashHandlerUtil() {
    }

    public static CrashHandlerUtil getInstance() {
        return sInstance;
    }

    //这里主要完成初始化工作
    public void init(Context context, String path) {
        PATH = path;
        //获取系统默认的异常处理器
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        //将当前实例设为系统默认的异常处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
        //获取Context，方便内部使用
        mContext = context.getApplicationContext();
    }

    /**
     * 这个是最关键的函数，当程序中有未被捕获的异常，系统将会自动调用#uncaughtException方法
     * thread为出现未捕获异常的线程，ex为未捕获的异常，有了这个ex，我们就可以得到异常信息。
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            //导出异常信息到SD卡中
            dumpExceptionToSDCard(ex);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //打印出当前调用栈信息
        ex.printStackTrace();

        DeviceUtil.reboot("程序crash了，重启", true);

        //如果系统提供了默认的异常处理器，则交给系统去结束我们的程序，否则就由我们自己结束自己
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(thread, ex);
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
        }

    }

    private void dumpExceptionToSDCard(Throwable ex) throws IOException {
        File dir = new File(PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        long current = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA).format(new Date(current));
        //以当前时间创建log文件
        File file = new File(PATH  + time + FILE_NAME + FILE_NAME_SUFFIX);

        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            //导出发生异常的时间
            pw.println(time);

            //导出手机信息
            dumpPhoneInfo(pw);

            pw.println();
            //导出异常的调用栈信息
            ex.printStackTrace(pw);

            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dumpPhoneInfo(PrintWriter pw) throws PackageManager.NameNotFoundException {
        //应用的版本名称和版本号
        PackageManager pm = mContext.getPackageManager();
        PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
        pw.print("App Version: ");
        pw.print(pi.versionName);
        pw.print('_');
        pw.println(pi.versionCode);

    }
}
