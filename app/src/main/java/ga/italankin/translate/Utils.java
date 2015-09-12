package ga.italankin.translate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static Pattern sLangPattern = Pattern.compile("(.*) \\((.*)\\)");

    public static final String TAG = "HERO1M";
    public static final String API_KEY = "trnsl.1.1.20150909T160500Z.1739f3dcac749dac.c93fc2917663d5e294ecf7102dfb198c08d91a1c";

    /**
     * Extracts language code from string
     * @param s string to fetch data from
     * @return language code if success, itself otherwise
     */
    public static String extractLangCode(String s) {
        Matcher matcher = sLangPattern.matcher(s);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return s;
    }
    /**
     * Extracts language name from string
     * @param s string to fetch data from
     * @return language name if success, itself otherwise
     */
    public static String extractLangName(String s) {
        if (s == null) {
            return "";
        }
        Matcher matcher = sLangPattern.matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return s;
    }

    /**
     * Lookups languages array for a specific code
     * @param code language code
     * @param array array of languages
     * @return language name if found, <tt>code</tt> otherwise
     */
    public static String findLangNameByCode(String code, String[] array) {
        code = code.toLowerCase();
        if (array == null) {
            return code;
        }
        for (String s : array) {
            if (s.toLowerCase().contains("(" + code + ")")) {
                return s;
            }
        }
        return code;
    }
}
