package com.saasvoting.voteraccess.application.port.out;

/** Cryptographically secure random secrets and their hashes (RI-12: only hashes are stored). */
public interface SecretGenerator {

    /** Personal link token: 64 URL-safe characters. */
    String newLinkToken();

    /** Voting token: URL-safe, at least 256 bits. */
    String newVotingToken();

    /** Access code: 6 digits. */
    String newAccessCode();

    /** Hex SHA-256 of the value. */
    /** Results token after the tally (D-41): 256 random bits, URL-safe. */
    String newResultsToken();

    String hash(String value);
}
