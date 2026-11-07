package com.saasvoting.ballotbox.domain.model;

import java.util.Locale;

/** Languages of the ballot (RF-20). */
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
