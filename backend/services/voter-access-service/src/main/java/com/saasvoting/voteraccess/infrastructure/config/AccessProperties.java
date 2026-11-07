package com.saasvoting.voteraccess.infrastructure.config;

import com.saasvoting.voteraccess.domain.model.AccessPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Voter access settings (D-31): base URL of the voter PWA for the personal links, time zone of the dates shown and
 * of the voting day (D-18), sender address, the limits of {@link AccessPolicy} and the Keycloak client of the
 * institutional account of voters (RF-05b).
 */
@ConfigurationProperties(prefix = "verivoto.access")
public record AccessProperties(
        String publicBaseUrl,
        ZoneId zone,
        String mailFrom,
        Duration invitationInterval,
        Duration codeValidity,
        Duration resendDelay,
        Duration codeWindow,
        Integer maxCodesPerWindow,
        Integer maxFailedAttempts,
        Duration lockDuration,
        Duration votingTokenValidity,
        Duration linkValidityAfterClose,
        Duration resultsTokenValidity,
        String ssoClientId) {

    public AccessProperties {
        Objects.requireNonNull(publicBaseUrl, "verivoto.access.public-base-url must be set");
        zone = zone == null ? ZoneId.of("Europe/Madrid") : zone;
        mailFrom = mailFrom == null ? "no-reply@verivoto.local" : mailFrom;
        // RF-05b (D-48): Keycloak client of the voter PWA for the institutional account
        ssoClientId = ssoClientId == null || ssoClientId.isBlank() ? "verivoto-voter" : ssoClientId;
    }

    public AccessPolicy policy() {
        AccessPolicy defaults = AccessPolicy.DEFAULT;
        return new AccessPolicy(
                codeValidity == null ? defaults.codeValidity() : codeValidity,
                resendDelay == null ? defaults.resendDelay() : resendDelay,
                codeWindow == null ? defaults.codeWindow() : codeWindow,
                maxCodesPerWindow == null ? defaults.maxCodesPerWindow() : maxCodesPerWindow,
                maxFailedAttempts == null ? defaults.maxFailedAttempts() : maxFailedAttempts,
                lockDuration == null ? defaults.lockDuration() : lockDuration,
                votingTokenValidity == null ? defaults.votingTokenValidity() : votingTokenValidity,
                linkValidityAfterClose == null ? defaults.linkValidityAfterClose() : linkValidityAfterClose,
                resultsTokenValidity == null ? defaults.resultsTokenValidity() : resultsTokenValidity);
    }
}
