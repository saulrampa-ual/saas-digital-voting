package com.saasvoting.election.domain.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Text in several languages (RF-20). Values are stripped and blank values are dropped, so a language is present
 * only when it has real text. It can be empty; whoever uses it decides which languages are required.
 */
public record LocalizedText(Map<Language, String> values) {

    public static final LocalizedText EMPTY = new LocalizedText(Map.of());

    public LocalizedText {
        Objects.requireNonNull(values, "values must not be null");
        EnumMap<Language, String> normalized = new EnumMap<>(Language.class);
        values.forEach((language, text) -> {
            Objects.requireNonNull(language, "language must not be null");
            if (text != null && !text.isBlank()) {
                normalized.put(language, text.strip());
            }
        });
        values = Collections.unmodifiableMap(normalized);
    }

    public static LocalizedText of(String es, String en) {
        EnumMap<Language, String> values = new EnumMap<>(Language.class);
        values.put(Language.ES, es);
        values.put(Language.EN, en);
        return new LocalizedText(values);
    }

    public Optional<String> get(Language language) {
        return Optional.ofNullable(values.get(language));
    }

    public boolean has(Language language) {
        return values.containsKey(language);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Set<Language> languages() {
        return values.keySet();
    }

    /** Length of the longest translation, to check the limits of RI-4 and RI-5. */
    public int maxLength() {
        return values.values().stream().mapToInt(String::length).max().orElse(0);
    }
}
