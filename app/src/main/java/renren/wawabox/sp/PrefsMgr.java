package renren.wawabox.sp;

import android.content.SharedPreferences;

import renren.wawabox.app.WawaNewApplication;


/**
 * Created by xunwang on 2017/8/29.
 */

public class PrefsMgr {

    public static int getInt(String tbl, String key, int def) {
        if (WawaNewApplication.getInstance() == null) {
            return def;
        }
        SharedPreferences prefs = WawaNewApplication.getInstance().getCommSharedPreferences(tbl);
        return prefs.getInt(key, def);
    }

    public static long getLong(String tbl, String key, long def) {
        if (WawaNewApplication.getInstance() == null) {
            return def;
        }
        SharedPreferences prefs = WawaNewApplication.getInstance().getCommSharedPreferences(tbl);
        return prefs.getLong(key, def);
    }

    public static String getString(String tbl, String key, String def) {
        if (WawaNewApplication.getInstance() == null) {
            return def;
        }
        SharedPreferences prefs = WawaNewApplication.getInstance().getCommSharedPreferences(tbl);
        return prefs.getString(key, def);
    }

    public static boolean getBoolean(String tbl, String key, boolean def) {
        if (WawaNewApplication.getInstance() == null) {
            return def;
        }
        SharedPreferences prefs = WawaNewApplication.getInstance().getCommSharedPreferences(tbl);
        return prefs.getBoolean(key, def);
    }

    public static float getFloat(String tbl, String key, float def) {
        if (WawaNewApplication.getInstance() == null) {
            return def;
        }

        SharedPreferences prefs = WawaNewApplication.getInstance().getCommSharedPreferences(tbl);
        return prefs.getFloat(key, def);
    }

    public static void putInt(String tbl, String key, int value) {
        SharedPreferences prefs = WawaNewApplication.getInstance().getCommSharedPreferences(tbl);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void putLong(String tbl, String key, long value) {
        SharedPreferences prefs = WawaNewApplication.getInstance().getCommSharedPreferences(tbl);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static void putString(String tbl, String key, String value) {
        SharedPreferences prefs = WawaNewApplication.getInstance().getCommSharedPreferences(tbl);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void putBoolean(String tbl, String key, boolean value) {
        SharedPreferences prefs = WawaNewApplication.getInstance().getCommSharedPreferences(tbl);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void putFloat(String tbl, String key, float value) {
        SharedPreferences prefs = WawaNewApplication.getInstance().getCommSharedPreferences(tbl);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    public static void removeValue(String tbl, String key) {
        SharedPreferences prefs = WawaNewApplication.getInstance().getCommSharedPreferences(tbl);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.apply();
    }
}
