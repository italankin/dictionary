/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.italankin.dictionary.dto;

import android.support.annotation.NonNull;

/**
 * Class for handling languages data.
 */
public class Language implements Comparable<Language> {

    private String code;
    private String name;
    private boolean favorite = false;

    public Language(String code, String name) {
        this.code = code;
        this.name = name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    @Override
    public int compareTo(@NonNull Language another) {
        if (this.favorite && !another.favorite) {
            return -1;
        } else if (!this.favorite && another.favorite) {
            return 1;
        }
        return this.name.compareTo(another.name);
    }

}
