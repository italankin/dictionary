package ga.italankin.translate;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class LanguagesTask extends AsyncTask<String, Void, LanguagesTask.Result> {

    private OkHttpClient client;
    private TranslateActivity activity;
    private ProgressDialog dialog;
    private int i;

    public LanguagesTask(TranslateActivity activity, int i) {
        this.activity = activity;
        this.i = i;
    }

    @Override
    protected void onPreExecute() {
        client = new OkHttpClient();
        if (activity != null && i >= 0) {
            dialog = new ProgressDialog(activity);
            dialog.setMessage(activity.getString(R.string.loading));
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    LanguagesTask.this.cancel(false);
                }
            });
            dialog.show();
        }
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

            result.keys = new String[json.length()];

            Iterator<String> iter = json.keys();
            int i = 0;
            String key;
            while (iter.hasNext()) {
                key = iter.next();
                result.keys[i++] = json.getString(key) + " (" + key.toUpperCase() + ")";
            }

            Arrays.sort(result.keys);

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
        if (dialog != null) {
            dialog.dismiss();
        }
        if (activity != null) {
            activity.onLanguagesTaskResult(result, i);
        }
    }


    public static class Result {
        public int code;
        public String[] keys;
    }

    public interface Callbacks {

        void onLanguagesTaskResult(Result result, int i);

    }
}