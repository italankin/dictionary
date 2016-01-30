package com.italankin.dictionary.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.italankin.dictionary.R;
import com.italankin.dictionary.dto.Translation;
import com.italankin.dictionary.dto.TranslationEx;

public class TranslationActivity extends AppCompatActivity {

    private static final String EXTRA_DATA = "data";
    private static final String EXTRA_TRANSCRIPTION = "transcription";

    private TranslationEx mData;

    public static Intent getStartIntent(Context context, TranslationEx data, String transcription) {
        Intent starter = new Intent(context, TranslationActivity.class);
        starter.putExtra(EXTRA_DATA, data);
        starter.putExtra(EXTRA_TRANSCRIPTION, transcription);
        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String tr = null;
        if (getIntent() != null) {
            mData = getIntent().getParcelableExtra(EXTRA_DATA);
            tr = getIntent().getStringExtra(EXTRA_TRANSCRIPTION);
        } else if (savedInstanceState != null) {
            mData = savedInstanceState.getParcelable(EXTRA_DATA);
            tr = savedInstanceState.getString(EXTRA_TRANSCRIPTION);
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

        CustomLayout listMeans = (CustomLayout) findViewById(R.id.list_means);
        CustomLayout listSynonyms = (CustomLayout) findViewById(R.id.list_synonyms);

        TextView textView;
        LayoutInflater inflater = getLayoutInflater();
        if (mData.mean != null) {
            for (Translation.Mean m : mData.mean) {
                textView = (TextView) inflater.inflate(R.layout.item_mean, listMeans, false);
                textView.setText(m.text);
                listMeans.addView(textView);
            }
        }
        if (mData.syn != null) {
            for (Translation.Synonym s : mData.syn) {
                textView = (TextView) inflater.inflate(R.layout.item_syn, listSynonyms, false);
                textView.setText(s.text);
                listSynonyms.addView(textView);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_DATA, mData);
    }
}
