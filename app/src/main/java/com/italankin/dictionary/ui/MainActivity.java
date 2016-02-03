package com.italankin.dictionary.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.italankin.dictionary.R;
import com.italankin.dictionary.adapters.LanguageAdapter;
import com.italankin.dictionary.adapters.TranslationAdapter;
import com.italankin.dictionary.dto.Result;
import com.italankin.dictionary.dto.TranslationEx;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {

    private MainPresenter mPresenter;

    private Toolbar mToolbar;
    private EditText mInput;
    private View mInputLayout;
    private TextView mTextDest;
    private TextView mTextSource;
    private ImageView mArrow;
    private View mLookup;
    private TextView mTranscription;
    private View mToolbarInnerLayout;

    private RecyclerView mRecyclerView;
    private TranslationAdapter mRecyclerViewAdapter;
    private TranslationAdapter.OnAdapterItemClickListener mRecyclerViewListener;
    private List<TranslationEx> mTranslations;

    private LanguageAdapter mLangsSourceAdapter;
    private LanguageAdapter mLangsDestAdapter;

    private PublishSubject<String> mLookupEvents = PublishSubject.create();
    private Subscription mLookupSub;

    private InputMethodManager mInputManager;
    private ClipboardManager mClipboardManager;

    private int mRecyclerViewScroll = 0;

    ///////////////////////////////////////////////////////////////////////////
    // Activity callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mClipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        mPresenter = MainPresenter.getInstance(this);
        mPresenter.attach(this);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mToolbarInnerLayout = findViewById(R.id.toolbar_inner_layout);
        mTextDest = (TextView) findViewById(R.id.tvDirectionTo);
        mTextSource = (TextView) findViewById(R.id.tvDirectionFrom);

        mInputLayout = findViewById(R.id.input_card);
        mInputLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInput.requestFocus();
                mInputManager.showSoftInput(mInput, 0);
            }
        });

        mInput = (EditText) findViewById(R.id.etInput);
        mInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    lookupNext(mInput.getText().toString());
                    return true;
                }
                return false;
            }
        });
        mInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mRecyclerView.smoothScrollToPosition(0);
                }
            }
        });
        mTranscription = (TextView) findViewById(R.id.tvTranscription);
        mTranscription.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipData clip = ClipData.newPlainText("transcription",
                        mTranscription.getText().toString());
                mClipboardManager.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, R.string.msg_transcription_copied, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mLookup = findViewById(R.id.tvLoad);
        mLookup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lookupNext(mInput.getText().toString());
            }
        });

        mTextDest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDestLanguageDialog();
            }
        });
        mTextSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSourceLanguageDialog();
            }
        });

        mArrow = (ImageView) findViewById(R.id.ivArrow);
        mArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swapLanguages();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mRecyclerViewScroll += dy;
                updateTopView();
            }
        });
        mRecyclerViewListener = new TranslationAdapter.OnAdapterItemClickListener() {
            @Override
            public void onItemClick(int position) {
                TranslationEx item = mTranslations.get(position);
                startActivity(TranslationActivity.getStartIntent(getApplicationContext(), item));
            }

            @Override
            public void onItemMenuClick(int position, int menuItemId) {
                ClipData clip = null;
                switch (menuItemId) {
                    case R.id.action_lookup_word:
                        TranslationEx item = mTranslations.get(position);
                        lookupNext(item.text);
                        break;
                    case R.id.action_copy_mean:
                        clip = ClipData.newPlainText("means", mTranslations.get(position).means);
                        break;
                    case R.id.action_copy_synonyms:
                        clip = ClipData.newPlainText("synonyms", mTranslations.get(position).synonyms);
                        break;
                    case R.id.action_copy_translation:
                        clip = ClipData.newPlainText("translation", mTranslations.get(position).text);
                        break;
                    case R.id.action_copy_examples:
                        clip = ClipData.newPlainText("examples", mTranslations.get(position).examples);
                        break;
                }
                if (clip != null) {
                    mClipboardManager.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, R.string.msg_copied, Toast.LENGTH_SHORT).show();
                }
            }
        };

        setControlsState(false);

        mPresenter.loadLanguages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLookupSub = mLookupEvents
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return !TextUtils.isEmpty(s);
                    }
                })
                .debounce(600, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        startLookup(s);
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLookupSub != null && !mLookupSub.isUnsubscribed()) {
            mLookupSub.unsubscribe();
            mLookupSub = null;
        }
        if (isFinishing()) {
            mPresenter.saveLanguages();
            mPresenter.detach();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private boolean handleIntent(Intent intent) {
        if (intent != null && intent.getType() != null) {
            String type = intent.getType();
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            text = text.substring(0, Math.min(text.length(), 80));
            if (type != null && !TextUtils.isEmpty(text) && "text/plain".equals(type)) {
                intent.setType(null);
                mInput.setText(text);
                mInput.selectAll();
                startLookup(text);
            }
            return true;
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Options menu
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                Result result = mPresenter.getLastResult();
                if (result != null && !result.isEmpty()) {
                    Intent intent = ShareCompat.IntentBuilder
                            .from(this)
                            .setType("text/plain")
                            .setText(result.toString())
                            .setSubject(result.text)
                            .getIntent();
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.error_share, Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.action_history:
                showHistoryDialog();
                return true;

            case R.id.action_settings:
                Intent intent = new Intent("com.italankin.dictionary.SETTINGS");
                startActivity(intent);
                return true;
        }

        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Views update
    ///////////////////////////////////////////////////////////////////////////

    private void updateTextViews() {
        String source = mPresenter.getSourceLanguage().getName();
        mTextSource.setText(source);
        String dest = mPresenter.getDestLanguage().getName();
        mTextDest.setText(dest);
    }

    /**
     * Sync scroll state of the top layout.
     */
    private void updateTopView() {
        float factor = 3;
        float max = mInputLayout.getHeight() * factor + mInputLayout.getHeight() / 10f;
        float value = max;
        float abs = Math.abs(mRecyclerViewScroll / factor);
        if (abs < max) {
            value = abs;
        }
        mInputLayout.setTranslationY(-value);
    }

    private void setControlsState(boolean enabled) {
        mInput.setEnabled(enabled);
        mLookup.setEnabled(enabled);
        mRecyclerView.setEnabled(enabled);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Dialogs
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Show dialog for choosing source language.
     */
    public void showSourceLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.source);
        if (mLangsSourceAdapter == null) {
            mLangsSourceAdapter = new LanguageAdapter(this, mPresenter.getLanguagesList());
        }
        mPresenter.sortLanguagesList();
        mLangsSourceAdapter.notifyDataSetChanged();
        builder.setAdapter(mLangsSourceAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPresenter.setSourceLanguage(which);
                updateTextViews();
            }
        });
        builder.show();
    }

    /**
     * Show dialog for choosing destination language.
     */
    public void showDestLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.destination);
        if (mLangsDestAdapter == null) {
            mLangsDestAdapter = new LanguageAdapter(this, mPresenter.getLanguagesList());
        }
        mPresenter.sortLanguagesList();
        mLangsDestAdapter.notifyDataSetChanged();
        builder.setAdapter(mLangsDestAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPresenter.setDestLanguage(which);
                updateTextViews();
            }
        });
        builder.show();
    }

    private void showHistoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String[] values = mPresenter.getLastQueries();
        if (values == null || values.length == 0) {
            Toast.makeText(this, R.string.msg_history_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                values);
        builder.setNeutralButton(R.string.clear_history, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPresenter.clearLastQueries();
            }
        });
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                lookupNext(values[which]);
            }
        });
        builder.show();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Main
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Emit a new lookup request object.
     *
     * @param s text to lookup
     */
    private void lookupNext(String s) {
        mLookupEvents.onNext(s);
    }

    /**
     * Start lookup process, sending actual query.
     *
     * @param text text to lookup
     */
    private void startLookup(String text) {
        mPresenter.lookup(text);
        mInputManager.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
        mInput.clearFocus();
    }

    /**
     * Called from {@link MainPresenter}, when languages were fetched from cache/net.
     */
    public void onLanguagesResult() {
        // if we got share intent from other application
        if (!handleIntent(getIntent())) {
            mPresenter.getLastResultAsync();
        }
        mToolbarInnerLayout.setVisibility(View.VISIBLE);
        mToolbarInnerLayout.setTranslationY(-mToolbarInnerLayout.getHeight());
        mToolbarInnerLayout.setAlpha(0);
        mToolbarInnerLayout.animate()
                .alpha(1)
                .translationY(0)
                .setDuration(700)
                .setInterpolator(new DecelerateInterpolator(2.5f))
                .start();

        mLookup.setVisibility(View.VISIBLE);
        mLookup.setScaleX(0);
        mLookup.setScaleY(0);
        mLookup.animate()
                .scaleX(1)
                .scaleY(1)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator())
                .start();

        setControlsState(true);
        mInput.requestFocus();
        mInputManager.showSoftInput(mInput, 0);
        updateTextViews();
    }

    private void swapLanguages() {
        if (!mPresenter.swapLanguages()) {
            return;
        }
        lookupNext(mInput.getText().toString());

        int duration = 450;

        float rotation = 180f;
        if (mArrow.getRotation() > 0) {
            rotation *= -1;
        }
        mArrow.setRotation(0);
        mArrow.animate()
                .rotationBy(rotation)
                .setDuration(300)
                .start();

        SwitchAnimation anim = new SwitchAnimation(mTextSource,
                mTextSource.getHeight(),
                0f,
                duration,
                new SwitchAnimation.OnSwitchListener() {
                    @Override
                    public void onSwitch() {
                        updateTextViews();
                    }
                });
        anim.start();
        anim = new SwitchAnimation(mTextDest, -mTextDest.getHeight(), 0, duration, null);
        anim.start();
    }

    /**
     * Called from {@link MainPresenter} when received lookup result from server
     *
     * @param result result object returned from server
     */
    public void onLookupResult(@Nullable Result result) {
        if (result == null || result.isEmpty()) {
            onError(getString(R.string.error_no_results));
            return;
        }
        mTranslations = result.translations;
        mInput.setText(result.text);
        if (!TextUtils.isEmpty(result.transcription)) {
            mTranscription.setText(String.format("[%s]", result.transcription));
        } else {
            mTranscription.setText("");
        }
        if (mRecyclerViewAdapter == null) {
            mRecyclerViewAdapter = new TranslationAdapter(mToolbar.getHeight() + mInputLayout.getHeight(),
                    mRecyclerViewListener);
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
        }
        mRecyclerViewAdapter.setItems(mTranslations);
        mRecyclerViewScroll = 0;
        updateTopView();
        mRecyclerView.scrollToPosition(0);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Errors
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Error message.
     *
     * @param messaage text to show
     */
    public void onError(String messaage) {
        if (messaage == null) {
            messaage = getString(R.string.error);
        }
        Snackbar snackbar = Snackbar.make(mInput, messaage, Snackbar.LENGTH_LONG);
        snackbar.setAction(android.R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // empty callback for showing OK button
            }
        });
        snackbar.show();
    }

    /**
     * If error was occured while fetching languages.
     */
    public void onLanguagesError() {
        setControlsState(false);
        Snackbar snackbar = Snackbar.make(mInput, R.string.error_langs, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.loadLanguages();
            }
        });
        snackbar.show();
    }

}
