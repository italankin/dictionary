package ga.italankin.translate;

import android.os.AsyncTask;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class LanguagesTask extends AsyncTask<String, Void, LanguagesTask.Result> {

    private OkHttpClient client;
    private Callbacks callbacks;

    public LanguagesTask(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    protected void onPreExecute() {
        client = new OkHttpClient();
    }

    @Override
    protected Result doInBackground(String... params) {
        Result result = new Result();
        try {
            TranslatorHelper helper = new TranslatorHelper();
            String url = helper.langsUrl(params[0], Utils.API_KEY);

            Log.d(Utils.TAG, "LanguagesTask.url: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();

            if (isCancelled()) {
                return null;
            }

            result.code = response.code();

            if (response.code() != 200) {
                return result;
            }

            JSONObject json = new JSONObject(response.body().string());

            if (!json.has("langs")) {
                return result;
            }

            json = json.getJSONObject("langs");

            result.languages = new ArrayList<>(json.length());

            Iterator<String> iter = json.keys();
            String key;
            while (iter.hasNext()) {
                key = iter.next();
                result.languages.add(new Language(key, json.getString(key)));
            }

            Collections.sort(result.languages);

            return result;
        } catch (IOException e) {
            Log.e(Utils.TAG, e.toString());
            result.code = 0;
        } catch (JSONException e) {
            Log.e(Utils.TAG, e.toString());
            result.code = 1;
        } catch (Exception e) {
            Log.e(Utils.TAG, e.toString());
            result.code = 2;
        }
        return result;
    }

    @Override
    protected void onPostExecute(Result result) {
        Log.d(Utils.TAG, "LanguagesTask.Result.code: " + result.code);
        if (callbacks != null) {
            callbacks.onLanguagesTaskResult(result);
        }
    }


    public interface Callbacks {

        void onLanguagesTaskResult(Result result);

    }

    public static class Result {
        public int code;
        public ArrayList<Language> languages;
    }
}