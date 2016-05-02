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
package com.italankin.dictionary.ui.translation;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.italankin.dictionary.R;
import com.italankin.dictionary.dto.Attribute;
import com.italankin.dictionary.dto.TranslationEx;
import com.italankin.dictionary.ui.main.MainActivity;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Activity is displaying single translation item in the more detailed form.
 */
public class TranslationActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final String EXTRA_DATA = "data";

    private TranslationEx mData;

    //region Views
    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.text_translation)
    TextView textTrans;

    @Bind(R.id.text_position)
    TextView textPos;

    @Bind(R.id.card_means)
    View layoutMeans;

    @Bind(R.id.card_synonyms)
    View layoutSynonyms;

    @Bind(R.id.card_examples)
    View layoutExamples;
    //endregion

    public static Intent getStartIntent(Context context, TranslationEx data) {
        Intent starter = new Intent(context, TranslationActivity.class);
        starter.putExtra(EXTRA_DATA, data);
        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            mData = getIntent().getParcelableExtra(EXTRA_DATA);
        } else if (savedInstanceState != null) {
            mData = savedInstanceState.getParcelable(EXTRA_DATA);
        }

        setContentView(R.layout.activity_translation);
        ButterKnife.bind(this);

        // setup toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // translation text
        textTrans.setText(mData.text);
        textTrans.setOnClickListener(this);
        textTrans.setOnLongClickListener(this);

        // speech position text
        textPos.setText(mData.pos);

        // process means array, if we got one
        if (mData.mean != null && mData.mean.length > 0) {
            layoutMeans.setVisibility(View.VISIBLE);
            LinearLayout listMeans = (LinearLayout) findViewById(R.id.layout_means);
            addViewsForAttributes(listMeans, mData.mean);
        }

        // process synonyms array
        if (mData.syn != null && mData.syn.length > 0) {
            layoutSynonyms.setVisibility(View.VISIBLE);
            LinearLayout listSynonyms = (LinearLayout) findViewById(R.id.layout_synonyms);
            addViewsForAttributes(listSynonyms, mData.syn);
        }

        // process examples array
        if (mData.ex != null && mData.ex.length > 0) {
            layoutExamples.setVisibility(View.VISIBLE);
            LinearLayout listExamples = (LinearLayout) findViewById(R.id.layout_examples);
            addViewsForAttributes(listExamples, mData.ex);
        }
    }

    /**
     * Add separate view for every attribute in array.
     *
     * @param parent     a parent view group to which views will be added
     * @param attributes array of attributes
     */
    private void addViewsForAttributes(ViewGroup parent, Attribute[] attributes) {
        LayoutInflater inflater = getLayoutInflater();
        TextView view;
        for (Attribute a : attributes) {
            view = (TextView) inflater.inflate(R.layout.item_translation_text, parent, false);
            view.setText(a.text);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            parent.addView(view);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_DATA, mData);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.translation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                String text = mData.means + "\n" +
                        mData.synonyms + "\n" +
                        mData.examples;
                Intent intent = ShareCompat.IntentBuilder
                        .from(this)
                        .setType("text/plain")
                        .setText(text)
                        .setSubject(mData.text)
                        .getIntent();
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.error_no_app, Toast.LENGTH_SHORT).show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        // lookup word
        TextView textView = (TextView) v;
        Intent intent = new Intent(this, MainActivity.class);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, textView.getText().toString());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public boolean onLongClick(View v) {
        // share text with another applications
        String text = ((TextView) v).getText().toString();
        Intent intent = ShareCompat.IntentBuilder.from(this)
                .setText(text)
                .setChooserTitle(getString(R.string.share_word, text))
                .setType("text/plain")
                .createChooserIntent();
        startActivity(intent);
        return true;
    }

}
