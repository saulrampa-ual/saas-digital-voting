package com.saasvoting.election.domain.model;

import java.util.regex.Pattern;

/**
 * Shape of an email address accepted by VeriVoto (RI-3, RF-18): {@code ^[^@\s]+@[^@\s]+\.[^@\s]+$}, that is, a local part
 * and a domain without spaces or a second {@code @}, and a dot in the domain that is neither its first nor its last
 * character. Checked without that pattern because its two adjacent {@code [^@\s]+} groups backtrack (SonarQube S8786).
 */
final class EmailRule {

    private static final Pattern LOCAL_AND_DOMAIN = Pattern.compile("^[^@\\s]+@[^@\\s]+$");

    private EmailRule() {
    }

    static boolean isValid(String email) {
        if (email == null || !LOCAL_AND_DOMAIN.matcher(email).matches()) {
            return false;
        }
        String domain = email.substring(email.indexOf('@') + 1);
        return domain.length() >= 3 && domain.substring(1, domain.length() - 1).indexOf('.') >= 0;
    }
}
