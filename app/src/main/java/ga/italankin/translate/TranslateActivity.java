package ga.italankin.translate;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class TranslateActivity extends AppCompatActivity {

    public static final String LOG_TAG = "HERO1M";

    public static final String API_KEY = "trnsl.1.1.20150909T160500Z.1739f3dcac749dac.c93fc2917663d5e294ecf7102dfb198c08d91a1c";

    public static final String KEY_LANG_FROM = "from";
    public static final String KEY_LANG_TO = "to";

    private SharedPreferences mPrefs;

    private ClipboardManager mClipboard;

    private EditText mInput;
    private TextView mOutput;
    private TextView mDirection;

    private TranslateTask mTask;
    private String mLangFrom = "en";
    private String mLangTo = "ru";
    private String mText = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        mPrefs = getPreferences(MODE_PRIVATE);
        mClipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        mOutput = (TextView) findViewById(R.id.tvOutput);
        mDirection = (TextView) findViewById(R.id.tvDirection);
        mInput = (EditText) findViewById(R.id.etInput);

        mInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    sendTranslateRequest(mInput.getText().toString());
                    return true;
                }
                return false;
            }
        });

        mDirection.setText(getString(R.string.input_hint, mLangFrom, mLangTo).toUpperCase());
    }

    public void setLangsPrefs(String from, String to) {
        mLangFrom = from;
        mLangTo = to;
        mDirection.setText(getString(R.string.input_hint, mLangFrom, mLangTo).toUpperCase());
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(KEY_LANG_FROM, mLangFrom);
        editor.putString(KEY_LANG_TO, mLangTo);
        editor.apply();
    }

    public void sendTranslateRequest(String text) {
        text = text.trim().replaceAll("\\s+", "+").replaceAll("\\?&", "");
        if (text.length() == 0) {
            mInput.setText("");
            Toast.makeText(this, getString(R.string.error_empty_query), Toast.LENGTH_SHORT).show();
            return;
        }
        if (mTask != null) {
            mTask.cancel(false);
            mTask = null;
        }
        mTask = new TranslateTask(this);
        mTask.execute(mLangFrom, mLangTo, text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_translate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_copy:
                if (TextUtils.isEmpty(mText)) {
                    Toast.makeText(this,
                            getString(R.string.error_copy_nothing),
                            Toast.LENGTH_SHORT).show();
                } else {
                    ClipData clip = ClipData.newPlainText(mText, mText);
                    mClipboard.setPrimaryClip(clip);
                    Toast.makeText(this,
                            getString(R.string.toast_copy),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_switch:
                setLangsPrefs(mLangTo, mLangFrom);
                Toast.makeText(this,
                        getString(R.string.toast_switch,
                                mLangFrom.toUpperCase(),
                                mLangTo.toUpperCase()),
                        Toast.LENGTH_SHORT).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if (mTask != null) {
            mTask.cancel(false);
            mTask.unlink();
        }
        super.onDestroy();
    }

    public static class TranslateTask extends AsyncTask<String, Void, TranslateResult> {

        private TranslateActivity mActivity;
        private OkHttpClient client;

        public TranslateTask(TranslateActivity activity) {
            mActivity = activity;
        }

        public void unlink() {
            mActivity = null;
        }

        @Override
        protected void onPreExecute() {
            client = new OkHttpClient();
            if (mActivity != null) {
                mActivity.mOutput.setText(mActivity.getString(R.string.dots));
            }
        }

        @Override
        protected TranslateResult doInBackground(String... params) {
            TranslateResult result = new TranslateResult();
            try {
                TranslatorHelper helper = new TranslatorHelper(params[0], params[1]);
                String url = helper.translateUrl(params[2], API_KEY);

                Log.v(LOG_TAG, "url: " + url);

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

                return result;
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
                result.code = 0;
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.toString());
                result.code = 1;
            }

            return result;
        }

        @Override
        protected void onPostExecute(TranslateResult result) {
            Log.v(LOG_TAG, "code: " + result.code);
            if (mActivity == null) {
                return;
            }
            switch (result.code) {
                case 0:
                    Toast.makeText(mActivity.getApplicationContext(),
                            mActivity.getString(R.string.error_no_connection),
                            Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(mActivity.getApplicationContext(),
                            mActivity.getString(R.string.error_nonsense),
                            Toast.LENGTH_LONG).show();
                    break;
                case 401:
                    Toast.makeText(mActivity.getApplicationContext(),
                            mActivity.getString(R.string.error_invalid_key),
                            Toast.LENGTH_LONG).show();
                    break;
                case 402:
                    Toast.makeText(mActivity.getApplicationContext(),
                            mActivity.getString(R.string.error_blocked_key),
                            Toast.LENGTH_LONG).show();
                    break;
                case 403:
                    Toast.makeText(mActivity.getApplicationContext(),
                            mActivity.getString(R.string.error_daily_limit),
                            Toast.LENGTH_LONG).show();
                    break;
                case 404:
                    Toast.makeText(mActivity.getApplicationContext(),
                            mActivity.getString(R.string.error_volume_limit),
                            Toast.LENGTH_LONG).show();
                    break;
                case 405:
                    Toast.makeText(mActivity.getApplicationContext(),
                            mActivity.getString(R.string.error_char_limit),
                            Toast.LENGTH_LONG).show();
                    break;
                case 422:
                    Toast.makeText(mActivity.getApplicationContext(),
                            mActivity.getString(R.string.error_unprocessable),
                            Toast.LENGTH_LONG).show();
                    break;
                case 501:
                    Toast.makeText(mActivity.getApplicationContext(),
                            mActivity.getString(R.string.error_invalid_lang),
                            Toast.LENGTH_LONG).show();
                    break;
                default:
                    mActivity.mOutput.setText(result.text);
                    mActivity.mText = result.text;
            }
        }

    } // TranslateTask

    private static class TranslateResult {
        public int code;
        public String text;
    }

}
