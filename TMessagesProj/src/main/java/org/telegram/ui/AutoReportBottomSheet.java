package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class AutoReportBottomSheet extends BottomSheet {

    private int currentAccount;
    private long dialogId;
    private int reportCount = 7;
    private int timerSeconds = 20;
    private boolean autoMode = false;
    private int completedCount = 0;
    private Handler handler = new Handler();
    private TextView progressText;
    private ProgressBar progressBar;
    private Button startButton;
    private boolean isRunning = false;

    private static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences("auto_report_settings", 0);
    }

    public AutoReportBottomSheet(Activity activity, int account, long dialogId) {
        super(activity, false);
        this.currentAccount = account;
        this.dialogId = dialogId;

        SharedPreferences prefs = getPrefs(activity);
        reportCount = prefs.getInt("report_count", 7);
        timerSeconds = prefs.getInt("timer_seconds", 20);
        autoMode = prefs.getBoolean("auto_mode", false);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        // Title
        TextView title = new TextView(activity);
        title.setText("Auto Report");
        title.setTextSize(20);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(title);

        // Target info
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
        TextView targetInfo = new TextView(activity);
        if (chat != null) {
            String username = chat.username != null ? "@" + chat.username : "Private Group";
            int members = chat.participants_count;
            targetInfo.setText("Target: " + chat.title + "\n" + username + "\nID: " + dialogId + "\nMembers: " + members);
        } else if (user != null) {
            String username = user.username != null ? "@" + user.username : "Private User";
            targetInfo.setText("Target: " + UserObject.getName(user) + "\n" + username + "\nID: " + dialogId);
        }
        targetInfo.setTextColor(Theme.getColor(Theme.key_dialogTextGray));
        targetInfo.setPadding(0, 16, 0, 16);
        layout.addView(targetInfo);

        // Count setting
        TextView countLabel = new TextView(activity);
        countLabel.setText("Report Count: " + reportCount);
        countLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(countLabel);

        SeekBar countSeek = new SeekBar(activity);
        countSeek.setMax(50);
        countSeek.setProgress(reportCount);
        countSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                reportCount = Math.max(1, p);
                countLabel.setText("Report Count: " + reportCount);
                getPrefs(activity).edit().putInt("report_count", reportCount).apply();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        layout.addView(countSeek);

        // Timer setting
        TextView timerLabel = new TextView(activity);
        timerLabel.setText("Timer: " + timerSeconds + " sec");
        timerLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(timerLabel);

        SeekBar timerSeek = new SeekBar(activity);
        timerSeek.setMax(300);
        timerSeek.setProgress(timerSeconds);
        timerSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                timerSeconds = Math.max(5, p);
                timerLabel.setText("Timer: " + timerSeconds + " sec");
                getPrefs(activity).edit().putInt("timer_seconds", timerSeconds).apply();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        layout.addView(timerSeek);

        // Auto mode toggle
        LinearLayout autoRow = new LinearLayout(activity);
        autoRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView autoLabel = new TextView(activity);
        autoLabel.setText("Auto Mode");
        autoLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        autoLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Switch autoSwitch = new Switch(activity);
        autoSwitch.setChecked(autoMode);
        autoSwitch.setOnCheckedChangeListener((btn, checked) -> {
            autoMode = checked;
            getPrefs(activity).edit().putBoolean("auto_mode", autoMode).apply();
        });
        autoRow.addView(autoLabel);
        autoRow.addView(autoSwitch);
        layout.addView(autoRow);

        // Progress
        progressText = new TextView(activity);
        progressText.setText("0/" + reportCount);
        progressText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        progressText.setPadding(0, 20, 0, 8);
        layout.addView(progressText);

        progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(reportCount);
        progressBar.setProgress(0);
        layout.addView(progressBar);

        // Start button
        startButton = new Button(activity);
        startButton.setText("Start Reporting");
        startButton.setOnClickListener(v -> {
            if (!isRunning) {
                completedCount = 0;
                progressBar.setMax(reportCount);
                isRunning = true;
                startButton.setText("Stop");
                startReporting(activity);
            } else {
                stopReporting();
                startButton.setText("Start Reporting");
            }
        });
        layout.addView(startButton);

        setCustomView(layout);
    }

    private void startReporting(Context ctx) {
        if (!isRunning || completedCount >= reportCount) {
            if (completedCount >= reportCount) {
                showComplete(ctx);
            }
            return;
        }
        sendReport();
        if (autoMode) {
            handler.postDelayed(() -> startReporting(ctx), timerSeconds * 1000L);
        }
    }

    private void sendReport() {
        TLRPC.TL_messages_report req = new TLRPC.TL_messages_report();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        req.option = new byte[]{};
        req.message = "Spam";
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            completedCount++;
            progressText.setText(completedCount + "/" + reportCount);
            progressBar.setProgress(completedCount);
            if (completedCount >= reportCount) {
                stopReporting();
            }
        }));
    }

    private void stopReporting() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        startButton.setText("Start Reporting");
    }

    private void showComplete(Context ctx) {
        Toast.makeText(ctx, "✅ " + completedCount + "/" + reportCount + " Reports Complete!", Toast.LENGTH_LONG).show();
    }
}
