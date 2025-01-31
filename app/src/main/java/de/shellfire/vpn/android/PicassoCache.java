package de.shellfire.vpn.android;

import android.content.Context;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class PicassoCache {
    private static PicassoCache instance;
    private final Picasso picassoInstance;

    private PicassoCache(Context context) {
        // Set up OkHttp client with disk cache
        File httpCacheDirectory = new File(context.getCacheDir(), "picasso-cache");
        Cache cache = new Cache(httpCacheDirectory, 150 * 1024 * 1024); // 150 MB

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .cache(cache)
                .build();

        picassoInstance = new Picasso.Builder(context)
                .downloader(new OkHttp3Downloader(okHttpClient))
                .build();
    }

    public static synchronized PicassoCache getInstance(Context context) {
        if (instance == null) {
            instance = new PicassoCache(context);
        }
        return instance;
    }
    public void getAndLoadInto(Server server, ImageView imageView, Callback callback) {
        String imageUrl = server.getImageUrl();
        loadImageWithCache(imageUrl, imageView, 0, 0, false, callback);
    }

    public void getAndLoadIntoWithResizeAndCrop(Server server, ImageView imageView, int width, int height, Callback callback) {
        String imageUrl = server.getImageUrl();
        loadImageWithCache(imageUrl, imageView, width, height, true, callback);
    }

    private void loadImageWithCache(String imageUrl, ImageView imageView, int width, int height, boolean resize, Callback callback) {
        RequestCreator request = picassoInstance.load(imageUrl);

        if (resize) {
            request = request.resize(width, height).centerCrop();
        }

        request.into(imageView, callback);


    }


}
