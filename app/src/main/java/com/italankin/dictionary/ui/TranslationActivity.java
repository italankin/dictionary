package com.italankin.dictionary.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.italankin.dictionary.R;
import com.italankin.dictionary.dto.Translation;
import com.italankin.dictionary.dto.TranslationEx;

public class TranslationActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String EXTRA_DATA = "data";

    private TranslationEx mData;

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        TextView tvTranslation = (TextView) findViewById(R.id.translation);
        tvTranslation.setText(mData.text);
        TextView tvPosition = (TextView) findViewById(R.id.position);
        tvPosition.setText(mData.pos);

        View layoutMeans = findViewById(R.id.card_means);
        View layoutSynonyms = findViewById(R.id.card_synonyms);

        TextView textView;
        LayoutInflater inflater = getLayoutInflater();
        if (mData.mean != null) {
            LinearLayout listMeans = (LinearLayout) findViewById(R.id.list_means);
            layoutMeans.setVisibility(View.VISIBLE);
            for (Translation.Mean m : mData.mean) {
                textView = (TextView) inflater.inflate(R.layout.item_translation_text, listMeans, false);
                textView.setText(m.text);
                textView.setOnClickListener(this);
                listMeans.addView(textView);
            }
        }
        if (mData.syn != null) {
            LinearLayout listSynonyms = (LinearLayout) findViewById(R.id.list_synonyms);
            layoutSynonyms.setVisibility(View.VISIBLE);
            for (Translation.Synonym s : mData.syn) {
                textView = (TextView) inflater.inflate(R.layout.item_translation_text, listSynonyms, false);
                textView.setText(s.text);
                textView.setOnClickListener(this);
                listSynonyms.addView(textView);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_DATA, mData);
    }

    @Override
    public void onClick(View v) {
        TextView textView = (TextView) v;
        Intent intent = new Intent(this, MainActivity.class);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, textView.getText().toString());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}
