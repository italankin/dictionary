package com.italankin.dictionary.utils;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class CacheInterceptor implements Interceptor {

    private final int maxAge;

    public CacheInterceptor(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        if (response.isSuccessful()) {
            return response.newBuilder()
                    .header("Cache-Control", String.format("public, max-age=%d", maxAge))
                    .build();
        }
        return response;
    }
}
