package com.saasvoting.voteraccess.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saasvoting.voteraccess.application.port.in.VoterAccessUseCase.AccessGranted;
import com.saasvoting.voteraccess.application.port.out.Mailer;
import com.saasvoting.voteraccess.application.port.out.ResultsTokenRepository;
import com.saasvoting.voteraccess.application.port.out.RollRepository;
import com.saasvoting.voteraccess.application.port.out.SecretGenerator;
import com.saasvoting.voteraccess.application.port.out.SnapshotRepository;
import com.saasvoting.voteraccess.application.port.out.UnitOfWork;
import com.saasvoting.voteraccess.application.port.out.VoterIdentityVerifier;
import com.saasvoting.voteraccess.application.port.out.VoterIdentityVerifier.VerifiedIdentity;
import com.saasvoting.voteraccess.application.port.out.VotingTokenRepository;
import com.saasvoting.voteraccess.domain.model.AccessCredential;
import com.saasvoting.voteraccess.domain.model.AccessLockedException;
import com.saasvoting.voteraccess.domain.model.AccessPolicy;
import com.saasvoting.voteraccess.domain.model.AccountMismatchException;
import com.saasvoting.voteraccess.domain.model.InvalidIdentityTokenException;
import com.saasvoting.voteraccess.domain.model.Language;
import com.saasvoting.voteraccess.domain.model.Participation;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.ProcessStatus;
import com.saasvoting.voteraccess.domain.model.ResultsVisibility;
import com.saasvoting.voteraccess.domain.model.RollEntry;
import com.saasvoting.voteraccess.domain.model.VoterAuthentication;
import com.saasvoting.voteraccess.domain.model.VotingToken;
import com.saasvoting.voteraccess.domain.model.WrongAccessMethodException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * P7.1 (A5), RF-05b (D-48): the institutional account opens the ballot only when its verified e-mail is the e-mail of
 * the roll entry of the link (case and spaces aside); another account or an unverified e-mail counts as a failed
 * attempt, like a wrong code, and locks the access at the limit (RNF-14). Each process accepts only its own method.
 * Synthetic link, tokens and accounts only.
 */
class VoterAccessServiceSsoTest {

    private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");
    private static final long PROCESS_ID = 4;
    private static final String LINK = "S".repeat(64);

