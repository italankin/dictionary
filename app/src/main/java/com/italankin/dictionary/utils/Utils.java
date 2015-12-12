package com.italankin.dictionary.utils;

import com.italankin.dictionary.dto.Result;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    
    //// TODO: 12.12.2015  

    public static final String FORMAT_CHAR = "\\$";
    public static final String PARAM_TRANSCRIPTION = "ts";
    public static final String PARAM_TEXT = "text";
    public static final String PARAM_TRANSLATION = "tr";

    private static Pattern PATTERN1 = Pattern.compile(FORMAT_CHAR + "[a-zA-Z]+" + FORMAT_CHAR);
    private static Pattern PATTERN2 = Pattern.compile(FORMAT_CHAR + "([a-zA-Z]+)(\\d+)" + FORMAT_CHAR);

    public static String formattedResult(Result result, String format, String formatTr) {
        Matcher matcher = PATTERN1.matcher(format);
        String s = format;
        while (matcher.find()) {
            String g = matcher.group();
            switch (g.toLowerCase()) {
                case PARAM_TRANSCRIPTION:
                    s = s.replace(g, result.transcription);
                    break;
                case PARAM_TEXT:
                    s = s.replace(g, result.text);
                    break;
                case PARAM_TRANSLATION:
                    s = s.replace(g, formatTranslation(result, format, formatTr));
                    break;
            }
        }
        return s;
    }

    private static String formatTranslation(Result result, String format, String formatTr) {
        Matcher matcher = PATTERN2.matcher(formatTr);
        while (matcher.find()) {
            String g = matcher.group(1);
            switch (g) {
                case PARAM_TRANSLATION:
            }
        }
        return null;
    }

}
