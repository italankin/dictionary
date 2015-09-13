package ga.italankin.translate;

import android.os.AsyncTask;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class TranslateTask extends AsyncTask<String, Void, TranslateTask.Result> {

    private OkHttpClient client;
    private Callbacks callbacks;

    public TranslateTask(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    protected void onPreExecute() {
        client = new OkHttpClient();
        if (callbacks != null) {
            callbacks.onTranslateTaskPre();
        }
    }

    @Override
    protected Result doInBackground(String... params) {
        Result result = new Result();
        try {
            TranslatorHelper helper;
            if (params.length == 2) {
                helper = new TranslatorHelper(params[1]);
            } else {
                helper = new TranslatorHelper(params[1], params[2]);
            }
            String url = helper.translateUrl(params[0], Utils.API_KEY);

            Log.d(Utils.TAG, "url: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();

            if (isCancelled()) {
                return null;
            }

            result.code = response.code();

            if (result.code != 200) {
                return result;
            }

            JSONObject json = new JSONObject(response.body().string());

            JSONArray texts = json.getJSONArray("text");

            result.text = "";

            for (int i = 0; i < texts.length(); i++) {
                result.text += texts.getString(i) + "\n";
            }

            result.from = json.getString("lang").substring(0, 2);

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
        Log.d(Utils.TAG, "TranslateTask.Result.code: " + result.code);
        if (callbacks != null) {
            callbacks.onTranslateTaskResult(result);
        }
    }

    public interface Callbacks {

        void onTranslateTaskPre();

        void onTranslateTaskResult(Result result);

    }

    public static class Result {
        public int code;
        public String text;
        public String from;
    }

}
