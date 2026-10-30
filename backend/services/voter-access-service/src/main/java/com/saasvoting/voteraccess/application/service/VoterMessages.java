package com.saasvoting.voteraccess.application.service;

import com.saasvoting.voteraccess.application.port.out.Mailer.MailMessage;
import com.saasvoting.voteraccess.domain.model.Language;
import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;
import com.saasvoting.voteraccess.domain.model.RollEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;

/**
 * Texts of the emails to voters in their language (RF-20), with plain words (RNF-6). They never contain the chosen
 * option (RNF-11). Dates are shown in the organization's time zone.
 */
public final class VoterMessages {

    private final String publicBaseUrl;
    private final ZoneId zone;

    public VoterMessages(String publicBaseUrl, ZoneId zone) {
        String url = Objects.requireNonNull(publicBaseUrl, "publicBaseUrl must not be null");
        this.publicBaseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
    }

    public String personalLink(String linkToken) {
        return publicBaseUrl + "/votar/" + linkToken;
    }

    public MailMessage invitation(RollEntry entry, ProcessSnapshot process, String linkToken) {
        String title = process.titleIn(entry.language());
        String link = personalLink(linkToken);
        if (entry.language() == Language.EN) {
            return new MailMessage(entry.email(), "Your link to vote: " + title, String.join("\n",
                    "Hello " + entry.displayName() + ",",
                    "",
                    "You are on the voter list of \"" + title + "\".",
                    "Voting opens on " + format(process.opensAt(), Locale.ENGLISH)
                            + " and closes on " + format(process.closesAt(), Locale.ENGLISH) + ".",
                    "",
                    "This is your personal link. When you open it we will email you a 6-digit code:",
                    link,
                    "",
                    "Do not share this link. Each person can vote only once and your vote is secret."));
        }
        return new MailMessage(entry.email(), "Tu enlace para votar: " + title, String.join("\n",
                "Hola, " + entry.displayName() + ":",
                "",
                "Estás en el censo de «" + title + "».",
                "La votación se abre el " + format(process.opensAt(), Locale.forLanguageTag("es"))
                        + " y se cierra el " + format(process.closesAt(), Locale.forLanguageTag("es")) + ".",
                "",
                "Este es tu enlace personal. Al abrirlo te enviaremos por correo un código de 6 cifras:",
                link,
                "",
                "No compartas este enlace. Cada persona solo puede votar una vez y tu voto es secreto."));
    }

    /** The code to vote or, after the tally, to see the result (D-41). */
    public MailMessage accessCode(RollEntry entry, ProcessSnapshot process, String code, long validMinutes,
            boolean forResults) {
        String title = process.titleIn(entry.language());
        if (entry.language() == Language.EN) {
            return new MailMessage(entry.email(), "Your access code for " + title, String.join("\n",
                    (forResults ? "Your code to see the result of \"" : "Your code to vote in \"") + title + "\" is:",
                    "",
                    code,
                    "",
                    "It expires in " + validMinutes + " minutes. If you did not request it, ignore this message."));
        }
        return new MailMessage(entry.email(), "Tu código de acceso a " + title, String.join("\n",
                (forResults ? "Tu código para ver el resultado de «" : "Tu código para votar en «") + title + "» es:",
                "",
                code,
                "",
                "Caduca en " + validMinutes + " minutos. Si no lo has pedido, ignora este mensaje."));
    }

    private String format(Instant instant, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(zone)
                .format(instant);
    }
}
