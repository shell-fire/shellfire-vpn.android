package de.shellfire.vpn.android;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.shellfire.vpn.android.utils.CommonUtils;
import de.shellfire.vpn.android.utils.Util;

public class LogActivity extends AppCompatActivity {
    private static final String LOG_TEXT = "logText";
    private static final String TAG = "LogSectionFragment";
    public static final Handler uiHandler;
    protected final ExecutorService executorService = Executors.newFixedThreadPool(4);
    protected final Handler mainHandler = new Handler(Looper.getMainLooper());

    static {
        uiHandler = new Handler(Looper.getMainLooper());
    }

    private EditText msgFromUser;
    private RelativeLayout mainLayout;
    private Button sendLog;
    private CheckBox includeLogsCheckbox;
    private LogRepository logRepository;

    public LogActivity() {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (CommonUtils.isTablet(this)) {
            setContentView(R.layout.activity_log_tablet);
        } else {
            setContentView(R.layout.activity_log);

        }

        Log.d(TAG, "onCreate called");

        msgFromUser = findViewById(R.id.msg_from_user);
        msgFromUser.setImeOptions(EditorInfo.IME_ACTION_DONE);
        msgFromUser.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        includeLogsCheckbox = findViewById(R.id.include_logs_checkbox);
        mainLayout = findViewById(R.id.mainLayout);
        sendLog = findViewById(R.id.sendLogToShellfire);
        logRepository = LogRepository.getInstance(this);

        initListeners();

        // Ensure the submit button is visible when the keyboard is displayed
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!msgFromUser.getText().toString().isEmpty()) {
            outState.putString(LOG_TEXT, msgFromUser.getText().toString());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    public void initListeners() {
        mainLayout.setOnClickListener(view -> {
            msgFromUser.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(msgFromUser, InputMethodManager.SHOW_IMPLICIT);
        });

        msgFromUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 0) {
                    sendLog.setAlpha(1f);
                    sendLog.setEnabled(true);
                } else {
                    sendLog.setAlpha(0.4f);
                    sendLog.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    public void onClickSendLogToShellfire(View view) {
        Log.d(TAG, "onClickSendLogToShellfire - start()");
        Log.d(TAG, "onClickSendLogToShellfire - logSectionFragment != null, appending user message");

        Runnable action = this::sendLogToShellfire;

        if (Util.isInternetAvailable(this)) {
            action.run();
        } else {
            showDialogInternetRequired(action);
        }
    }

    public void onClickButtonBack(View view) {
        finish();
    }

    private void showDialogInternetRequired(final Runnable action) {
        int id = R.string.retry;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.network_problem_title));
        alert.setMessage(getString(R.string.network_problem_message));
        alert.setPositiveButton(getString(id), (arg0, arg1) -> {
            if (Util.isInternetAvailable(LogActivity.this)) {
                action.run();
            } else {
                showDialogInternetRequired(action);
            }
        });
        alert.show();
    }

    protected void sendLogToShellfire() {
        mainHandler.post(() -> {
            ProgressDialog dialog = new ProgressDialog(LogActivity.this);
            dialog.setMessage(getString(R.string.send_feedback_progress));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();

            executorService.execute(() -> {
                try {
                    LiveData<Boolean> resultLiveData;
                    Log.d(TAG, "onClickSendLogToShellfire - msgFromUser: " + msgFromUser.getText().toString());
                    if (includeLogsCheckbox.isChecked()) {
                        Reader logcatReader = getLogCatLogReader();
                        resultLiveData = logRepository.sendLog(msgFromUser.getText().toString(), logcatReader);
                    } else {
                        resultLiveData = logRepository.sendLog(msgFromUser.getText().toString());
                    }

                    mainHandler.post(() -> resultLiveData.observe(this, result -> {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        CommonUtils.hideKeyboard(this);

                        if (result) {
                            Toast.makeText(this, R.string.feedback_sent_successfully, Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, R.string.feedback_not_sent, Toast.LENGTH_SHORT).show();
                        }
                    }));

                } catch (IOException e) {
                    Util.handleException(e, LogActivity.this);
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    CommonUtils.hideKeyboard(this);
                    Toast.makeText(this, R.string.feedback_not_sent, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private Reader getLogCatLogReader() throws IOException {
        Process process = Runtime.getRuntime().exec("logcat -d");
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }
}
