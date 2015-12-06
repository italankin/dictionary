package com.italankin.dictionary.dto;

public class TranslationEx extends Translation {

    public String means = "";
    public String examples = "";
    public String synonyms = "";
    public String transcription = "";

    public TranslationEx(Translation t) {
        this.mean = t.mean;
        this.ex = t.ex;
        this.syn = t.syn;
        this.text = t.text;
        this.pos = t.pos;
        this.num = t.num;
        this.gen = t.gen;
        this.asp = t.asp;

        if (this.mean != null) {
            this.means = "";
            for (Mean m : this.mean) {
                if (this.means.length() > 0) {
                    this.means += ", " + m.text;
                } else {
                    this.means = m.text;
                }
            }
        }

        if (this.ex != null) {
            this.examples = "";
            for (Example e : this.ex) {
                if (this.examples.length() > 0) {
                    this.examples += ", " + e.text;
                } else {
                    this.examples = e.text;
                }
            }
        }

        if (this.syn != null) {
            this.synonyms = "";
            for (Synonym s : this.syn) {
                if (this.synonyms.length() > 0) {
                    this.synonyms += ", " + s.text;
                } else {
                    this.synonyms = s.text;
                }
            }
        }
    }

}
