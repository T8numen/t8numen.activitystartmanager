package t8numen.activitystartmanager;

import android.app.Application;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import de.robv.android.xposed.XposedBridge;

public class LogUtil {
    private static final String TAG = "LaunchCtrl";
    private static final String PREFIX = "[ALC] ";
    private static Application mApp;
    private static volatile boolean sDiagnosticEnabled;

    public static void init(Application application) {
        mApp = application;
        sDiagnosticEnabled = ModuleSettingsStore.loadDiagnosticLogEnabledLocal(application);
    }

    public static void writeLog(String text) {
        String message = PREFIX + text;
        Log.i(TAG, message);
        try {
            XposedBridge.log(message);
        } catch (Throwable ignored) {
        }
        try {
            if (mApp == null) {
                return;
            }
            String name = "log.txt";
            File externalDir = mApp.getExternalFilesDir(null);
            if (externalDir == null) {
                return;
            }
            String path = externalDir.getPath();
            File logFile = new File(path, name);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            FileWriter fw = new FileWriter(logFile, true);
            fw.write(new Date() + " " + message + "\n");
            fw.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to persist log", e);
        }
    }

    public static void writeDiagnosticLog(String text) {
        if (sDiagnosticEnabled) {
            writeLog(text);
        }
    }

    public static void setDiagnosticEnabled(boolean enabled) {
        sDiagnosticEnabled = enabled;
    }

    public static boolean isDiagnosticEnabled() {
        return sDiagnosticEnabled;
    }
}
