package com.italankin.dictionary.utils;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NetworkInterceptor implements Interceptor {

    public static final String TAG = "[NWRK]";

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Log.d(TAG, "Request: " + request.url().toString());

        Response response = chain.proceed(request);

        String bodyString = response.body().string();
        Log.d(TAG, String.format("Response:\n%s", bodyString));

        ResponseBody body = ResponseBody.create(null, bodyString);
        return response.newBuilder().body(body).build();
    }

}
