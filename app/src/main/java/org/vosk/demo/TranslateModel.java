package org.vosk.demo;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

enum TranslateModel {
    ENGLISH(R.id.radio_en_2, "en"),

    VIETNAMESE(R.id.radio_vi_2, "vi");

    private final String code;
    private final int radioId;

    TranslateModel(int radioId, String code) {
        this.radioId = radioId;
        this.code = code;
    }

    public int radioId() {
        return radioId;
    }

    public String code() {
        return code;
    }

    public static TranslateModel fromIndex(int index) {
        return values()[index];
    }

    public static TranslateModel current(Context context) {
        return fromIndex(getIndex(context));
    }

    public static TranslateModel fromId(int id) {
        for (TranslateModel languageModel : values()) {
            if (languageModel.radioId == id) {
                return languageModel;
            }
        }
        return null;
    }

    public static int getIndex(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE);
        return sharedPreferences.getInt(TranslateModel.class.getSimpleName(), 0);
    }

    public static void saveIndex(Context context, int index) {
        SharedPreferences.Editor edit = context.getSharedPreferences(context.getPackageName(), MODE_PRIVATE).edit();
        edit.putInt(TranslateModel.class.getSimpleName(), index);
        edit.apply();
    }
}


