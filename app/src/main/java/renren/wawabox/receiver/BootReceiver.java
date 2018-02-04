package renren.wawabox.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import renren.wawabox.ui.BoxMainActivity;

/**
 * Created by xunwang on 2017/9/20.
 */

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent intent_n = new Intent(context, BoxMainActivity.class);
            intent_n.setAction("android.intent.action.MAIN");
            intent_n.addCategory("android.intent.category.LAUNCHER");
            intent_n.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent_n);
        }
    }
}
