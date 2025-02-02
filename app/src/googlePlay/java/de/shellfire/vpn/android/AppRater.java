package de.shellfire.vpn.android;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.model.ReviewErrorCode;

public class AppRater {

    private static final String DEFAULT_PREF_GROUP = "app_rater";
    private static final String DEFAULT_PREFERENCE_DONT_SHOW = "flag_dont_show";
    private static final String DEFAULT_PREFERENCE_ACTION_COUNT = "action_count";
    private static final String DEFAULT_PREFERENCE_LAST_PROMPT = "last_prompt_time";
    private static final String DEFAULT_TARGET_URI = "market://details?id=%1$s";
    private static final int DEFAULT_ACTIONS_BEFORE_PROMPT = 15;
    private static final int DEFAULT_MINIMUM_DAYS_BETWEEN_PROMPTS = 7;
    private final Context mContext;
    private int mActionsBeforePrompt;
    private int mMinimumDaysBetweenPrompts;
    private String mPrefGroup;
    private String mPreference_actionCount;
    private String mPreference_lastPrompt;

    private static final String TAG = "AppRater";

    public AppRater(final Context context) {
        this(context, context.getPackageName());
    }

    public AppRater(final Context context, final String packageName) {
        if (context == null) {
            throw new RuntimeException("context may not be null");
        }
        mContext = context;
        mActionsBeforePrompt = DEFAULT_ACTIONS_BEFORE_PROMPT;
        mMinimumDaysBetweenPrompts = DEFAULT_MINIMUM_DAYS_BETWEEN_PROMPTS;
        mPrefGroup = DEFAULT_PREF_GROUP;
        mPreference_actionCount = DEFAULT_PREFERENCE_ACTION_COUNT;
        mPreference_lastPrompt = DEFAULT_PREFERENCE_LAST_PROMPT;
    }

    private static void savePreferences(SharedPreferences.Editor editor) {
        if (editor != null) {
            editor.apply();
        }
    }

    public void logAction() {
        SharedPreferences prefs = mContext.getSharedPreferences(mPrefGroup, 0);
        SharedPreferences.Editor editor = prefs.edit();
        long actionCount = prefs.getLong(mPreference_actionCount, 0);
        actionCount++;
        editor.putLong(mPreference_actionCount, actionCount);
        savePreferences(editor);
    }

    public void triggerReview() {
        SharedPreferences prefs = mContext.getSharedPreferences(mPrefGroup, 0);
        SharedPreferences.Editor editor = prefs.edit();
        long actionCount = prefs.getLong(mPreference_actionCount, 0);
        long lastPromptTime = prefs.getLong(mPreference_lastPrompt, 0);

        boolean shouldShow = actionCount >= mActionsBeforePrompt &&
                System.currentTimeMillis() >= (lastPromptTime + (mMinimumDaysBetweenPrompts * DateUtils.DAY_IN_MILLIS));

        Log.d(TAG, "Action count: " + actionCount);
        Log.d(TAG, "Last prompt time: " + lastPromptTime);
        Log.d(TAG, "Should show review: " + shouldShow);

        if (shouldShow) {
            editor.putLong(mPreference_lastPrompt, System.currentTimeMillis());
            savePreferences(editor);
            showInitialDialog();
        }
    }

    public void rateApp() {
        showInitialDialog();
    }

    private void showInitialDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.initial_dialog_title)
                .setMessage(R.string.initial_dialog_message)
                .setPositiveButton(R.string.initial_dialog_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showRateDialog();
                    }
                })
                .setNegativeButton(R.string.initial_dialog_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showFeedbackDialog();
                    }
                })
                .show();
    }

    private void showRateDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.rate_dialog_title)
                .setMessage(R.string.rate_dialog_message)
                .setPositiveButton(R.string.rate_dialog_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        launchReviewFlow();
                    }
                })
                .setNegativeButton(R.string.rate_dialog_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNeutralButton(R.string.rate_dialog_neutral, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
    private void showFeedbackDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.feedback_dialog_title)
                .setMessage(R.string.feedback_dialog_message)
                .setPositiveButton(R.string.feedback_dialog_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        redirectToFeedback();
                    }
                })
                .setNegativeButton(R.string.feedback_dialog_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }


    private void launchReviewFlow() {
        // Ensure that we have an activity context
        if (!(mContext instanceof Activity)) {
            Log.e(TAG, "Context is not an instance of Activity");
            return;
        }

        ReviewManager manager = ReviewManagerFactory.create(mContext);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "ReviewInfo request successful");
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow((Activity) mContext, reviewInfo);
                flow.addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Log.d(TAG, "Review flow completed successfully");
                    } else {
                        Log.e(TAG, "Review flow failed: " + task1.getException());
                    }
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                });
            } else {
                // There was some problem, log or handle the error code.
                Exception exception = task.getException();
                if (exception instanceof ReviewException) {
                    @ReviewErrorCode int reviewErrorCode = ((ReviewException)    exception).getErrorCode();
                    Log.e(TAG, "ReviewInfo request failed with error code: " + reviewErrorCode);
                    Log.e(TAG, "ReviewInfo request failed with exception: " +exception);

                } else {
                    Log.e(TAG, "Error occured (not ReviewException): " + exception.toString(), exception);
                }
            }
        });
    }


    private void redirectToFeedback() {
        Intent intent = new Intent(mContext, LogActivity.class);
        mContext.startActivity(intent);
    }
}
