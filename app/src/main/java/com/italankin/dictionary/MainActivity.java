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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.italankin.dictionary.dto.Translation;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MainPresenter mPresenter;

    private EditText mInput;
    private TextView mTextDest;
    private TextView mTextSource;
    private ImageView mArrow;

    private LanguageAdapter mLangsSourceAdapter;
    private LanguageAdapter mLangsDestAdapter;

    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPresenter = MainPresenter.getInstance(this);
        mPresenter.attach(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextDest = (TextView) findViewById(R.id.tvDirectionTo);
        mTextSource = (TextView) findViewById(R.id.tvDirectionFrom);
        mInput = (EditText) findViewById(R.id.etInput);
        mInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    startLookup();
                    return true;
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

        mArrow = (ImageView) findViewById(R.id.ivArrow);
        mArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swapLangs();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        mPresenter.getLangs();
    }

    private void swapLangs() {
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
                        mPresenter.swapLangs();
                        updateView(false);
                    }
                });
        anim.start();
        anim = new SwitchAnimation(mTextDest, -mTextDest.getHeight(), 0, duration, null);
        anim.start();
    }

    private void startLookup() {
        String input = mInput.getText().toString();
        if (TextUtils.isEmpty(input)) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
        mInput.clearFocus();
        mPresenter.lookup(input);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mPresenter.isAttached()) {
            mPresenter.attach(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPresenter.saveLangs();
        mPresenter.detach();
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
        updateView(false);
    }

    public void onLookupResult(List<Translation> list) {
        if (list == null || list.isEmpty()) {
            Snackbar snackbar = Snackbar.make(mInput, R.string.error_no_results, Snackbar.LENGTH_LONG);
            snackbar.setAction(android.R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //
                }
            });
            snackbar.show();
            return;
        }
        RecyclerView.Adapter adapter = new DefinitionAdapter(list);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(mRecyclerView) {
            @Override
            public void onItemClick(View view, int position, boolean isLongClick) {
                // TODO
            }
        });
    }
}
