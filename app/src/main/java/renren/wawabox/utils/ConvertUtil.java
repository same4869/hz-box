package renren.wawabox.utils;

import android.text.TextUtils;

/**
 * Created by xunwang on 2017/9/22.
 */

public class ConvertUtil {
    //把String转化为float
    public static float convertToFloat(String number, float defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(number);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }

    }

    //把String转化为double
    public static double convertToDouble(String number, double defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(number);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }

    }

    //把String转化为int
    public static int convertToInt(String number, int defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(number);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }
}
