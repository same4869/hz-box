package renren.wawabox.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import renren.wawabox.config.Constants;

/**
 * Created by xunwang on 2017/9/22.
 */

public class FileUtil {

    /**
     * 将文件转换成map存储
     *
     * @return
     */
    public static Map<String, String> fTM(String filePath) {
        Map<String, String> map = new HashMap<>();
        File file = new File(filePath);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                LogUtil.d(Constants.TAG, "fTM line " + line + ": " + tempString, false);
                if (!tempString.startsWith("#")) {
                    String[] strArray = tempString.split(" ");
                    map.put(strArray[0], strArray[1]);
                }
                line++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        for (Map.Entry entry : map.entrySet()) {
            LogUtil.d(Constants.TAG, "fTM " + entry.getKey() + "=" + entry.getValue(), false);
        }
        return map;
    }

    /**
     *  从assets目录中复制整个文件夹内容
     *  @param  context  Context 使用CopyFiles类的Activity
     *  @param  oldPath  String  原文件路径  如：/aa
     *  @param  newPath  String  复制后路径  如：xx:/bb/cc
     */
    public static void copyFilesFassets(Context context,String oldPath,String newPath) {
        try {
            String fileNames[] = context.getAssets().list(oldPath);//获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {//如果是目录
                File file = new File(newPath);
                file.mkdirs();//如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyFilesFassets(context,oldPath + File.separator + fileName,newPath+File.separator+fileName);
                }
            } else {//如果是文件
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount=0;
                while((byteCount=is.read(buffer))!=-1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                }
                fos.flush();//刷新缓冲区
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
