package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

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
    private String savedComment = "";
    private String selectedReportType = "Spam";
    private byte[] selectedReportOption = new byte[]{};
    private TextView commentPreview;
    private TextView timerCountdown;

    private static final String[] REPORT_TYPES = {"Spam", "Violence", "Child Abuse", "Copyright", "Illegal Drugs", "Personal Information", "Pornography", "Terrorist Activity", "Other"};

    private static SharedPreferences getPrefs(Context ctx, int account) {
        return ctx.getSharedPreferences("auto_report_global", 0);
    }

    public AutoReportBottomSheet(Activity activity, int account, long dialogId) {
        super(activity, false);
        this.currentAccount = account;
        this.dialogId = dialogId;

        SharedPreferences prefs = getPrefs(activity, account);
        reportCount = prefs.getInt("report_count", 7);
        timerSeconds = prefs.getInt("timer_seconds", 20);
        autoMode = prefs.getBoolean("auto_mode", false);
        savedComment = prefs.getString("saved_comment", "");
        selectedReportType = prefs.getString("report_type", "Spam");

        ScrollView scrollView = new ScrollView(activity);
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);

        // ── Title ──────────────────────────────────────
        TextView title = new TextView(activity);
        title.setText("⚡ Auto Report");
        title.setTextSize(22);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setPadding(0, 0, 0, 16);
        layout.addView(title);

        // ── Target Info ────────────────────────────────
        if (dialogId != 0) {
            TextView targetInfo = new TextView(activity);
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (chat != null) {
                String username = chat.username != null ? "@" + chat.username : "Private Group";
                int members = chat.participants_count;
                targetInfo.setText("🎯 " + chat.title + "\n" + username + "\n🆔 " + dialogId + "\n👥 " + members + " members");
            } else if (user != null) {
                String username = user.username != null ? "@" + user.username : "Private";
                targetInfo.setText("🎯 " + UserObject.getFirstName(user) + "\n" + username + "\n🆔 " + dialogId);
            }
            targetInfo.setTextColor(Theme.getColor(Theme.key_dialogTextGray));
            targetInfo.setBackgroundColor(0x11000000);
            targetInfo.setPadding(16, 16, 16, 16);
            targetInfo.setPadding(0, 0, 0, 20);
            layout.addView(targetInfo);
        }

        // ── Report Type ────────────────────────────────
        TextView typeLabel = new TextView(activity);
        typeLabel.setText("📋 Report Type");
        typeLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        typeLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        layout.addView(typeLabel);

        Spinner typeSpinner = new Spinner(activity);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, REPORT_TYPES);
        typeSpinner.setAdapter(typeAdapter);
        for (int i = 0; i < REPORT_TYPES.length; i++) {
            if (REPORT_TYPES[i].equals(selectedReportType)) {
                typeSpinner.setSelection(i);
                break;
            }
        }
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedReportType = REPORT_TYPES[pos];
                getPrefs(activity, account).edit().putString("report_type", selectedReportType).apply();
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        layout.addView(typeSpinner);

        // ── Comment ────────────────────────────────────
        TextView commentLabel = new TextView(activity);
        commentLabel.setText("💬 Comment (auto-saved)");
        commentLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        commentLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        commentLabel.setPadding(0, 20, 0, 4);
        layout.addView(commentLabel);

        EditText commentEdit = new EditText(activity);
        commentEdit.setHint("Enter report comment...");
        commentEdit.setText(savedComment);
        commentEdit.setLines(2);
        commentEdit.setMaxLines(3);
        commentEdit.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                savedComment = s.toString();
                getPrefs(activity, account).edit().putString("saved_comment", savedComment).apply();
            }
            public void afterTextChanged(android.text.Editable s) {}
        });
        layout.addView(commentEdit);

        // ── Count ──────────────────────────────────────
        TextView countLabel = new TextView(activity);
        countLabel.setText("🔢 Report Count: " + reportCount);
        countLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        countLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        countLabel.setPadding(0, 20, 0, 4);
        layout.addView(countLabel);

        SeekBar countSeek = new SeekBar(activity);
        countSeek.setMax(50);
        countSeek.setProgress(reportCount);
        countSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                reportCount = Math.max(1, p);
                countLabel.setText("🔢 Report Count: " + reportCount);
                if (progressBar != null) progressBar.setMax(reportCount);
                getPrefs(activity, account).edit().putInt("report_count", reportCount).apply();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        layout.addView(countSeek);

        // ── Timer ──────────────────────────────────────
        TextView timerLabel = new TextView(activity);
        timerLabel.setText("⏱️ Timer: " + timerSeconds + " sec");
        timerLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        timerLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        timerLabel.setPadding(0, 20, 0, 4);
        layout.addView(timerLabel);

        SeekBar timerSeek = new SeekBar(activity);
        timerSeek.setMax(300);
        timerSeek.setProgress(timerSeconds);
        timerSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                timerSeconds = Math.max(5, p);
                timerLabel.setText("⏱️ Timer: " + timerSeconds + " sec");
                getPrefs(activity, account).edit().putInt("timer_seconds", timerSeconds).apply();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        layout.addView(timerSeek);

        // ── Auto Mode ──────────────────────────────────
        LinearLayout autoRow = new LinearLayout(activity);
        autoRow.setOrientation(LinearLayout.HORIZONTAL);
        autoRow.setPadding(0, 20, 0, 8);
        TextView autoLabel = new TextView(activity);
        autoLabel.setText("⚡ Auto Mode");
        autoLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        autoLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        autoLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Switch autoSwitch = new Switch(activity);
        autoSwitch.setChecked(autoMode);
        autoSwitch.setOnCheckedChangeListener((btn, checked) -> {
            autoMode = checked;
            getPrefs(activity, account).edit().putBoolean("auto_mode", autoMode).apply();
        });
        autoRow.addView(autoLabel);
        autoRow.addView(autoSwitch);
        layout.addView(autoRow);

        // ── Timer Countdown ────────────────────────────
        timerCountdown = new TextView(activity);
        timerCountdown.setText("");
        timerCountdown.setTextColor(0xFF2196F3);
        timerCountdown.setGravity(Gravity.CENTER);
        timerCountdown.setPadding(0, 8, 0, 8);
        layout.addView(timerCountdown);

        // ── Progress ───────────────────────────────────
        progressText = new TextView(activity);
        progressText.setText("0/" + reportCount);
        progressText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        progressText.setGravity(Gravity.CENTER);
        progressText.setPadding(0, 16, 0, 4);
        layout.addView(progressText);

        progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(reportCount);
        progressBar.setProgress(0);
        layout.addView(progressBar);

        // ── Start Button ───────────────────────────────
        startButton = new Button(activity);
        startButton.setText("▶️ Start Reporting");
        startButton.setTextColor(0xFFFFFFFF);
        startButton.setBackgroundColor(0xFF2196F3);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 120);
        btnParams.setMargins(0, 20, 0, 0);
        startButton.setLayoutParams(btnParams);
        startButton.setOnClickListener(v -> {
            if (!isRunning) {
                completedCount = 0;
                progressBar.setMax(reportCount);
                progressBar.setProgress(0);
                progressText.setText("0/" + reportCount);
                isRunning = true;
                startButton.setText("⏹ Stop");
                startButton.setBackgroundColor(0xFFE53935);
                startReporting(activity);
            } else {
                stopReporting();
            }
        });
        layout.addView(startButton);

        scrollView.addView(layout);
        setCustomView(scrollView);
    }

    private void startReporting(Context ctx) {
        if (!isRunning || completedCount >= reportCount) {
            if (completedCount >= reportCount) {
                showComplete(ctx);
            }
            isRunning = false;
            return;
        }
        sendReport(ctx);
    }

    private void sendReport(Context ctx) {
        TLRPC.TL_messages_report req = new TLRPC.TL_messages_report();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        req.option = selectedReportOption;
        req.message = TextUtils.isEmpty(savedComment) ? selectedReportType : savedComment;

        org.telegram.messenger.ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            completedCount++;
            progressText.setText(completedCount + "/" + reportCount);
            progressBar.setProgress(completedCount);

            if (completedCount >= reportCount) {
                stopReporting();
                showComplete(ctx);
                return;
            }

            if (autoMode && isRunning) {
                startCountdown(ctx, timerSeconds);
            }
        }));
    }

    private void startCountdown(Context ctx, int seconds) {
        final int[] remaining = {seconds};
        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    timerCountdown.setText("");
                    return;
                }
                if (remaining[0] <= 0) {
                    timerCountdown.setText("Sending...");
                    sendReport(ctx);
                } else {
                    timerCountdown.setText("⏱️ Next report in " + remaining[0] + " sec");
                    remaining[0]--;
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(countdownRunnable);
    }

    private void stopReporting() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        timerCountdown.setText("");
        startButton.setText("▶️ Start Reporting");
        startButton.setBackgroundColor(0xFF2196F3);
    }

    private void showComplete(Context ctx) {
        AndroidUtilities.runOnUIThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setTitle("✅ Complete!");
            builder.setMessage(completedCount + "/" + reportCount + " reports sent successfully!\nType: " + selectedReportType);
            builder.setPositiveButton("OK", null);
            builder.show();
        });
    }
}
