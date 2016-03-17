package com.italankin.dictionary.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Container object used to represent query result.
 */
public class Result {

    public List<Definition> rawResult;
    public String text;
    public List<TranslationEx> translations;
    public String transcription;

    public Result(List<Definition> definitions) {
        this.rawResult = definitions;
        this.transcription = "";
        this.text = "";
        List<TranslationEx> list = new ArrayList<>();
        for (Definition d : definitions) {
            for (Translation t : d.tr) {
                list.add(new TranslationEx(t));
            }
            if (d.text != null && text.length() == 0) {
                this.text = d.text;
            }
            if (d.ts != null && transcription.length() == 0) {
                this.transcription = d.ts;
            }
        }
        this.translations = list;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (transcription != null && transcription.length() > 0) {
            sb.append("[");
            sb.append(transcription);
            sb.append("]\n");
        }
        if (translations != null && !translations.isEmpty()) {
            TranslationEx t;
            for (int i = 0; i < translations.size(); i++) {
                t = translations.get(i);
                if (i != 0) {
                    sb.append("\n");
                }
                sb.append(t.text);
                if (t.means != null && t.means.length() > 0) {
                    sb.append(" (");
                    sb.append(t.means);
                    sb.append(")");
                }
            }
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return rawResult == null || rawResult.isEmpty();
    }

}
