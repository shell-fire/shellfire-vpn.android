package de.shellfire.vpn.android.webservice;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.Locale;

import de.shellfire.vpn.android.webservice.model.AliasListContainer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WebService {
    private static WebService instance;
    private static Context context;
    private final JsonWebService jsonWebService;
    private final ShellfireWebService shellfireWebService;

    private WebService(Context context) {
        WebService.context = context;
        this.shellfireWebService = ShellfireWebService.getInstance(context);
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                // .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Content-Type", "application/json")
                            .header("x-shellfirevpn-client-os", "android")
                            .header("x-shellfirevpn-client-arch", "java")
                            .header("x-shellfirevpn-client-lang", Locale.getDefault().getLanguage())
                            .header("x-shellfirevpn-client-version", "5.0");

                    String token = shellfireWebService.getToken();
                    if (token != null) {
                        requestBuilder.header("x-authorization-token", token);
                    }

                    Request request = requestBuilder.method(original.method(), original.body()).build();
                    Response response = chain.proceed(request);

                    // Log request and response headers
                    // System.out.println("Request URL: " + request.url());
                    // System.out.println("Request Headers: " + request.headers());
                    // System.out.println("Response Body: " + response.body().source().peek());

                    // System.out.println("Response Headers: " + response.headers());
                    return response;
                });

        OkHttpClient client = builder.build();

        Gson gson = new GsonBuilder()
                // .registerTypeAdapterFactory(new LoggingTypeAdapterFactory())
                .registerTypeAdapter(AliasListContainer.class, new AliasListContainerDeserializer())
                .registerTypeAdapter(Date.class, new UnixTimestampDateDeserializer())
                /*
                .registerTypeAdapter(AliasListContainer.class, new LoggingDeserializer<>(new JsonDeserializer<AliasListContainer>() {
                    @Override
                    public AliasListContainer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        return new Gson().fromJson(json, AliasListContainer.class);
                    }
                }))
                .registerTypeAdapter(AliasList.class, new LoggingDeserializer<>(new JsonDeserializer<AliasList>() {
                    @Override
                    public AliasList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        return new Gson().fromJson(json, AliasList.class);
                    }
                }))
                */

                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.shellfire.de/webservice/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();

        jsonWebService = retrofit.create(JsonWebService.class);
    }

    public static WebService getInstance(Context context) {
        if (instance == null) {
            instance = new WebService(context);
        }
        return instance;
    }

    public JsonWebService getJsonWebService() {
        return jsonWebService;
    }
}
