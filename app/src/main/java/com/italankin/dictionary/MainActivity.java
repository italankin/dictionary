package com.italankin.dictionary;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.italankin.dictionary.dto.Definition;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MainPresenter mPresenter;

    private EditText mInput;
    private TextView mOutput;
    private TextView mTextDest;
    private TextView mTextSource;

    private LanguageAdapter mLangsSourceAdapter;
    private LanguageAdapter mLangsDestAdapter;

    // for touch events handling
    private float x;
    private float y;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPresenter = MainPresenter.getInstance(this);
        mPresenter.attach(this);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mOutput = (TextView) findViewById(R.id.tvOutput);
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

        mPresenter.getLangs();
    }

    private void startLookup() {
        String input = mInput.getText().toString();
        if (TextUtils.isEmpty(input)) {
            return;
        }
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

            case R.id.action_switch:
                mPresenter.swapLangs();
                updateView(true);
                break;
        }

        return super.onOptionsItemSelected(item);
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
                    mPresenter.swapLangs();
                    updateView(true);
                } else {
                    if (y < 0) {
                        // move up - translate text
                        startLookup();
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

    public void onLookupResult(List<Definition> list) {
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
        // TODO
        mOutput.setText(list.get(0).tr[0].text);
    }
}
