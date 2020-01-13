package com.italankin.dictionary.ui.main;

import com.arellomobile.mvp.MvpView;
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy;
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType;
import com.arellomobile.strategy.TagStrategy;
import com.italankin.dictionary.api.dto.Language;
import com.italankin.dictionary.api.dto.Result;

import java.util.List;

public interface MainView extends MvpView {

    String TAG_LANGS = "langs";
    String TAG_RESULT = "result";

    @StateStrategyType(value = TagStrategy.class, tag = TAG_LANGS)
    void onLanguagesResult(List<Language> languages, int destIndex, int sourceIndex);

    @StateStrategyType(value = TagStrategy.class, tag = TAG_LANGS)
    void onLanguagesError();

    @StateStrategyType(value = TagStrategy.class, tag = TAG_RESULT)
    void onLookupResult(Result result);

    @StateStrategyType(OneExecutionStateStrategy.class)
    void showError(int message);

    @StateStrategyType(OneExecutionStateStrategy.class)
    void onEmptyResult();

    @StateStrategyType(OneExecutionStateStrategy.class)
    void switchLanguages(int destIndex, int sourceIndex);
}
