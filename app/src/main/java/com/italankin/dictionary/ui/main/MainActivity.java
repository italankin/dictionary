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
package com.italankin.dictionary.ui.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ShareCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arellomobile.mvp.MvpAppCompatActivity;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.italankin.dictionary.App;
import com.italankin.dictionary.R;
import com.italankin.dictionary.dto.Language;
import com.italankin.dictionary.dto.Result;
import com.italankin.dictionary.dto.TranslationEx;
import com.italankin.dictionary.ui.main.util.HidingViewBehavior;
import com.italankin.dictionary.ui.main.util.SwitchAnimation;
import com.italankin.dictionary.ui.settings.SettingsActivity;
import com.italankin.dictionary.ui.translation.TranslationActivity;
import com.italankin.dictionary.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static android.widget.AdapterView.OnItemSelectedListener;

@SuppressLint("RestrictedApi")
public class MainActivity extends MvpAppCompatActivity implements MainView {

    private static final int SWITCH_ANIM_DURATION = 450;
    private static final int SWAP_LANGS_ANIM_DURATION = 300;
    private static final long PROGRESS_ANIM_DURATION = 300;
    private static final int TOOLBAR_ANIM_IN_DURATION = 600;
    private static final int SHARE_FAB_ANIM_DURATION = 300;

    private static final int REQUEST_CODE_SHARE = 17;

    @InjectPresenter
    MainPresenter presenter;

    @Inject
    SharedPrefs prefs;

    @Inject
    InputMethodManager inputManager;

    @Inject
    ClipboardManager clipboardManager;

    private CoordinatorLayout rootLayout;
    /**
     * EditText for text input
     */
    private EditText input;
    private View inputLayout;
    /**
     * TextView in toolbar indicating current destination language
     */
    private Spinner spinnerSource;
    /**
     * TextView in toolbar indicating current source language
     */
    private Spinner spinnerDest;
    /**
     * Arrow in the toolbar for switching languages
     */
    private View swapLangs;
    private View lookup;
    private TextView transcription;
    private View toolbarInnerLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FloatingActionButton shareFab;
    private TranslationAdapter recyclerViewAdapter;

