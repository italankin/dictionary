package com.italankin.dictionary.dto;

import java.util.ArrayList;
import java.util.List;

public class Result {

    public List<Definition> rawResult;
    public String text;
    public List<TranslationEx> translations;
    public String transcription;

    public Result(List<Definition> definitions) {
        this.rawResult = definitions;
        this.transcription = "";
        this.text = "";
        List<TranslationEx> list = new ArrayList<>(0);
        for (Definition d : definitions) {
            for (Translation t : d.tr) {
                list.add(new TranslationEx(t));
            }
            if (d.text != null && this.text.length() == 0) {
                this.text = d.text;
            }
            if (d.ts != null && this.transcription.length() == 0) {
                this.transcription = d.ts;
            }
        }
        this.translations = list;
    }

    @Override
    public String toString() {
        String s = transcription;
        for (int i1 = 0; i1 < translations.size(); i1++) {
            if (i1 > 0 || transcription.length() > 0) {
                s += "\n";
            }
            TranslationEx t = translations.get(i1);
            s += t.text;
            if (t.means != null && t.means.length() > 0) {
                s += " (" + t.means + ")";
            }
        }
        return s;
    }

    public boolean isEmpty() {
        return rawResult == null || rawResult.isEmpty();
    }

}