    private final UUID entryId = UUID.randomUUID();
    private final SnapshotRepository snapshots = mock(SnapshotRepository.class);
    private final RollRepository roll = mock(RollRepository.class);
    private final VotingTokenRepository tokens = mock(VotingTokenRepository.class);
    private final ResultsTokenRepository resultsTokens = mock(ResultsTokenRepository.class);
    private final SecretGenerator secrets = mock(SecretGenerator.class);
    private final Mailer mailer = mock(Mailer.class);
    private final VoterMessages messages = mock(VoterMessages.class);
    private final VoterIdentityVerifier identities = mock(VoterIdentityVerifier.class);
    private final UnitOfWork unitOfWork = new UnitOfWork() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };
    private final VoterAccessService service = new VoterAccessService(snapshots, roll, tokens, resultsTokens, secrets,
            mailer, messages, AccessPolicy.DEFAULT, unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC), identities);

    @BeforeEach
    void setUp() {
        when(secrets.hash(anyString())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0));
        when(secrets.newVotingToken()).thenReturn("synthetic-voting-token");
        AccessCredential credential = AccessCredential.empty(entryId).withLink("link-hash", NOW.plusSeconds(86_400));
        when(roll.findCredentialByLinkHash("hash:" + LINK)).thenReturn(Optional.of(credential));
        when(roll.lockCredential(entryId)).thenReturn(credential);
        when(roll.findEntry(entryId)).thenReturn(Optional.of(new RollEntry(entryId, PROCESS_ID, 31L,
                "Persona Sintética 1", "p01@verivoto.test", Language.ES)));
        when(roll.findParticipation(entryId)).thenReturn(Participation.notVoted(entryId));
        process(VoterAuthentication.INSTITUTIONAL_SSO);
    }

    @Test
    void theVerifiedAccountOfTheRollEntryGetsAVotingToken() {
        when(identities.verify("id-token")).thenReturn(new VerifiedIdentity("  P01@Verivoto.TEST ", true));

        AccessGranted access = service.verifyInstitutionalAccount(LINK, "id-token");

        assertEquals("synthetic-voting-token", access.votingToken());
        assertFalse(access.hasVoted());
        ArgumentCaptor<VotingToken> stored = ArgumentCaptor.forClass(VotingToken.class);
        verify(tokens).save(stored.capture());
        assertEquals("hash:synthetic-voting-token", stored.getValue().tokenHash()); // only the hash is stored
        verify(roll, never()).save(any(AccessCredential.class));
    }

    @Test
    void anotherAccountCountsAsAFailedAttempt() {
        when(identities.verify("id-token")).thenReturn(new VerifiedIdentity("otra@verivoto.test", true));

        AccountMismatchException error = assertThrows(AccountMismatchException.class,
                () -> service.verifyInstitutionalAccount(LINK, "id-token"));

        assertEquals(AccessPolicy.DEFAULT.maxFailedAttempts() - 1, error.remainingAttempts());
        ArgumentCaptor<AccessCredential> saved = ArgumentCaptor.forClass(AccessCredential.class);
        verify(roll).save(saved.capture());
        assertEquals(1, saved.getValue().failedAttempts());
        verify(tokens, never()).save(any(VotingToken.class));
    }

    @Test
    void anUnverifiedOrMissingEmailCountsAsAFailedAttempt() {
        when(identities.verify("unverified")).thenReturn(new VerifiedIdentity("p01@verivoto.test", false));
        when(identities.verify("no-email")).thenReturn(new VerifiedIdentity(null, true));

        assertThrows(AccountMismatchException.class, () -> service.verifyInstitutionalAccount(LINK, "unverified"));
        assertThrows(AccountMismatchException.class, () -> service.verifyInstitutionalAccount(LINK, "no-email"));
        verify(tokens, never()).save(any(VotingToken.class));
    }

    @Test
    void theLastFailedAttemptLocksTheAccess() {
        AccessCredential almost = new AccessCredential(entryId, "link-hash", NOW.plusSeconds(86_400), null, null, null,
                null, 0, AccessPolicy.DEFAULT.maxFailedAttempts() - 1, null);
        when(roll.lockCredential(entryId)).thenReturn(almost);
        when(identities.verify("id-token")).thenReturn(new VerifiedIdentity("otra@verivoto.test", true));

        AccessLockedException error = assertThrows(AccessLockedException.class,
                () -> service.verifyInstitutionalAccount(LINK, "id-token"));

        assertEquals(NOW.plus(AccessPolicy.DEFAULT.lockDuration()), error.lockedUntil());
    }

    @Test
    void anInvalidIdTokenIsRejectedBeforeTouchingTheRoll() {
        when(identities.verify("bad")).thenThrow(new InvalidIdentityTokenException("audience"));

        assertThrows(InvalidIdentityTokenException.class, () -> service.verifyInstitutionalAccount(LINK, "bad"));
        verify(roll, never()).lockCredential(any(UUID.class));
        verify(roll, never()).save(any(AccessCredential.class));
    }

    @Test
    void eachProcessAcceptsOnlyItsOwnAccessMethod() {
        when(identities.verify("id-token")).thenReturn(new VerifiedIdentity("p01@verivoto.test", true));
        WrongAccessMethodException code = assertThrows(WrongAccessMethodException.class,
                () -> service.verifyCode(LINK, "123456"));
        assertEquals(VoterAuthentication.INSTITUTIONAL_SSO, code.expected());
        assertThrows(WrongAccessMethodException.class, () -> service.sendCode(LINK));

        process(VoterAuthentication.EMAIL_CODE);
        WrongAccessMethodException sso = assertThrows(WrongAccessMethodException.class,
                () -> service.verifyInstitutionalAccount(LINK, "id-token"));
        assertEquals(VoterAuthentication.EMAIL_CODE, sso.expected());

        verify(tokens, never()).save(any(VotingToken.class));
        verify(roll, never()).save(any(AccessCredential.class));
    }

    private void process(VoterAuthentication authentication) {
        ProcessSnapshot snapshot = new ProcessSnapshot(PROCESS_ID, "orgA", Map.of(Language.ES, "Proceso sintético"),
                ProcessStatus.OPEN, NOW.minusSeconds(3600), NOW.plusSeconds(3600), Language.ES,
                ResultsVisibility.CENSUS_ONLY, authentication);
        when(snapshots.find(PROCESS_ID)).thenReturn(Optional.of(snapshot));
    }
}
