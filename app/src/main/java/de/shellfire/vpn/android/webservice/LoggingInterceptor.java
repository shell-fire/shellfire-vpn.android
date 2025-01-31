package de.shellfire.vpn.android.webservice;

import android.util.Log;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSource;

public class LoggingInterceptor implements Interceptor {
    private static final String TAG = "LoggingInterceptor";
    private static final int MAX_LOG_LENGTH = 4000; // Maximum length of a single log statement

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        if (response.isSuccessful() && response.body() != null) {
            BufferedSource source = response.body().source();
            source.request(Long.MAX_VALUE); // Buffer the entire body.
            Buffer buffer = source.buffer();
            String responseBodyString = buffer.clone().readString(Charset.forName("UTF-8"));
            logLongString("Full JSON Response: " + responseBodyString);
        }

        return response;
    }

    private void logLongString(String message) {
        int length = message.length();
        for (int i = 0; i < length; i += MAX_LOG_LENGTH) {
            int end = Math.min(length, i + MAX_LOG_LENGTH);
            Log.d(TAG, message.substring(i, end));
        }
    }
}
