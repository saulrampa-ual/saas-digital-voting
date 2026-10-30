package com.saasvoting.voteraccess.infrastructure.adapter.out.crypto;

import com.saasvoting.voteraccess.application.port.out.SecretGenerator;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** {@link SecureRandom} secrets (RI-12: 64-character link) and SHA-256 hashes. */
@Component
public class SecureRandomSecretGenerator implements SecretGenerator {

    private static final Base64.Encoder URL_SAFE = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom random = new SecureRandom();

    /** 48 random bytes = 64 URL-safe characters (contract pattern of {@code linkToken}). */
    @Override
    public String newLinkToken() {
        return randomUrlSafe(48);
    }

    /** 32 random bytes (256 bits) = 43 URL-safe characters. */
    @Override
    public String newVotingToken() {
        return randomUrlSafe(32);
    }

    @Override
    public String newAccessCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    @Override
    public String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String randomUrlSafe(int bytes) {
        byte[] buffer = new byte[bytes];
        random.nextBytes(buffer);
        return URL_SAFE.encodeToString(buffer);
    }
}
