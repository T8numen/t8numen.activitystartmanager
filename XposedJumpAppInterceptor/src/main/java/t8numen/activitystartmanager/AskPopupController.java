package t8numen.activitystartmanager;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AskPopupController {
    private static final int TYPE_SYSTEM_ALERT = 2003;
    private static AskPopupController sInstance;

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable countdownRunnable = this::updateCountdown;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ALC-ask-popup");
        thread.setDaemon(true);
        return thread;
    });
    private final WindowManager windowManager;
    private View popupView;
    private TextView countdownView;
    private Toast copyToast;
    private String currentRequestId;
    private long currentExpiresAt;

    private AskPopupController(Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
    }

    static synchronized void init(Context context) {
        if (context == null || sInstance != null) {
            return;
        }
        sInstance = new AskPopupController(context);
        sInstance.registerReceiver();
        sInstance.registerObserver();
        sInstance.onAskUpdated();
        LogUtil.writeLog("ask popup controller initialized in module app");
    }

    static synchronized void refresh() {
        if (sInstance != null) {
            sInstance.onAskUpdated();
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(ModuleConfig.ACTION_ASK_UPDATED);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                onAskUpdated();
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }
    }

    private void registerObserver() {
        try {
            context.getContentResolver().registerContentObserver(
                    ModuleConfig.PENDING_ASK_URI,
                    false,
                    new ContentObserver(mainHandler) {
                        @Override
                        public void onChange(boolean selfChange) {
                            onAskUpdated();
                        }
                    }
            );
        } catch (Throwable throwable) {
            LogUtil.writeLog("ask observer register failed: " + throwable);
        }
    }

    private void onAskUpdated() {
        executor.execute(() -> {
            PendingAskState state = PendingAskStore.peek(context);
            mainHandler.post(() -> showOrDismiss(state));
        });
    }

    private void showOrDismiss(PendingAskState state) {
        dismissCurrent();
        if (state == null || state.isExpired()) {
            return;
        }
        currentRequestId = state.getRequestId();
        currentExpiresAt = state.getExpiresAt();
        popupView = buildPopupView(state);
        if (!addPopupView(popupView)) {
            PendingAskStore.clearAsync(context);
            currentRequestId = null;
            popupView = null;
            return;
        }
        updateCountdown();
        long delay = Math.max(1L, state.getExpiresAt() - SystemClock.elapsedRealtime());
        mainHandler.postDelayed(() -> timeoutIfCurrent(state.getRequestId()), delay);
        LogUtil.writeDiagnosticLog("ask popup shown request=" + state.getRequestId()
                + " source=" + state.getSourceRef().getDisplayName()
                + " target=" + state.getTargetRef().getDisplayName());
    }

    private View buildPopupView(PendingAskState state) {
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Color.TRANSPARENT);
        root.setClickable(true);
        root.setOnClickListener(view -> reject(state.getRequestId()));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(14);
        card.setPadding(padding, padding, padding, padding);
        card.setClickable(true);
        card.setOnClickListener(view -> {
        });
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(250, 250, 250));
        background.setCornerRadius(dp(18));
        card.setBackground(background);
        if (Build.VERSION.SDK_INT >= 21) {
            card.setElevation(dp(8));
        }

        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.BOTTOM);
        card.addView(headerRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView titleView = new TextView(context);
        titleView.setText("Activity链式启动器");
        titleView.setTextColor(Color.rgb(28, 28, 28));
        titleView.setTextSize(15);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headerRow.addView(titleView, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        countdownView = new TextView(context);
        countdownView.setTextColor(Color.rgb(120, 120, 120));
        countdownView.setTextSize(12);
        countdownView.setGravity(Gravity.END | Gravity.BOTTOM);
        headerRow.addView(countdownView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout contentRow = new LinearLayout(context);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);
        contentRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams contentRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        contentRowParams.topMargin = dp(12);
        card.addView(contentRow, contentRowParams);

        contentRow.addView(buildAppColumn(state.getSourceRef()), weightedColumnParams());
        SpaceView space = new SpaceView(context);
        contentRow.addView(space, new LinearLayout.LayoutParams(dp(12), 1));
        contentRow.addView(buildAppColumn(state.getTargetRef()), weightedColumnParams());

        LinearLayout buttonRow = new LinearLayout(context);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams buttonRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonRowParams.topMargin = dp(12);
        card.addView(buttonRow, buttonRowParams);

        Button agreeButton = new Button(context);
        agreeButton.setText("同意");
        agreeButton.setAllCaps(false);
        agreeButton.setOnClickListener(view -> approve(state.getRequestId()));
        buttonRow.addView(agreeButton, weightedButtonParams());

        Button rejectButton = new Button(context);
        rejectButton.setText("拒绝");
        rejectButton.setAllCaps(false);
        rejectButton.setOnClickListener(view -> reject(state.getRequestId()));
        LinearLayout.LayoutParams rejectParams = weightedButtonParams();
        rejectParams.leftMargin = dp(12);
        buttonRow.addView(rejectButton, rejectParams);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        cardParams.leftMargin = dp(8);
        cardParams.rightMargin = dp(8);
        cardParams.bottomMargin = dp(12);
        root.addView(card, cardParams);
        return root;
    }

    private LinearLayout buildAppColumn(ActivityRef ref) {
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        String detail = buildDetailText(ref);
        column.setLongClickable(true);
        column.setOnLongClickListener(view -> {
            copyText(detail);
            return true;
        });

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        column.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        ImageView iconView = new ImageView(context);
        iconView.setImageDrawable(loadIcon(ref));
        header.addView(iconView, new LinearLayout.LayoutParams(dp(32), dp(32)));

        TextView labelView = new TextView(context);
        labelView.setText(loadLabel(ref.getPackageName()));
        labelView.setTextColor(Color.rgb(28, 28, 28));
        labelView.setTextSize(15);
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labelView.setSingleLine(true);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.leftMargin = dp(8);
        header.addView(labelView, labelParams);

        TextView detailView = new TextView(context);
        detailView.setText(detail);
        detailView.setTextColor(Color.rgb(80, 80, 80));
        detailView.setTextSize(12);
        detailView.setSingleLine(true);
        detailView.setEllipsize(TextUtils.TruncateAt.END);
        detailView.setLongClickable(true);
        detailView.setOnLongClickListener(view -> {
            copyText(detail);
            return true;
        });
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.topMargin = dp(6);
        column.addView(detailView, detailParams);

        return column;
    }

    private boolean addPopupView(View view) {
        if (!canDrawOverlays()) {
            LogUtil.writeLog("ask popup blocked: overlay permission is not granted");
            return false;
        }
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : TYPE_SYSTEM_ALERT;
        try {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    android.graphics.PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.BOTTOM;
            params.x = 0;
            params.y = dp(12);
            windowManager.addView(view, params);
            return true;
        } catch (Throwable throwable) {
            LogUtil.writeLog("ask popup add failed type=" + type + ": " + throwable);
            return false;
        }
    }

    private void approve(String requestId) {
        dismissIfCurrent(requestId);
        executor.execute(() -> {
            PendingAskState state = PendingAskStore.peek(context);
            if (state == null || !requestId.equals(state.getRequestId())) {
                return;
            }
            PendingAskStore.clear(context);
            sendDecision(requestId, ModuleConfig.ASK_DECISION_APPROVE);
            LogUtil.writeDiagnosticLog("ask approve decision sent request=" + requestId
                    + " target=" + state.getTargetRef().getDisplayName());
        });
    }

    private void reject(String requestId) {
        dismissIfCurrent(requestId);
        executor.execute(() -> {
            PendingAskState state = PendingAskStore.peek(context);
            if (state != null && requestId.equals(state.getRequestId())) {
                PendingAskStore.clear(context);
                sendDecision(requestId, ModuleConfig.ASK_DECISION_REJECT);
                LogUtil.writeDiagnosticLog("ask rejected request=" + requestId);
            }
        });
    }

    private void timeoutIfCurrent(String requestId) {
        if (!requestId.equals(currentRequestId)) {
            return;
        }
        dismissCurrent();
        executor.execute(() -> {
            PendingAskState state = PendingAskStore.peek(context);
            if (state != null && requestId.equals(state.getRequestId())) {
                PendingAskStore.clear(context);
                sendDecision(requestId, ModuleConfig.ASK_DECISION_REJECT);
                LogUtil.writeDiagnosticLog("ask timed out request=" + requestId);
            }
        });
    }

    private void dismissIfCurrent(String requestId) {
        if (requestId.equals(currentRequestId)) {
            dismissCurrent();
        }
    }

    private void dismissCurrent() {
        mainHandler.removeCallbacks(countdownRunnable);
        if (popupView != null) {
            try {
                windowManager.removeViewImmediate(popupView);
            } catch (Throwable throwable) {
                LogUtil.writeLog("ask popup remove failed: " + throwable);
            }
        }
        popupView = null;
        countdownView = null;
        currentRequestId = null;
        currentExpiresAt = 0L;
    }

    private void sendDecision(String requestId, String decision) {
        try {
            Intent intent = new Intent(ModuleConfig.ACTION_ASK_DECISION);
            intent.putExtra(ModuleConfig.EXTRA_ASK_REQUEST_ID, requestId);
            intent.putExtra(ModuleConfig.EXTRA_ASK_DECISION, decision);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.sendBroadcast(intent);
        } catch (Throwable throwable) {
            LogUtil.writeLog("ask decision send failed request=" + requestId + ": " + throwable);
        }
    }

    private String buildDetailText(ActivityRef ref) {
        return ref.getDisplayName();
    }

    private Drawable loadIcon(ActivityRef ref) {
        PackageManager packageManager = context.getPackageManager();
        if (ref.getClassName() != null && !ref.getClassName().isEmpty()) {
            try {
                return packageManager.getActivityIcon(new ComponentName(ref.getPackageName(), ref.getClassName()));
            } catch (Throwable ignored) {
            }
        }
        try {
            return packageManager.getApplicationIcon(ref.getPackageName());
        } catch (Throwable throwable) {
            return packageManager.getDefaultActivityIcon();
        }
    }

    private String loadLabel(String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            CharSequence label = packageManager.getApplicationLabel(applicationInfo);
            if (label != null && label.length() > 0) {
                return label.toString();
            }
        } catch (Throwable ignored) {
        }
        return packageName;
    }

    private void copyText(String text) {
        try {
            ClipboardManager clipboardManager =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText("Activity启动信息", text));
            if (copyToast != null) {
                copyToast.cancel();
            }
            copyToast = Toast.makeText(context, "已复制", Toast.LENGTH_SHORT);
            copyToast.show();
            mainHandler.postDelayed(() -> {
                if (copyToast != null) {
                    copyToast.cancel();
                    copyToast = null;
                }
            }, 1200L);
        } catch (Throwable throwable) {
            LogUtil.writeLog("ask copy failed: " + throwable);
        }
    }

    private void updateCountdown() {
        if (countdownView == null || currentRequestId == null || currentExpiresAt <= 0L) {
            return;
        }
        long remainingMs = Math.max(0L, currentExpiresAt - SystemClock.elapsedRealtime());
        int seconds = (int) Math.ceil(remainingMs / 1000.0d);
        countdownView.setText(seconds + "秒后忽略请求");
        if (remainingMs > 0L) {
            mainHandler.postDelayed(countdownRunnable, 1000L);
        }
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context);
    }

    private LinearLayout.LayoutParams weightedColumnParams() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class SpaceView extends View {
        SpaceView(Context context) {
            super(context);
        }
    }
}
