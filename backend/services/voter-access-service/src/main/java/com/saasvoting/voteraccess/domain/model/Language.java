package com.saasvoting.voteraccess.domain.model;

import java.util.Locale;

/** Languages of the voter screens and messages (RF-20, RNF-5). */
public enum Language {
    ES,
    EN;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Language fromCode(String code) {
        return Language.valueOf(code.strip().toUpperCase(Locale.ROOT));
    }
}
