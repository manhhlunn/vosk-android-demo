package org.vosk.demo;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

enum LanguageModel {
    ENGLISH("model-en", R.id.radio_en, "en"),

    CHINESE("model-cn", R.id.radio_cn, "zh-CN"),

    RUSSIAN("model-ru", R.id.radio_ru,"ru"),

    JAPANESE("model-ja", R.id.radio_ja,"ja");

    private final String path;

    private final String code;
    private final int radioId;

    LanguageModel(String path, int radioId, String code) {
        this.path = path;
        this.radioId = radioId;
        this.code = code;
    }

    public String path() {
        return path;
    }

    public int radioId() {
        return radioId;
    }

    public String code() {
        return code;
    }

    public static LanguageModel fromIndex(int index) {
        return values()[index];
    }

    public static LanguageModel current(Context context) {
        return fromIndex(getIndex(context));
    }

    public static LanguageModel fromId(int id) {
        for (LanguageModel languageModel : values()) {
            if (languageModel.radioId == id) {
                return languageModel;
            }
        }
        return null;
    }

    public static int getIndex(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        return sharedPreferences.getInt(LanguageModel.class.getSimpleName(), 0);
    }

    public static void saveIndex(Context context, int index) {
        SharedPreferences.Editor edit = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE).edit();
        edit.putInt(LanguageModel.class.getSimpleName(), index);
        edit.apply();
    }
}


