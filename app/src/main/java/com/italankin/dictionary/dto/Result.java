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
        String s = "";
        try {
            if (transcription != null && transcription.length() > 0) {
                s = String.format("[%s]\n", transcription);
            }

            if (translations != null && !translations.isEmpty()) {
                TranslationEx t = translations.get(0);
                s += t.text;
                if (t.means != null && t.means.length() > 0) {
                    s += " (" + t.means + ")";
                }
                for (int i = 1; i < translations.size(); i++) {
                    t = translations.get(i);
                    s += "\n" + t.text;
                    if (t.means != null && t.means.length() > 0) {
                        s += " (" + t.means + ")";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    public boolean isEmpty() {
        return rawResult == null || rawResult.isEmpty();
    }

}
