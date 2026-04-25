package t8numen.activitystartmanager;

import android.app.Application;

public class ModuleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.init(this);
        AskPopupController.init(this);
    }
}
