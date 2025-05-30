package com.example.compresseur_mobile;

import android.support.annotation.DrawableRes;

import androidx.annotation.NonNull;

public class Language {

    public final String code;
    public final @DrawableRes int flag;
    public final String displayName;

    public Language(String code, int flag, String displayName) {
        this.code = code;
        this.flag = flag;
        this.displayName = displayName;
    }

    @NonNull
    @Override public String toString() { return displayName; }


}
