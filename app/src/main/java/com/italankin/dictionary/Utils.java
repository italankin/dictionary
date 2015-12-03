package com.italankin.dictionary;

import android.content.Context;
import android.util.Log;

import com.italankin.dictionary.dto.Language;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class Utils {

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
