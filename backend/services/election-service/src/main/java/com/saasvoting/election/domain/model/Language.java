package com.saasvoting.election.domain.model;

import java.util.Locale;

/** Languages supported by ballots and voter communications (RF-20, RNF-5). */
public enum Language {
    ES,
    EN;

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses "es" or "en" (any case). */
    public static Language fromCode(String code) {
        return Language.valueOf(code.strip().toUpperCase(Locale.ROOT));
    }
}
