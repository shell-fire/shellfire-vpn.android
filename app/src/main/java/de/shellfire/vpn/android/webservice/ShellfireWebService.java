package de.shellfire.vpn.android.webservice;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;

import de.shellfire.vpn.android.Server;
import de.shellfire.vpn.android.auth.SecurePreferences;
import de.shellfire.vpn.android.webservice.model.BaseResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShellfireWebService {

    private static final String TAG = "ShellfireWebService";
    private static final int DNS_SLEEP_WAIT = 250;
    private static ShellfireWebService instance;
    private final Context context;

    private Server selectedServer;
    private boolean dnsOkay = false;

    private ShellfireWebService(Context context) {
        this.context = context;
    }

    public static ShellfireWebService getInstance(Context context) {
        if (instance == null) {
            instance = new ShellfireWebService(context);
        }
        return instance;
    }

    public String getToken() {
        try {
            SecurePreferences securePreferences = SecurePreferences.getInstance(context);
            return securePreferences.getToken();
        } catch (Exception e) {
            Log.e("ShellfireWebService", "Failed to get token", e);
            return null;
        }
    }

    public void setToken(String token) {
        try {
            SecurePreferences securePreferences = SecurePreferences.getInstance(context);
            securePreferences.saveToken(token);
        } catch (Exception e) {
            Log.e("ShellfireWebService", "Failed to save token", e);
        }
    }

    public void clearToken() {
        try {
            SecurePreferences securePreferences = SecurePreferences.getInstance(context);
            securePreferences.clearToken();
        } catch (Exception e) {
            Log.e("ShellfireWebService", "Failed to clear token", e);
        }
    }

    public <T extends BaseResponse<R>, R> void makeAsyncCall(Call<T> call, String errorMessage, BaseCallback<R> callback) {
        final String TAG = "makeAsyncCall";
        final int MAX_RETRIES = 3; // Set the maximum number of retries
        final int RETRY_DELAY = 2000; // Set the delay between retries in milliseconds

        Log.d(TAG, "Making async call with URL: " + call.request().url());

        makeCallWithRetry(call, errorMessage, callback, MAX_RETRIES, RETRY_DELAY);
    }

    private <T extends BaseResponse<R>, R> void makeCallWithRetry(Call<T> call, String errorMessage, BaseCallback<R> callback, int retriesLeft, int retryDelay) {
        final String TAG = "makeAsyncCall";

        call.clone().enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NotNull Call<T> call, @NotNull Response<T> response) {
                Log.d(TAG, "Received response with status code: " + response.code());

                if (response.isSuccessful()) {
                    Log.d(TAG, "Response is successful for: " + call.request().url());

                    if (response.body() != null) {
                        Log.d(TAG, "Response body is not null: " + call.request().url());

                        if (response.body().isSuccess()) {
                            Log.d(TAG, "Response indicates success: " + call.request().url());
                            callback.onSuccess(response.body().getData()); // Pass the data
                        } else {
                            Log.d(TAG, "Response indicates failure: " + response.body().getMessage());
                            callback.onFailure(new Exception(response.body().getMessage()));
                        }
                    } else {
                        Log.e(TAG, "Response body is null: " + call.request().url());
                        callback.onFailure(new Exception("Response body is null: " + call.toString()));
                    }
                } else {
                    Log.e(TAG, "Response is not successful: " + response.message());
                    callback.onFailure(new Exception(errorMessage));
                }
            }

            @Override
            public void onFailure(@NotNull Call<T> call, @NotNull Throwable t) {
                Log.e(TAG, "Call failed with error", t);

                if (retriesLeft > 0) {
                    Log.d(TAG, "Retrying... attempts left: " + retriesLeft);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        makeCallWithRetry(call.clone(), errorMessage, callback, retriesLeft - 1, retryDelay);
                    }, retryDelay);
                } else {
                    callback.onFailure(new Exception(t));
                }
            }
        });
    }


    public interface BaseCallback<T> {
        void onSuccess(T response);

        void onFailure(Exception e);
    }

    private class RemoteDnsCheck extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                dnsOkay = false;
                InetAddress addr = InetAddress.getByName("whatismyip.akamai.com");
                dnsOkay = true;
            } catch (UnknownHostException e) {
                // Handle exception
            }
            return null;
        }
    }


}
