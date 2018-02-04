package renren.wawabox.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import renren.wawabox.config.Constants;

/**
 * Created by xunwang on 2017/9/19.
 */

public class AppUtil {
    private static String channel;

    /**
     * 把数据源HashMap转换成json
     *
     * @param map
     */
    public static String hashMapToJson(HashMap map) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry e = (Map.Entry) it.next();
            stringBuilder.append("\"" + e.getKey() + "\":");
            stringBuilder.append("\"" + e.getValue() + "\",");
        }
        String string = stringBuilder.substring(0, stringBuilder.lastIndexOf(","));
        string += "}";
        return string;
    }

    public static int getVersionCode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packInfo = null;
        int versionCode = 0;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionCode = packInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        LogUtil.d(Constants.TAG, "versionCode --> " + versionCode, LogUtil.LOG_FILE_NAME_NORMAL);
        return versionCode;
    }

    public static String getChannelByMeta(Context context) {
        if (channel != null) {
            return channel;
        }
        ApplicationInfo appinfo = context.getApplicationInfo();
        String sourceDir = appinfo.sourceDir;
        String ret = "";
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(sourceDir);
            Enumeration<?> entries = zipfile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                String entryName = entry.getName();
                if (entryName.startsWith("META-INF/channel")) {
                    ret = entryName;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipfile != null) {
                try {
                    zipfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String[] split = ret.split("_");
        if (split.length >= 2) {
            channel = ret.substring(split[0].length() + 1);
        }
        return channel;
    }

    public static boolean fileIsExists(String strFile) {
        try {
            File f = new File(strFile);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 删除单个文件
     *
     * @param filePath 被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    //删除文件夹和文件夹里面的文件
    public static void deleteDir(final String pPath) {
        File dir = new File(pPath);
        deleteDirWihtFile(dir);
    }

    public static void deleteDirWihtFile(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                file.delete(); // 删除所有文件
            } else if (file.isDirectory()) {
                deleteDirWihtFile(file); // 递规的方式删除文件夹
            }
        }
        dir.delete();// 删除目录本身
    }
}
