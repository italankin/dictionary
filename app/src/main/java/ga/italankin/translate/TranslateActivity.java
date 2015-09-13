package ga.italankin.translate;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class TranslateActivity extends AppCompatActivity implements TranslateTask.Callbacks {

    public static final String PREF_SOURCE = "source";
    public static final String PREF_SOURCE_NAME = "source_name";
    public static final String PERF_DEST = "dest";
    public static final String PREF_DEST_NAME = "dest_name";
    /**
     * Used to represent localized string
     */
    public static Language LANG_AUTO;
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
    public TextView mTextDest;
    /**
     * Shows current source language
     */
    public TextView mTextSource;
    /**
     * Source language display (for auto detection)
     */
    public TextView mTextAuto;
    /**
     * Translation async task
     */
    public TranslateTask mTask;
    /**
     * Currently selected source language
     */
    public Language mLangSource;
    /**
     * Source language of lastly translated text
     */
    public Language mLastLangSource;
    /**
     * Destination language
     */
    public Language mLangDest;
    /**
     * Translated text
     */
    public String mText = "";
    /**
     * Language names and keys
     */
    public ArrayList<Language> mLangList;
    /**
     * Represents current source language detection
     */
    public boolean mLangSourceAuto = false;
    // dialog for loading processes
    public ProgressDialog mDialog;
    /**
     * Shared preferences
     */
    private SharedPreferences mPrefs;
    /**
     * Copy to clipboard
     */
    private ClipboardManager mClipboard;
    // for touch events handling
    private float x;
    private float y;

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

        mLangList = Utils.readLanguagesFile(this);
        if (mLangList == null) {
            new LanguagesTask(new LanguagesTask.Callbacks() {
                @Override
                public void onLanguagesTaskResult(LanguagesTask.Result result) {
                    if (result.code == 200) {
                        mLangList = result.languages;
                    }
                }
            }).execute(Locale.getDefault().getLanguage());
        }

        LANG_AUTO = new Language("auto", getString(R.string.auto));

        mPrefs = getPreferences(MODE_PRIVATE);
        mClipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        mOutput = (TextView) findViewById(R.id.tvOutput);
        mTextDest = (TextView) findViewById(R.id.tvDirectionTo);
        mTextSource = (TextView) findViewById(R.id.tvDirectionFrom);
        mTextAuto = (TextView) findViewById(R.id.tvSource);
        mInput = (EditText) findViewById(R.id.etInput);

        if (mPrefs.contains(PREF_SOURCE)) {
            mLangSource = new Language(mPrefs.getString(PREF_SOURCE, Locale.getDefault().getLanguage()),
                    mPrefs.getString(PREF_SOURCE_NAME, Locale.getDefault().getDisplayLanguage()));
            mLastLangSource = mLangSource;
            mLangSourceAuto = mLangSource.compareTo(LANG_AUTO) == 0;
        } else {
            mLangSource = LANG_AUTO;
            mLangSourceAuto = true;
        }
        mLangDest = new Language(mPrefs.getString(PERF_DEST, Locale.getDefault().getLanguage()),
                mPrefs.getString(PREF_DEST_NAME, Locale.getDefault().getDisplayLanguage()));

        mInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    sendTranslateRequest(mInput.getText().toString());
                }
                return false;
            }
        });

        mTextDest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDestDialog();
            }
        });
        mTextSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSourceDialog();
            }
        });

        updateView(false);
    }

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
     * @param lang new source language
     */
    public void setSourceLang(Language lang) {
        mLangSource = lang;

        SharedPreferences.Editor editor = mPrefs.edit();
        if (mLangSourceAuto) {
            editor.putString(PREF_SOURCE, LANG_AUTO.getCode());
            editor.putString(PREF_SOURCE_NAME, LANG_AUTO.getName());
        } else {
            editor.putString(PREF_SOURCE, mLangSource.getCode());
            editor.putString(PREF_SOURCE_NAME, mLangSource.getName());
        }
        editor.apply();
    }

    /**
     * Set destination language (also saves it to {@link SharedPreferences})
     *
     * @param lang new source language
     */
    public void setDestLang(Language lang) {
        mLangDest = lang;

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PERF_DEST, mLangDest.getCode());
        editor.putString(PREF_DEST_NAME, mLangDest.getName());
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
        if (mLastLangSource == null) {
            return;
        }
        if (mLangSourceAuto) {
            if (replace) {
                setSourceLang(LANG_AUTO);
                setDestLang(mLastLangSource);
                mLastLangSource = null;
            } else {
                mLangSourceAuto = false;
                setSourceLang(mLangDest);
                setDestLang(mLastLangSource);
            }
        } else {
            Language from = mLangSource;
            setSourceLang(mLangDest);
            setDestLang(from);
        }
        updateView(true);
    }

    /**
     * Update language display fields
     *
     * @param notify notify user with toast if languages have been changed
     */
    public void updateView(boolean notify) {
        String from = LANG_AUTO.getName();
        if (!mLangSourceAuto) {
            from = mLangSource.getName();
        }
        String to = mLangDest.getName();

        mTextDest.setText(to);
        mTextSource.setText(from);
        mTextAuto.setText("");
        if (notify) {
            Toast.makeText(this, getString(R.string.toast_switch, from, to),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show source language dialog selection. If there are no currently list of available languages,
     * loads them from server
     */
    public void showSourceDialog() {
        if (mLangList == null) {
            mDialog = new ProgressDialog(this);
            mDialog.setMessage(getString(R.string.loading));
            mDialog.show();
            new LanguagesTask(new LanguagesTask.Callbacks() {
                @Override
                public void onLanguagesTaskResult(LanguagesTask.Result result) {
                    mLangList = result.languages;
                    showSourceDialog();
                    if (mDialog != null) {
                        mDialog.dismiss();
                    }
                }
            }).execute(Locale.getDefault().getLanguage());
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.source);

        Collections.sort(mLangList);
        builder.setAdapter(new LanguageAdapter(this, mLangList), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setSourceLang(mLangList.get(which));
                mLangSourceAuto = false;
                updateView(true);
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.detect, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setSourceLang(LANG_AUTO);
                mLangSourceAuto = true;
                updateView(true);
                dialog.dismiss();
                Collections.sort(mLangList);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    /**
     * Show destination language dialog selection. If there are no currently list of available
     * languages, loads them from server
     */
    public void showDestDialog() {
        if (mLangList == null) {
            mDialog = new ProgressDialog(this);
            mDialog.setMessage(getString(R.string.loading));
            mDialog.show();
            new LanguagesTask(new LanguagesTask.Callbacks() {
                @Override
                public void onLanguagesTaskResult(LanguagesTask.Result result) {
                    mLangList = result.languages;
                    showSourceDialog();
                    if (mDialog != null) {
                        mDialog.dismiss();
                    }
                }
            }).execute(Locale.getDefault().getLanguage());
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.destination);

        Collections.sort(mLangList);
        builder.setAdapter(new LanguageAdapter(this, mLangList), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setDestLang(mLangList.get(which));
                updateView(true);
                dialog.dismiss();
                Collections.sort(mLangList);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
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
        if (mLangSourceAuto) {
            mTask.execute(text, mLangDest.getCode());
        } else {
            mTask.execute(text, mLangSource.getCode(), mLangDest.getCode());
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
     * On translate task pre execute
     */
    public void onTranslateTaskPre() {
        mOutput.setText(getString(R.string.dots));
    }

    /**
     * Callback function to handle translation task results
     *
     * @param result result of translation request
     */
    public void onTranslateTaskResult(TranslateTask.Result result) {
        if (result.code == 200) {
            mOutput.setText(result.text);
            mText = result.text;
            mLastLangSource = Utils.findLanguageByCode(result.from, mLangList);
            updateView(false);
            if (mLangSourceAuto) {
                mTextAuto.setText("(" + mLastLangSource.getName() + ")");
            }
        } else {
            Toast.makeText(this, Utils.getErrorMessage(this, result.code), Toast.LENGTH_SHORT)
                    .show();
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
    public void onStop() {
        super.onStop();
        Utils.writeLanguagesFile(this, mLangList);
    }

    @Override
    public void onDestroy() {
        if (mTask != null) {
            mTask.cancel(false);
        }
        super.onDestroy();
    }
}
