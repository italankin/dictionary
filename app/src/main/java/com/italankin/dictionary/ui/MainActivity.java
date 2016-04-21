/*
 * Copyright 2016 Igor Talankin
 *
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
package com.italankin.dictionary.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
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
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.italankin.dictionary.App;
import com.italankin.dictionary.R;
import com.italankin.dictionary.dto.Result;
import com.italankin.dictionary.dto.TranslationEx;
import com.italankin.dictionary.ui.adapters.LanguageAdapter;
import com.italankin.dictionary.ui.adapters.TranslationAdapter;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnLongClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {

    private static final int SWITCH_ANIM_DURATION = 450;
    private static final int SWAP_LANGS_ANIM_DURATION = 300;
    private static final int TOOLBAR_ANIM_IN_DURATION = 700;
    private static final float INPUT_SCROLL_PARALLAX_FACTOR = 1.5f;

    private static final int REQUEST_CODE_SHARE = 17;

    @Inject
    MainPresenter _presenter;

    //region Views
    @Bind(R.id.toolbar)
    Toolbar toolbar;

    /**
     * EditText for text input
     */
    @Bind(R.id.edit_input)
    EditText mInput;

    @Bind(R.id.input_card)
    View mInputLayout;

    /**
     * TextView in toolbar indicating current destination language
     */
    @Bind(R.id.text_to)
    TextView mTextDest;

    /**
     * TextView in toolbar indicating current source language
     */
    @Bind(R.id.text_from)
    TextView mTextSource;

    /**
     * Arrow in the toolbar for switching languages
     */
    @Bind(R.id.swap_langs)
    View mSwapLangs;

    @Bind(R.id.lookup)
    View mLookup;

    @Bind(R.id.text_transcription)
    TextView mTranscription;

    @Bind(R.id.toolbar_inner_layout)
    View mToolbarInnerLayout;

    @Bind(R.id.recycler_view)
    RecyclerView mRecyclerView;
    //endregion

    /**
     * Adapter used to display source languages
     */
    private LanguageAdapter mLangsSourceAdapter;
    /**
     * Adapter used to display destination languages
     */
    private LanguageAdapter mLangsDestAdapter;

    /**
     * Used to implement debouce mechanism via publishing lookup requests to this object
     */
    private PublishSubject<String> mLookupEvents = PublishSubject.create();

    /**
     * Subscription for {@link #mLookupEvents}. Performs {@link #startLookup(String)} upon receiving
     * items.
     */
    private Subscription mLookupSub;

    private InputMethodManager mInputManager;
    private ClipboardManager mClipboardManager;

    private TranslationAdapter mRecyclerViewAdapter;

    ///////////////////////////////////////////////////////////////////////////
    // Activity callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.injector().inject(this);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mInputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mClipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        _presenter.attach(this);

        setSupportActionBar(toolbar);
        setupInputLayout();
        setupRecyclerView();
        setControlsState(false);

        _presenter.loadLanguages();
    }

    private void setupRecyclerView() {
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Resources res = getResources();
        int left = res.getDimensionPixelSize(R.dimen.list_item_margin_left);
        int top = res.getDimensionPixelSize(R.dimen.list_item_margin_top);
        int right = res.getDimensionPixelSize(R.dimen.list_item_margin_right);
        int bottom = res.getDimensionPixelSize(R.dimen.list_item_margin_bottom);
        mRecyclerView.addItemDecoration(new SimpleItemDecoration(left, top, right, bottom));

        // allow input layout to be scrolled along with recycler view
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateInputLayoutPosition();
            }
        });

        top = getResources().getDimensionPixelSize(R.dimen.list_top_offset);
        int inputHeight = getResources().getDimensionPixelSize(R.dimen.input_panel_height);
        mRecyclerView.setPadding(
                mRecyclerView.getPaddingLeft(),
                inputHeight + top,
                mRecyclerView.getPaddingRight(),
                mRecyclerView.getPaddingBottom()
        );

        // on click listener

        mRecyclerViewAdapter = new TranslationAdapter(this);
        mRecyclerViewAdapter.setListener(new TranslationAdapter.OnAdapterItemClickListener() {
            @Override
            public void onItemClick(int position) {
                TranslationEx item = _presenter.getLastResult().translations.get(position);
                startActivity(TranslationActivity.getStartIntent(getApplicationContext(), item));
            }

            @Override
            public void onItemMenuClick(int position, int menuItemId) {
                ClipData clip = null;
                TranslationEx item = _presenter.getLastResult().translations.get(position);
                switch (menuItemId) {
                    case R.id.action_lookup_word:
                        lookupNext(item.text);
                        break;

                    case R.id.action_copy_mean:
                        clip = ClipData.newPlainText("means", item.means);
                        break;

                    case R.id.action_copy_synonyms:
                        clip = ClipData.newPlainText("synonyms", item.synonyms);
                        break;

                    case R.id.action_copy_translation:
                        clip = ClipData.newPlainText("translation", item.text);
                        break;
                }
                if (clip != null) {
                    mClipboardManager.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, R.string.msg_copied, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mRecyclerView.setAdapter(mRecyclerViewAdapter);
    }

    private void setupInputLayout() {
        mInputLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInput.requestFocus();
                mInputManager.showSoftInput(mInput, 0);
            }
        });
        ViewTreeObserver vto = mInputLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= 16) {
                    ViewTreeObserver vto = mInputLayout.getViewTreeObserver();
                    vto.removeOnGlobalLayoutListener(this);
                }
                updateInputLayoutPosition();
            }
        });

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ensure we reattach presenter in case there're multiple activities being launched
        _presenter.attach(this);
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
            _presenter.saveLanguages();
            _presenter.detach(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private boolean handleIntent(Intent intent) {
        if (intent.getType() != null) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SHARE && resultCode == RESULT_OK && _presenter.closeOnShare()) {
            finish();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // ButterKnife
    ///////////////////////////////////////////////////////////////////////////

    @OnClick(R.id.lookup)
    void onLookupClick() {
        lookupNext(mInput.getText().toString());
    }

    @OnFocusChange(R.id.edit_input)
    void onEditInputFocusChange(boolean hasFocus) {
        if (hasFocus) {
            mRecyclerView.smoothScrollToPosition(0);
        }
    }

    @OnLongClick(R.id.text_transcription)
    boolean onTranscriptionLongClick(View v) {
        ClipData clip = ClipData.newPlainText("transcription", mTranscription.getText().toString());
        mClipboardManager.setPrimaryClip(clip);
        Toast.makeText(MainActivity.this, R.string.msg_transcription_copied, Toast.LENGTH_SHORT).show();
        return true;
    }

    @OnClick(R.id.text_to)
    void onDestLangClick() {
        showDestLanguageDialog();
    }

    @OnClick(R.id.text_from)
    void onSourceLangClick() {
        showSourceLanguageDialog();
    }

    @OnClick(R.id.swap_langs)
    void onSwapLangsClick() {
        swapLanguages();
    }

    @OnLongClick(R.id.swap_langs)
    boolean onSwapLangsLongClick() {
        Toast.makeText(MainActivity.this, R.string.toast_swap_langs, Toast.LENGTH_SHORT).show();
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Options menu
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                Result result = _presenter.getLastResult();
                if (result != null && !result.isEmpty()) {
                    Intent intent = ShareCompat.IntentBuilder
                            .from(this)
                            .setType("text/plain")
                            .setText(result.toString())
                            .setSubject(result.text)
                            .createChooserIntent();
                    startActivityForResult(intent, REQUEST_CODE_SHARE);
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
        String source = _presenter.getSourceLanguage().getName();
        mTextSource.setText(source);
        String dest = _presenter.getDestLanguage().getName();
        mTextDest.setText(dest);
    }

    /**
     * Sync scroll state of the top layout.
     */
    private void updateInputLayoutPosition() {
        float max = mInputLayout.getHeight() * INPUT_SCROLL_PARALLAX_FACTOR +
                mInputLayout.getHeight() / 10f; // additional space
        float value = max;
        float abs = Math.abs(mRecyclerView.computeVerticalScrollOffset() / INPUT_SCROLL_PARALLAX_FACTOR);
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
            mLangsSourceAdapter = new LanguageAdapter(this, _presenter.getLanguagesList());
        }
        _presenter.sortLanguagesList();
        mLangsSourceAdapter.notifyDataSetChanged();
        builder.setAdapter(mLangsSourceAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                _presenter.setSourceLanguage(which);
                updateTextViews();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    /**
     * Show dialog for choosing destination language.
     */
    public void showDestLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.destination);
        if (mLangsDestAdapter == null) {
            mLangsDestAdapter = new LanguageAdapter(this, _presenter.getLanguagesList());
        }
        _presenter.sortLanguagesList();
        mLangsDestAdapter.notifyDataSetChanged();
        builder.setAdapter(mLangsDestAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                _presenter.setDestLanguage(which);
                updateTextViews();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void showHistoryDialog() {
        final String[] values = _presenter.getHistory();
        if (values == null || values.length == 0) {
            Toast.makeText(this, R.string.msg_history_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_history);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                values);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                _presenter.loadHistory(which);
            }
        });
        builder.show();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Lookup
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Emit a new lookup request.
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
        text = text.replaceAll("[^\\p{L}\\w -]", " ").trim();
        mInput.setText(text);
        if (text.length() > 0) {
            _presenter.lookup(text);
            mInputManager.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
            mInput.clearFocus();
        }
    }

    /**
     * Called from {@link MainPresenter}, when languages were fetched from cache/net.
     */
    public void onLanguagesResult() {
        mToolbarInnerLayout.setVisibility(View.VISIBLE);
        mToolbarInnerLayout.setTranslationY(-mToolbarInnerLayout.getHeight());
        mToolbarInnerLayout.setAlpha(0);
        mToolbarInnerLayout.animate()
                .alpha(1)
                .translationY(0)
                .setDuration(TOOLBAR_ANIM_IN_DURATION)
                .setInterpolator(new DecelerateInterpolator(2.5f))
                .start();

        setControlsState(true);
        updateTextViews();

        // if we are not coming from share intent
        if (!handleIntent(getIntent())) {
            Result lastResult = _presenter.getLastResult();
            if (lastResult == null) {
                mInput.requestFocus();
            } else {
                onLookupResult(lastResult);
            }
        }
    }

    private void swapLanguages() {
        if (!_presenter.swapLanguages()) {
            return;
        }
        lookupNext(mInput.getText().toString());

        // anim stuff

        float rotation = 180f;
        if (mSwapLangs.getRotation() > 0) {
            rotation *= -1;
        }
        mSwapLangs.setRotation(0);
        mSwapLangs.animate()
                .rotationBy(rotation)
                .setDuration(SWAP_LANGS_ANIM_DURATION)
                .start();

        SwitchAnimation anim = new SwitchAnimation(mTextSource,
                mTextSource.getHeight(),
                0f,
                SWITCH_ANIM_DURATION,
                new SwitchAnimation.OnSwitchListener() {
                    @Override
                    public void onSwitch() {
                        updateTextViews();
                    }
                });
        anim.start();
        anim = new SwitchAnimation(mTextDest, -mTextDest.getHeight(), 0, SWITCH_ANIM_DURATION, null);
        anim.start();
    }

    /**
     * Called from {@link MainPresenter} when received lookup result from server
     *
     * @param result result object returned from server
     */
    public void onLookupResult(Result result) {
        List<TranslationEx> translations = result.translations;
        mInput.setText(result.text);
        if (!TextUtils.isEmpty(result.transcription)) {
            mTranscription.setText(String.format("[%s]", result.transcription));
        } else {
            mTranscription.setText("");
        }
        mRecyclerViewAdapter.setData(translations);

        updateInputLayoutPosition();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Errors
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Called when error occured. Typically it's related to server or connection problems/wrong
     * request.
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
     * Show error when no results were received.
     */
    public void onNoResults() {
        onError(getString(R.string.error_no_results));
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
                _presenter.loadLanguages();
            }
        });
        snackbar.show();
    }

}
