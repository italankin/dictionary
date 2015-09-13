package ga.italankin.translate;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class Utils {

    public static final String TAG = "HERO1M";
    public static final String API_KEY = "trnsl.1.1.20150909T160500Z.1739f3dcac749dac.c93fc2917663d5e294ecf7102dfb198c08d91a1c";
    private static final String DIR = "data";
    private static final String FILE = "languages.json";

    private static final String KEY_CODE = "code";
    private static final String KEY_NAME = "name";
    private static final String KEY_FAV = "fav";

    /**
     * Lookups languages array for a specific code
     *
     * @param code  language codea
     * @param array array of languages
     * @return language name if found, <tt>code</tt> otherwise
     */
    public static Language findLanguageByCode(String code, ArrayList<Language> array) {
        code = code.toLowerCase();
        if (array != null) {
            for (Language item : array) {
                if (item.getCode().toLowerCase().equals(code)) {
                    return item;
                }
            }
        }
        return new Language(code, code);
    }

    /**
     * Read language settings file
     *
     * @param context application context
     * @return list of languages, or null
     */
    public static ArrayList<Language> readLanguagesFile(Context context) {
        ArrayList<Language> result;
        try {
            File file = new File(context.getDir(DIR, Context.MODE_PRIVATE).getPath()
                    + File.separator + FILE);

            // get file input stream
            FileInputStream is = new FileInputStream(file);

            // buffer
            byte[] buffer = new byte[(int) file.length()];

            // read file contents
            int size = is.read(buffer);
            is.close();

            // return null if file is empty
            if (size == 0) {
                return null;
            }

            // convert json to list
            JSONArray json = new JSONArray(new String(buffer));
            result = new ArrayList<>();
            for (int i = 0; i < json.length(); i++) {
                JSONObject o = json.getJSONObject(i);
                result.add(new Language(
                        o.getString(KEY_CODE),
                        o.getString(KEY_NAME),
                        o.getInt(KEY_FAV) == 1));
            }
        } catch (Exception e) {
            Log.e(TAG, "readLanguagesFile failed with exception: " + e.toString());
            return null;
        }
        return result;
    }

    /**
     * Write language settings
     *
     * @param context application context
     * @param list    list of {@link Language}s to write on disk
     */
    public static void writeLanguagesFile(Context context, ArrayList<Language> list) {
        try {
            String file = context.getDir(DIR, Context.MODE_PRIVATE).getPath()
                    + File.separator + FILE;

            // get file output stream
            FileOutputStream os = new FileOutputStream(file);

            // create new json array
            JSONArray json = new JSONArray();
            JSONObject item;
            for (Language lang : list) {
                item = new JSONObject();
                item.put(KEY_CODE, lang.getCode());
                item.put(KEY_NAME, lang.getName());
                item.put(KEY_FAV, lang.isFavorite() ? 1 : 0);
                json.put(item);
            }

            // write json string
            os.write(json.toString().getBytes());
            os.close();
        } catch (Exception e) {
            Log.e(TAG, "writeLanguagesFile failed with exception: " + e.toString());
        }
    }

    /**
     * Retrieves error message for response code
     *
     * @param context application context
     * @param code response code
     * @return message, assosiated with a code, or null
     */
    public static String getErrorMessage(Context context, int code) {
        String message = null;
        switch (code) {
            // no connection
            case 0:
                message = context.getString(R.string.error_no_connection);
                break;

            // data received from server failed to parse
            case 1:
                message = context.getString(R.string.error_nonsense);
                break;

            // something strange happened, generally request sending failed
            case 2:
                message = context.getString(R.string.error_something_wrong);
                break;

            case 200:
                // OK
                break;

            // translate direction is not supported by server
            case 400:
                message = context.getString(R.string.error_not_supported);
                break;

            // invalid API key
            case 401:
                message = context.getString(R.string.error_invalid_key);
                break;

            // API key is blocked
            case 402:
                message = context.getString(R.string.error_blocked_key);
                break;

            // daily query limit reached
            case 403:
                message = context.getString(R.string.error_daily_limit);
                break;

            // daily translation volume reached
            case 404:
                message = context.getString(R.string.error_volume_limit);
                break;

            // query length is too big
            case 405:
                message = context.getString(R.string.error_char_limit);
                break;

            // server failed to translate text
            case 422:
                message = context.getString(R.string.error_unprocessable);
                break;

            // language is not supported by server
            case 501:
                message = context.getString(R.string.error_invalid_lang);
                break;

            // for unknown codes
            default:
                message = context.getString(R.string.error_unknown);
                break;
        }
        return message;
    }
}
