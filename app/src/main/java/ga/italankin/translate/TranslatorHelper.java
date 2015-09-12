package ga.italankin.translate;

public class TranslatorHelper {

    public static final String TRANSLATE_URL = "https://translate.yandex.net/api/v1.5/tr.json/translate";
    public static final String LANGS_URL = "https://translate.yandex.net/api/v1.5/tr.json/getLangs";

    private String fromLang;
    private String toLang;
    private boolean auto = false;

    public TranslatorHelper() {
        super();
    }

    public TranslatorHelper(String to) {
        toLang = to;
        auto = true;
    }

    public TranslatorHelper(String from, String to) {
        fromLang = from;
        toLang = to;
    }

    public String langsUrl(String locale, String apiKey) {
        return String.format("%s?key=%s&ui=%s", LANGS_URL, apiKey, locale);
    }

    public String translateUrl(String text, String apiKey) {
        String lang = toLang;
        if (!auto) {
            lang = fromLang + "-" + toLang;
        }
        return String.format("%s?key=%s&lang=%s&text=%s", TRANSLATE_URL, apiKey, lang.toLowerCase(), text);
    }

}
