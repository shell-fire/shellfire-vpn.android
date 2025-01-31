package de.shellfire.vpn.android;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import java.io.Reader;

import de.shellfire.vpn.android.webservice.JsonWebService;
import de.shellfire.vpn.android.webservice.LogRequestBody;
import de.shellfire.vpn.android.webservice.ShellfireWebService;
import de.shellfire.vpn.android.webservice.WebService;
import de.shellfire.vpn.android.webservice.model.BaseResponse;
import retrofit2.Call;

public class LogRepository {
    private static LogRepository instance;
    private final ShellfireWebService webService;
    private final JsonWebService jsonWebService;
    private final MediatorLiveData<Boolean> sendLogLiveData;

    private LogRepository(Context context) {
        webService = ShellfireWebService.getInstance(context);
        jsonWebService = WebService.getInstance(context).getJsonWebService();

        sendLogLiveData = new MediatorLiveData<>();
    }

    public static synchronized LogRepository getInstance(Context context) {
        if (instance == null) {
            instance = new LogRepository(context);
        }
        return instance;
    }

    public LiveData<Boolean> sendLog(String userMessage, Reader logReader) {
        LogRequestBody requestBody = new LogRequestBody(userMessage, logReader);
        Call<BaseResponse<Void>> call = jsonWebService.sendLog(requestBody);

        webService.makeAsyncCall(call, "Failed to send log to Shellfire", new ShellfireWebService.BaseCallback<Void>() {
            @Override
            public void onSuccess(Void response) {

                sendLogLiveData.postValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                sendLogLiveData.postValue(false);
                // Handle error (optional logging or additional error handling)
            }
        });

        return sendLogLiveData;
    }

    public LiveData<Boolean> sendLog(String string) {
        return sendLog(string, null);
    }
}