    @ProvidePresenter
    MainPresenter providePresenter() {
        return App.presenters().mainPresenter();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Activity callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.injector().inject(this);

        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.root);
        input = findViewById(R.id.edit_input);
        input.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                recyclerView.smoothScrollToPosition(0);
            }
        });
        inputLayout = findViewById(R.id.input_card);
        spinnerSource = findViewById(R.id.spinner_lang_source);
        spinnerDest = findViewById(R.id.spinner_lang_dest);
        swapLangs = findViewById(R.id.swap_langs);
        swapLangs.setOnClickListener(v -> swapLanguages());
        swapLangs.setOnLongClickListener(v -> {
            Toast.makeText(this, R.string.toast_swap_langs, Toast.LENGTH_SHORT).show();
            return true;
        });
        lookup = findViewById(R.id.lookup);
        lookup.setOnClickListener(view -> startLookup());
        transcription = findViewById(R.id.text_transcription);
        transcription.setOnClickListener(view -> {
            Intent intent = ShareCompat.IntentBuilder
                    .from(this)
                    .setType("text/plain")
                    .setText(transcription.getText().toString())
                    .createChooserIntent();
            startActivity(intent);
        });
        toolbarInnerLayout = findViewById(R.id.toolbar_inner_layout);
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        shareFab = findViewById(R.id.btn_share);
        shareFab.setOnClickListener(v -> shareLastResult());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupInputLayout();
        setupRecyclerView();
        setControlsState(false);

        presenter.loadLanguages();
    }

    private void setupInputLayout() {
        inputLayout.setOnClickListener(v -> input.requestFocus());
        ViewTreeObserver vto = inputLayout.getViewTreeObserver();
        // add a global layout listener to update RecyclerView's offset
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver vto = inputLayout.getViewTreeObserver();
                if (Build.VERSION.SDK_INT >= 16) {
                    vto.removeOnGlobalLayoutListener(this);
                } else {
                    vto.removeGlobalOnLayoutListener(this);
                }
                // Behavior which will offset input layout according to scroll events
                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) inputLayout.getLayoutParams();
                lp.setBehavior(new HidingViewBehavior(MainActivity.this, inputLayout,
                        recyclerView, inputLayout.getHeight()));
                inputLayout.getParent().requestLayout();
                resetViewsState();
            }
        });

        input.setImeActionLabel(getString(R.string.lookup), EditorInfo.IME_ACTION_SEARCH);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                startLookup();
                return true;
            }
            return false;
        });
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                resetViewsState();
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerViewAdapter = new TranslationAdapter(this);
        recyclerViewAdapter.setHasStableIds(true);
        // on click listener
        recyclerViewAdapter.setListener(new TranslationAdapter.OnAdapterItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Result result = presenter.getLastResult();
                TranslationEx item = result.translations.get(position);
                startActivity(TranslationActivity.getStartIntent(getApplicationContext(), item));
            }

            @Override
            public void onItemMenuClick(int position, int menuItemId) {
                ClipData clip = null;
                Result result = presenter.getLastResult();
                TranslationEx item = result.translations.get(position);
                switch (menuItemId) {
                    case R.id.action_lookup_word:
                        input.setText(item.text);
                        resetViewsState();
                        startLookup(item.text);
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
                    clipboardManager.setPrimaryClip(clip);
                    Toast.makeText(MainActivity.this, R.string.msg_copied, Toast.LENGTH_SHORT).show();
                }
            }
        });

        recyclerView.setAdapter(recyclerViewAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        int fabVisibility = shareFab.getVisibility();
        if (prefs.showShareFab()) {
            Result result = presenter.getLastResult();
            if (result != null && fabVisibility != View.VISIBLE) {
                showShareFab();
            }
        } else if (fabVisibility != View.GONE) {
            shareFab.setVisibility(View.GONE);
            recyclerViewAdapter.showExtraSpace(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.saveLanguages();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Called when a new {@link Intent} is received.
     *
     * @param intent new intent
     * @return {@code true}, if {@code intent} was successfully processed, {@code false} otherwise
     */
    private boolean handleIntent(Intent intent) {
        if (intent.getType() != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            int max = getResources().getInteger(R.integer.max_input_length);
            text = text.substring(0, Math.min(text.length(), max));
            if ("text/plain".equals(intent.getType()) && !TextUtils.isEmpty(text)) {
                intent.setType(null);
                input.setText(text);
                resetViewsState();
                startLookup(text);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SHARE && resultCode == RESULT_OK && prefs.closeOnShare()) {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (prefs.backFocusSearch() && !input.hasFocus()) {
            input.requestFocus();
        } else {
            super.onBackPressed();
        }
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
                shareLastResult();
                return true;

            case R.id.action_history:
                showHistoryDialog();
                return true;

            case R.id.action_settings:
                startActivity(SettingsActivity.getStartIntent(this));
                return true;
        }

        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Views state
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Enable or disable main controls.
     *
     * @param enabled new state
     */
    private void setControlsState(boolean enabled) {
        input.setEnabled(enabled);
        lookup.setEnabled(enabled);
        recyclerView.setEnabled(enabled);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Dialogs
    ///////////////////////////////////////////////////////////////////////////

    private void showHistoryDialog() {
        final ArrayList<String> history = presenter.getHistory();
        if (history.isEmpty()) {
            Toast.makeText(this, R.string.msg_history_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_history);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, history);
        builder.setAdapter(adapter, (dialog, which) -> {
            resetViewsState();
            startLookup(history.get(which));
        });
        builder.show();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Lookup
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Start lookup process.
     *
     * @param text text to lookup
     */
    private void startLookup(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        showProgressBar();
        inputManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
        input.clearFocus();
        presenter.lookup(text);
    }

    private void startLookup() {
        startLookup(input.getText().toString());
    }

    /**
     * Called from {@link MainPresenter}, when languages have been fetched from cache/net.
     *
     * @param languages   list of languages
     * @param destIndex   selected dest index
     * @param sourceIndex selected source index
     */
    @Override
    public void onLanguagesResult(List<Language> languages, int destIndex, int sourceIndex) {
        final LanguageAdapter adapter = new LanguageAdapter(this, languages);
//        adapter.setListener(Language::setFavorite); // TODO
        spinnerSource.setAdapter(adapter);
        spinnerSource.setSelection(sourceIndex);
        spinnerSource.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (presenter.setSourceLanguage(position)) {
                    startLookup();
                }
                presenter.sortLanguages();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                presenter.sortLanguages();
                adapter.notifyDataSetChanged();
            }
        });
        spinnerDest.setAdapter(adapter);
        spinnerDest.setSelection(destIndex);
        spinnerDest.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (presenter.setDestLanguage(position)) {
                    startLookup();
                }
                presenter.sortLanguages();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                presenter.sortLanguages();
                adapter.notifyDataSetChanged();
            }
        });

        toolbarInnerLayout.setVisibility(View.VISIBLE);
        toolbarInnerLayout.setTranslationY(-toolbarInnerLayout.getHeight());
        toolbarInnerLayout.setAlpha(0);
        toolbarInnerLayout.animate()
                .alpha(1)
                .translationY(0)
                .setDuration(TOOLBAR_ANIM_IN_DURATION)
                .setInterpolator(new DecelerateInterpolator(2.5f))
                .start();

        setControlsState(true);

        if (!handleIntent(getIntent())) {
            // if we are not coming from share intent
            Result result = presenter.getLastResult();
            if (result != null) {
                onLookupResult(result);
            }
            if (presenter.isRequestInProgress()) {
                showProgressBar();
            } else if (input.getText().length() == 0) {
                input.requestFocus();
            } else {
                input.clearFocus();
            }
        }
    }

    private void swapLanguages() {
        if (!presenter.swapLanguages()) {
            return;
        }
        startLookup();

        // anim stuff

        float rotation = 180f;
        if (swapLangs.getRotation() > 0) {
            rotation *= -1;
        }
        swapLangs.setRotation(0);
        swapLangs.animate()
                .rotationBy(rotation)
                .setDuration(SWAP_LANGS_ANIM_DURATION)
                .start();

        spinnerDest.setEnabled(false);
        spinnerSource.setEnabled(false);
        SwitchAnimation anim = new SwitchAnimation(spinnerDest,
                -spinnerDest.getHeight(),
                0f,
                SWITCH_ANIM_DURATION,
                () -> {
                    OnItemSelectedListener listener = spinnerDest.getOnItemSelectedListener();
                    spinnerDest.setOnItemSelectedListener(null);
                    spinnerDest.setSelection(presenter.getDestLanguageIndex());
                    spinnerDest.setOnItemSelectedListener(listener);
                    listener = spinnerSource.getOnItemSelectedListener();
                    spinnerSource.setOnItemSelectedListener(null);
                    spinnerSource.setSelection(presenter.getSourceLanguageIndex());
                    spinnerSource.setOnItemSelectedListener(listener);
                    spinnerDest.setEnabled(true);
                    spinnerSource.setEnabled(true);
                });
        anim.start();
        anim = new SwitchAnimation(spinnerSource, spinnerSource.getHeight(), 0, SWITCH_ANIM_DURATION, null);
        anim.start();
    }

    /**
     * Called from {@link MainPresenter} when received lookup result from server
     *
     * @param result result object returned from server
     */
    @Override
    public void onLookupResult(Result result) {
        List<TranslationEx> translations = result.translations;
        input.setText(result.text);
        input.clearFocus();
        if (!TextUtils.isEmpty(result.transcription)) {
            transcription.setText(String.format("[%s]", result.transcription));
        } else {
            transcription.setText("");
        }
        recyclerViewAdapter.setData(translations);
        recyclerView.scrollToPosition(0);
        if (recyclerView.getVisibility() != View.VISIBLE) {
            recyclerView.setVisibility(View.VISIBLE);
        }
        hideProgressBar();
        if (prefs.showShareFab() && shareFab.getVisibility() != View.VISIBLE) {
            showShareFab();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Errors
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Called when error occured. Typically it's related to server or connection problems/wrong
     * request.
     *
     * @param message text to show
     */
    @Override
    public void showError(int message) {
        Snackbar.make(input, message != 0 ? message : R.string.error, Snackbar.LENGTH_LONG)
                .setAction(android.R.string.ok, v -> {
                    // empty callback for showing OK button
                })
                .show();
        hideProgressBar();
    }

    /**
     * Show error when no results were received.
     */
    @Override
    public void onEmptyResult() {
        showError(R.string.error_no_results);
    }

    /**
     * If error was occured while fetching languages.
     */
    @Override
    public void onLanguagesError() {
        setControlsState(false);
        Snackbar snackbar = Snackbar.make(rootLayout, R.string.error_langs, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.retry, v -> presenter.loadLanguages());
        snackbar.show();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utility
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Reset translations applied by {@link HidingViewBehavior}.
     */
    private void resetViewsState() {
        inputLayout.setTranslationY(0);
        recyclerView.setTranslationY(inputLayout.getHeight());
        recyclerView.scrollToPosition(0);
    }

    /**
     * Share last result (currently displaying to user).
     */
    private void shareLastResult() {
        String[] result = presenter.getShareResult();
        if (result == null) {
            Toast.makeText(MainActivity.this, R.string.error_share, Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = ShareCompat.IntentBuilder
                    .from(this)
                    .setType("text/plain")
                    .setSubject(result[0])
                    .setText(result[1])
                    .createChooserIntent();
            startActivityForResult(intent, REQUEST_CODE_SHARE);
        }
    }

    /**
     * Show the progress bar.
     */
    private void showProgressBar() {
        if (progressBar.getVisibility() == View.INVISIBLE) {
            progressBar.setAlpha(0);
            progressBar.setVisibility(View.VISIBLE);
        } else if (progressBar.getAlpha() == 1) {
            return;
        }
        progressBar.animate()
                .alpha(1)
                .setDuration(PROGRESS_ANIM_DURATION)
                .setListener(null)
                .start();
    }

    /**
     * Hide the progress bar.
     */
    private void hideProgressBar() {
        if (progressBar.getVisibility() == View.VISIBLE) {
            progressBar.animate()
                    .alpha(0)
                    .setDuration(PROGRESS_ANIM_DURATION)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    })
                    .start();
        }
    }

    /**
     * Show share button.
     */
    private void showShareFab() {
        recyclerViewAdapter.showExtraSpace(true);
        shareFab.setVisibility(View.VISIBLE);
        shareFab.setScaleX(0);
        shareFab.setScaleY(0);
        shareFab.animate()
                .scaleX(1)
                .scaleY(1)
                .setDuration(SHARE_FAB_ANIM_DURATION)
                .setInterpolator(new DecelerateInterpolator(2))
                .start();
    }

}
