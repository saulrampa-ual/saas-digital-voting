package com.saasvoting.election.domain.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * RF-18: how the administrator identifies the person (data subject): the email (any case) or the phone of the census
 * (RI-3 keeps only digits, so spaces, dashes and a leading {@code +} are ignored).
 */
public record PrivacyQuery(String email, String phone) {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9]{1,15}$");

    public static PrivacyQuery parse(String value) {
        String text = value == null ? "" : value.strip();
        if (text.contains("@")) {
            String email = text.toLowerCase(Locale.ROOT);
            if (email.length() <= 100 && EMAIL.matcher(email).matches()) {
                return new PrivacyQuery(email, null);
            }
        } else {
            String phone = text.replaceAll("[\\s().+-]", "");
            if (PHONE.matcher(phone).matches()) {
                return new PrivacyQuery(null, phone);
            }
        }
        throw new InvalidVoterDataException("query", "Escribe el correo electrónico o el teléfono de la persona.");
    }
}
