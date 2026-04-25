package t8numen.activitystartmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AskUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ModuleConfig.ACTION_ASK_UPDATED.equals(intent.getAction())) {
            return;
        }
        AskPopupController.init(context);
        AskPopupController.refresh();
    }
}
