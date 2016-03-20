/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
