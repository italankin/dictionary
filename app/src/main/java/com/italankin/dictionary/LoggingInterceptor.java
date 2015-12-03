package com.italankin.dictionary;

import android.util.Log;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;

public class LoggingInterceptor implements Interceptor {

    public static final String TAG = "LoggingInterceptor";

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Log.d(TAG, "Request: " + request.uri());

        Response response = chain.proceed(request);

        String bodyString = response.body().string();
        Log.d(TAG, String.format("Response:\n%s", bodyString));

        ResponseBody body = ResponseBody.create(null, bodyString);
        return response.newBuilder().body(body).build();
    }

}
