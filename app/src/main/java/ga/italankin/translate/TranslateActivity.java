package ga.italankin.translate;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class TranslateActivity extends AppCompatActivity implements LanguagesTask.Callbacks, TranslateTask.Callbacks {

    public static final String PREF_LANG_FROM = "from";
    public static final String PREF_LANG_TO = "to";
    /**
     * Used to represent localized string
     */
    public static String AUTO;

    /**
     * Shared preferences
     */
    private SharedPreferences mPrefs;
    /**
     * Copy to clipboard
     */
    private ClipboardManager mClipboard;

    /**
     * Input text field
     */
    public EditText mInput;
    /**
     * Output text field (translation)
     */
    public TextView mOutput;
    /**
     * Shows current destination language
     */
    public TextView mDirectionTo;
    /**
     * Shows current source language
     */
    public TextView mDirectionFrom;
    /**
     * Source language display (for auto detection)
     */
    public TextView mSourceLanguage;

    /**
     * Translation async task
     */
    public TranslateTask mTask;

    /**
     * Currently selected source language
     */
    public String mLangFrom;
    /**
     * Source language of lastly translated text
     */
    public String mLangFromLast;
    /**
     * Destination language
     */
    public String mLangTo;
    /**
     * Translated text
     */
    public String mText = "";
    /**
     * Language names and keys
     */
    public String[] mLangKeys;
    /**
     * Represents current source language detection
     */
    public boolean mAuto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setLogo(R.mipmap.ic_launcher);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        new LanguagesTask(this, -1).execute(Locale.getDefault().getLanguage());

        AUTO = getString(R.string.text_auto);

        mPrefs = getPreferences(MODE_PRIVATE);
        mClipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        mOutput = (TextView) findViewById(R.id.tvOutput);
        mDirectionTo = (TextView) findViewById(R.id.tvDirectionTo);
        mDirectionFrom = (TextView) findViewById(R.id.tvDirectionFrom);
        mSourceLanguage = (TextView) findViewById(R.id.tvSource);
        mInput = (EditText) findViewById(R.id.etInput);

        mLangFrom = mPrefs.getString(PREF_LANG_FROM, AUTO);
        mAuto = TextUtils.equals(mLangFrom, AUTO);
        mLangTo = mPrefs.getString(PREF_LANG_TO, Locale.getDefault().getDisplayLanguage());

        mInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    sendTranslateRequest(mInput.getText().toString());
                }
                return false;
            }
        });

        mDirectionTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChooseDestDialog();
            }
        });
        mDirectionFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChooseSourceDialog();
            }
        });

        updateView(false);
    }

    // for touch events handling
    private float x;
    private float y;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = MotionEventCompat.getActionMasked(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
                x = event.getX() - x;
                y = event.getY() - y;
                // gesture length >= 50 px
                if (Math.abs(x) < 50 && Math.abs(y) < 50) {
                    return true;
                }
                // if true, direction is horizontal
                if (Math.abs(x) > Math.abs(y)) {
                    // move left/right - swap languages
                    swapLangs(x > 0);
                } else {
                    if (y < 0) {
                        // move up - translate text
                        sendTranslateRequest(mInput.getText().toString());
                    } else {
                        // move down - copy translation to input box
                        CharSequence text = mOutput.getText();
                        if (text.length() > 0) {
                            mInput.setText(text);
                            mInput.setSelection(text.length());
                        }
                    }
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Set source language (also saves it to {@link SharedPreferences})
     *
     * @param from new source language
     */
    public void setLangFrom(String from) {
        mLangFrom = from;

        SharedPreferences.Editor editor = mPrefs.edit();
        if (mAuto) {
            editor.putString(PREF_LANG_FROM, AUTO);
        } else {
            editor.putString(PREF_LANG_FROM, mLangFrom);
        }
        editor.apply();
    }

    /**
     * Set destination language (also saves it to {@link SharedPreferences})
     *
     * @param to new source language
     */
    public void setLangTo(String to) {
        mLangTo = to;

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_LANG_TO, mLangTo);
        editor.apply();
    }

    /**
     * Swaps source and destination languages
     *
     * @param replace if auto detection is enabled, swipe from right to left will replace
     *                destination text, but source text will be detected automatically. If value is
     *                <b>false</b>, source text will set to destination language
     */
    public void swapLangs(boolean replace) {
        if (TextUtils.isEmpty(mLangFromLast)) {
            return;
        }
        if (mAuto) {
            if (replace) {
                setLangFrom(AUTO);
                setLangTo(mLangFromLast);
                mLangFromLast = null;
            } else {
                mAuto = false;
                setLangFrom(mLangTo);
                setLangTo(mLangFromLast);
            }
        } else {
            String from = mLangFrom;
            setLangFrom(mLangTo);
            setLangTo(from);
        }
        updateView(true);
    }

    /**
     * Update language display fields
     *
     * @param notify notify user with toast if languages have been changed
     */
    public void updateView(boolean notify) {
        String from = AUTO;
        if (!mAuto) {
            from = Utils.extractLangName(mLangFrom);
        }
        String to = Utils.extractLangName(mLangTo);

        mDirectionTo.setText(to);
        mDirectionFrom.setText(from);
        mSourceLanguage.setText("");
        if (notify) {
            Toast.makeText(this, getString(R.string.toast_switch, from, to),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show source language dialog selection. If there are no currently list of available languages,
     * loads them from server
     */
    public void showChooseSourceDialog() {
        if (mLangKeys == null) {
            new LanguagesTask(this, 0).execute(Locale.getDefault().getLanguage());
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_language_from);
        builder.setItems(mLangKeys, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setLangFrom(mLangKeys[which]);
                mAuto = false;
                updateView(true);
                dialog.dismiss();
            }
        });
        builder.setNeutralButton(R.string.lang_detect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setLangFrom(AUTO);
                mAuto = true;
                updateView(true);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /**
     * Show destination language dialog selection. If there are no currently list of available
     * languages, loads them from server
     */
    public void showChooseDestDialog() {
        if (mLangKeys == null) {
            new LanguagesTask(this, 0).execute(Locale.getDefault().getLanguage());
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_language_to);
        builder.setItems(mLangKeys, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setLangTo(mLangKeys[which]);
                updateView(true);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /**
     * Sends translate request to the server
     *
     * @param text text to translate
     */
    public void sendTranslateRequest(String text) {
        text = text.trim()
                .replaceAll("\\s+", "+") // replace whitespaces
                .replaceAll("[\\?&%\\\\/\\^\\[\\]\\*\\{\\}]+", ""); // replace some special characters
        if (text.length() == 0) {
            mInput.setText("");
            Toast.makeText(this, getString(R.string.error_empty_query), Toast.LENGTH_SHORT).show();
            return;
        }
        // cancel current task if needed
        if (mTask != null) {
            mTask.cancel(false);
            mTask = null;
        }
        mTask = new TranslateTask(this);
        if (mAuto) {
            mTask.execute(text, Utils.extractLangCode(mLangTo));
        } else {
            mTask.execute(text, Utils.extractLangCode(mLangFrom), Utils.extractLangCode(mLangTo));
        }
    }

    /**
     * Copies text to clipboard
     */
    public void copyText() {
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
    }

    /**
     * Callback fuction to handle language load tasks completion
     *
     * @param result result of language task
     * @param i      identifier of task caller
     */
    public void onLanguagesTaskResult(LanguagesTask.Result result, int i) {
        switch (result.code) {
            // no connection
            case 0:
                Toast.makeText(this,
                        getString(R.string.error_no_connection),
                        Toast.LENGTH_SHORT).show();
                break;
            // cannot parse data received from the server
            case 1:
                Toast.makeText(this,
                        getString(R.string.error_nonsense),
                        Toast.LENGTH_SHORT).show();
                break;
            // something strange happened, generally request sending failed
            case 2:
                Toast.makeText(this,
                        getString(R.string.error_something_wrong),
                        Toast.LENGTH_SHORT);
                break;
            // invalid API key
            case 401:
                Toast.makeText(this,
                        getString(R.string.error_invalid_key),
                        Toast.LENGTH_SHORT).show();
                break;
            // API key is blocked
            case 402:
                Toast.makeText(this,
                        getString(R.string.error_blocked_key),
                        Toast.LENGTH_SHORT).show();
                break;
            // ok behaviour
            default:
                mLangKeys = result.keys;
                switch (i) {
                    case 0:
                        showChooseSourceDialog();
                        break;
                    case 1:
                        showChooseDestDialog();
                        break;
                }
        }
    }

    /**
     * On translate task pre execute
     */
    public void onTranslateTaskPre() {
        mOutput.setText(getString(R.string.dots));
    }

    /**
     * Callback function to handle translation task results
     * @param result result of translation request
     */
    public void onTranslateTaskResult(TranslateTask.Result result) {
        switch (result.code) {
            // no connection
            case 0:
                Toast.makeText(this,
                        getString(R.string.error_no_connection),
                        Toast.LENGTH_SHORT).show();
                break;
            // data received from server failed to parse
            case 1:
                Toast.makeText(this,
                        getString(R.string.error_nonsense),
                        Toast.LENGTH_SHORT).show();
                break;
            // something strange happened, generally request sending failed
            case 2:
                Toast.makeText(this,
                        getString(R.string.error_something_wrong),
                        Toast.LENGTH_SHORT);
                break;
            // translate direction is not supported by server
            case 400:
                Toast.makeText(this,
                        getString(R.string.error_not_supported),
                        Toast.LENGTH_SHORT).show();
                break;
            // invalid API key
            case 401:
                Toast.makeText(this,
                        getString(R.string.error_invalid_key),
                        Toast.LENGTH_SHORT).show();
                break;
            // API key is blocked
            case 402:
                Toast.makeText(this,
                        getString(R.string.error_blocked_key),
                        Toast.LENGTH_SHORT).show();
                break;
            // daily query limit reached
            case 403:
                Toast.makeText(this,
                        getString(R.string.error_daily_limit),
                        Toast.LENGTH_SHORT).show();
                break;
            // daily translation volume reached
            case 404:
                Toast.makeText(this,
                        getString(R.string.error_volume_limit),
                        Toast.LENGTH_SHORT).show();
                break;
            // query length is too big
            case 405:
                Toast.makeText(this,
                        getString(R.string.error_char_limit),
                        Toast.LENGTH_SHORT).show();
                break;
            // server failed to translate text
            case 422:
                Toast.makeText(this,
                        getString(R.string.error_unprocessable),
                        Toast.LENGTH_SHORT).show();
                break;
            // language is not supported by server
            case 501:
                Toast.makeText(this,
                        getString(R.string.error_invalid_lang),
                        Toast.LENGTH_SHORT).show();
                break;
            // ok behaviour
            default:
                mOutput.setText(result.text);
                mText = result.text;
                mLangFromLast = Utils.findLangNameByCode(result.from, mLangKeys);
                updateView(false);
                if (mAuto) {
                    mSourceLanguage.setText("(" + Utils.extractLangName(mLangFromLast) + ")");
                }
        }
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
                copyText();
                break;
            case R.id.action_switch:
                swapLangs(false);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if (mTask != null) {
            mTask.cancel(false);
        }
        super.onDestroy();
    }

}
