package com.italankin.dictionary;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
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

import com.italankin.dictionary.dto.Translation;

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

    private EditText mInput;
    private TextView mTextDest;
    private TextView mTextSource;
    private ImageView mArrow;
    private View mLoad;
    private TextView mTranscription;
    private View mToolbarInner;

    private LanguageAdapter mLangsSourceAdapter;
    private LanguageAdapter mLangsDestAdapter;

    private RecyclerView mRecyclerView;
    private List<Translation> mList;

    private PublishSubject<String> mLookup = PublishSubject.create();
    private Subscription mLookupSub;
    private InputMethodManager mInputManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mPresenter = MainPresenter.getInstance(this);
        mPresenter.attach(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mToolbarInner = findViewById(R.id.toolbar_inner_layout);
        mTextDest = (TextView) findViewById(R.id.tvDirectionTo);
        mTextSource = (TextView) findViewById(R.id.tvDirectionFrom);

        mInput = (EditText) findViewById(R.id.etInput);
        mInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    mLookup.onNext(mInput.getText().toString());
                    return true;
                }
                return false;
            }
        });
        mTranscription = (TextView) findViewById(R.id.tvTranscription);

        mLoad = findViewById(R.id.tvLoad);
        mLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLookup.onNext(mInput.getText().toString());
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
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(mRecyclerView) {
            @Override
            public void onItemClick(View view, int position, boolean isLongClick) {
                if (!isLongClick) {
                    Translation item = mList.get(position);
                    mLookup.onNext(item.text);
                }
            }
        });

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        mPresenter.getLangs();
    }

    private void swapLangs() {
        if (!mPresenter.swapLangs()) {
            return;
        }
        mLookup.onNext(mInput.getText().toString());

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
                        updateView(false);
                    }
                });
        anim.start();
        anim = new SwitchAnimation(mTextDest, -mTextDest.getHeight(), 0, duration, null);
        anim.start();
    }

    private void startLookup(String text) {
        mPresenter.lookup(text);
        mInputManager.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
        mInput.clearFocus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mPresenter.isAttached()) {
            mPresenter.attach(this);
        }
        mLookupSub = mLookup
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
        getMenuInflater().inflate(R.menu.menu_translate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_copy:
                // TODO
                break;
        }

        return super.onOptionsItemSelected(item);
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

    private void updateView(boolean notify) {
        String source = mPresenter.getSource().getName();
        mTextSource.setText(source);
        String dest = mPresenter.getDest().getName();
        mTextDest.setText(dest);
        if (notify) {
            String message = getString(R.string.toast_switch, source, dest);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
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
                updateView(true);
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
                updateView(true);
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
        mLoad.setVisibility(View.VISIBLE);
        mLoad.setScaleX(0);
        mLoad.setScaleY(0);
        mLoad.animate()
                .scaleX(1)
                .scaleY(1)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator())
                .start();
        updateView(false);
    }

    public void onLookupResult(List<Translation> list, String transcription) {
        if (list == null || list.isEmpty()) {
            onError(getString(R.string.error_no_results));
            return;
        }
        mList = list;
        mTranscription.setText(transcription);
        RecyclerView.Adapter adapter = new DefinitionAdapter(list);
        mRecyclerView.setAdapter(adapter);
    }

    public void onError(String messaage) {
        if (messaage == null) {
            messaage = getString(R.string.error_query);
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
