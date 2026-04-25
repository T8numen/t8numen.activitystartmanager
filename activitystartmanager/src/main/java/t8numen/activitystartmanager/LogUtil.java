package t8numen.activitystartmanager;

import android.app.Application;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XposedBridge;

public final class LogUtil {
    private static final String TAG = "ActivityStartManager";
    private static final String LOG_FILE_NAME = "activity-start-manager.log";
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        }
    };

    private static volatile Application moduleApplication;
    private static volatile boolean diagnosticEnabled;

    private LogUtil() {
    }

    public static void init(Application application) {
        moduleApplication = application;
        diagnosticEnabled = ModuleSettingsStore.loadDiagnosticLogEnabledLocal(application);
    }

    public static void writeLog(String text) {
        String message = "[Activity启动管理] " + String.valueOf(text);
        Log.i(TAG, message);
        writeXposedLog(message);
        writeAppLogFile(message);
    }

    public static void writeDiagnosticLog(String text) {
        if (diagnosticEnabled) {
            writeLog(text);
        }
    }

    public static void setDiagnosticEnabled(boolean enabled) {
        diagnosticEnabled = enabled;
    }

    public static boolean isDiagnosticEnabled() {
        return diagnosticEnabled;
    }

    private static void writeXposedLog(String message) {
        try {
            XposedBridge.log(message);
        } catch (Throwable ignored) {
        }
    }

    private static void writeAppLogFile(String message) {
        Application app = moduleApplication;
        if (app == null) {
            return;
        }
        try {
            File logDir = app.getExternalFilesDir(null);
            if (logDir == null) {
                return;
            }
            File logFile = new File(logDir, LOG_FILE_NAME);
            String timestamp = DATE_FORMAT.get().format(new Date());
            synchronized (LogUtil.class) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(logFile, true),
                        StandardCharsets.UTF_8
                ))) {
                    writer.write(timestamp);
                    writer.write(' ');
                    writer.write(message);
                    writer.newLine();
                }
            }
        } catch (Throwable throwable) {
            Log.e(TAG, "write app log failed", throwable);
        }
    }
}
