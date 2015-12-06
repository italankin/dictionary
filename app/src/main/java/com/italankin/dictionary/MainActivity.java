package com.italankin.dictionary;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    private View mInputCard;
    private TextView mTextDest;
    private TextView mTextSource;
    private ImageView mArrow;
    private View mLookup;
    private TextView mTranscription;
    private View mToolbarInner;

    private RecyclerView mRecyclerView;
    private DefinitionAdapter.OnAdapterItemClickListener mRecyclerViewListener;
    private DefinitionAdapter mRecyclerViewAdapter;
    private List<TranslationEx> mList;

    private LanguageAdapter mLangsSourceAdapter;
    private LanguageAdapter mLangsDestAdapter;

    private PublishSubject<String> mLookupEvents = PublishSubject.create();
    private Subscription mLookupSub;

    private InputMethodManager mInputManager;

    private int mScroll = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mPresenter = MainPresenter.getInstance(this);
        mPresenter.attach(this);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mToolbarInner = findViewById(R.id.toolbar_inner_layout);
        mTextDest = (TextView) findViewById(R.id.tvDirectionTo);
        mTextSource = (TextView) findViewById(R.id.tvDirectionFrom);

        mInputCard = findViewById(R.id.input_card);
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
                showDestDialog();
            }
        });
        mTextSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSourceDialog();
            }
        });

        mArrow = (ImageView) findViewById(R.id.ivArrow);
        mArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swapLangs();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mScroll += dy;
                int factor = 3;
                int max = mInputCard.getHeight() * factor + 5;
                int value = max;
                if (Math.abs(mScroll / factor) < max) {
                    value = mScroll / factor;
                }
                mInputCard.setTranslationY(-value);
            }
        });
        mRecyclerViewListener = new DefinitionAdapter.OnAdapterItemClickListener() {
            @Override
            public void onClick(int position) {
                TranslationEx item = mList.get(position);
                lookupNext(item.text);
            }

            @Override
            public void onMenuItemClick(int position, int menuItemId) {
                switch (menuItemId) {
                    case R.id.action_lookup_word:
                        TranslationEx item = mList.get(position);
                        lookupNext(item.text);
                        break;
                    case R.id.action_copy_mean:
                    case R.id.action_copy_synonyms:
                    case R.id.action_copy_translation:
                        Toast.makeText(MainActivity.this, "Copied!", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        mPresenter.getLangs();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mPresenter.isAttached()) {
            mPresenter.attach(this);
        }
        mLookupSub = mLookupEvents
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return !TextUtils.isEmpty(s);
                    }
                })
                .debounce(400, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        mInput.setText(s);
                        startLookup(s);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPresenter.saveLangs();
        mPresenter.detach();
        if (mLookupSub != null && !mLookupSub.isUnsubscribed()) {
            mLookupSub.unsubscribe();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                String result = mPresenter.getLastResult();
                if (result != null) {
                    Intent intent = ShareCompat.IntentBuilder.from(this)
                            .setType("text/plain")
                            .setText(mPresenter.getShareText())
                            .setSubject(result)
                            .getIntent();
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.error_share, Toast.LENGTH_SHORT).show();
                }
                return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        if (mInput.getText().length() > 0) {
            mInput.setText("");
            mInput.requestFocus();
            mInputManager.showSoftInput(mInput, 0);
            return;
        }
        super.onBackPressed();
    }

    private void swapLangs() {
        if (!mPresenter.swapLangs()) {
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

        SwitchAnimation anim = new SwitchAnimation(mTextSource, mTextSource.getHeight(), 0, duration,
                new SwitchAnimation.OnSwitchListener() {
                    @Override
                    public void onSwitch() {
                        updateView();
                    }
                });
        anim.start();
        anim = new SwitchAnimation(mTextDest, -mTextDest.getHeight(), 0, duration, null);
        anim.start();
    }

    private void lookupNext(String s) {
        mLookupEvents.onNext(s);
        mRecyclerView.smoothScrollToPosition(0);
    }

    private void startLookup(String text) {
        mPresenter.lookup(text);
        mInputManager.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
        mInput.clearFocus();
    }

    private void updateView() {
        String source = mPresenter.getSource().getName();
        mTextSource.setText(source);
        String dest = mPresenter.getDest().getName();
        mTextDest.setText(dest);
    }

    public void showSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.source);
        if (mLangsSourceAdapter == null) {
            mLangsSourceAdapter = new LanguageAdapter(this, mPresenter.getLangsList());
        }
        mPresenter.sortLangsList();
        mLangsSourceAdapter.notifyDataSetChanged();
        builder.setAdapter(mLangsSourceAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPresenter.setSourceLang(which);
                updateView();
            }
        });
        builder.show();
    }

    public void showDestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.destination);
        if (mLangsDestAdapter == null) {
            mLangsDestAdapter = new LanguageAdapter(this, mPresenter.getLangsList());
        }
        mPresenter.sortLangsList();
        mLangsDestAdapter.notifyDataSetChanged();
        builder.setAdapter(mLangsDestAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPresenter.setDestLang(which);
                updateView();
            }
        });
        builder.show();
    }

    public void onLangsResult() {
        mToolbarInner.setVisibility(View.VISIBLE);
        mToolbarInner.setAlpha(0);
        mToolbarInner.animate()
                .alpha(1)
                .setDuration(600)
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
        updateView();
    }

    public void onLookupResult(List<TranslationEx> list, String transcription) {
        if (list == null || list.isEmpty()) {
            onError(getString(R.string.error_no_results));
            return;
        }
        mList = list;
        mTranscription.setText(transcription);
        if (mRecyclerViewAdapter == null) {
            mRecyclerViewAdapter = new DefinitionAdapter(mToolbar.getHeight() + mInputCard.getHeight(),
                    mRecyclerViewListener);
            mRecyclerView.setAdapter(mRecyclerViewAdapter);
        }
        mRecyclerViewAdapter.setItems(list);
    }

    public void onError(String messaage) {
        if (messaage == null) {
            messaage = getString(R.string.error);
        }
        Snackbar snackbar = Snackbar.make(mInput, messaage, Snackbar.LENGTH_LONG);
        snackbar.setAction(android.R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //
            }
        });
        snackbar.show();
    }

}
