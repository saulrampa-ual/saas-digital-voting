package com.saasvoting.voteraccess.infrastructure.adapter.in.rest;

import com.saasvoting.voteraccess.domain.model.AccessLockedException;
import com.saasvoting.voteraccess.domain.model.CodeExpiredException;
import com.saasvoting.voteraccess.domain.model.CodeRequestTooSoonException;
import com.saasvoting.voteraccess.domain.model.LinkExpiredException;
import com.saasvoting.voteraccess.domain.model.LinkNotFoundException;
import com.saasvoting.voteraccess.domain.model.MessageDeliveryException;
import com.saasvoting.voteraccess.domain.model.ProcessNotOpenException;
import com.saasvoting.voteraccess.domain.model.ResultsTokenExpiredException;
import com.saasvoting.voteraccess.domain.model.ResultsTokenNotFoundException;
import com.saasvoting.voteraccess.domain.model.SnapshotConflictException;
import com.saasvoting.voteraccess.domain.model.SnapshotNotFoundException;
import com.saasvoting.voteraccess.domain.model.VotingTokenExpiredException;
import com.saasvoting.voteraccess.domain.model.VotingTokenNotFoundException;
import com.saasvoting.voteraccess.domain.model.VotingTokenUsedException;
import com.saasvoting.voteraccess.domain.model.WrongCodeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * RFC 9457 problem details with plain Spanish messages for voters (RNF-6, RNF-8). An invalid link always gives the
 * same 404, without saying whether it never existed (RF-08).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(LinkNotFoundException.class)
    ProblemDetail linkNotFound(LinkNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "El enlace no es válido. Usa el enlace de tu último correo de invitación.");
    }

    @ExceptionHandler(LinkExpiredException.class)
    ProblemDetail linkExpired(LinkExpiredException e) {
        return problem(HttpStatus.GONE, "El enlace ha caducado.");
    }

    @ExceptionHandler(ProcessNotOpenException.class)
    ProblemDetail notOpen(ProcessNotOpenException e) {
        String detail = switch (e.status()) {
            case SCHEDULED -> "La votación todavía no está abierta.";
            case OPEN -> "La votación está abierta.";
            default -> "La votación ya está cerrada.";
        };
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, detail);
        problem.setProperty("processStatus", e.status().name());
        return problem;
    }

    @ExceptionHandler(AccessLockedException.class)
    ProblemDetail locked(AccessLockedException e) {
        ProblemDetail problem = problem(HttpStatus.LOCKED,
                "Has superado el número de intentos. Por seguridad, espera unos minutos y vuelve a intentarlo.");
        problem.setProperty("lockedUntil", e.lockedUntil());
        return problem;
    }

    @ExceptionHandler(CodeRequestTooSoonException.class)
    ProblemDetail tooSoon(CodeRequestTooSoonException e) {
        ProblemDetail problem = problem(HttpStatus.TOO_MANY_REQUESTS,
                "Espera un poco antes de pedir otro código.");
        problem.setProperty("resendAvailableAt", e.availableAt());
        return problem;
    }

    @ExceptionHandler(CodeExpiredException.class)
    ProblemDetail codeExpired(CodeExpiredException e) {
        return problem(HttpStatus.GONE, "El código ha caducado o ya se ha usado. Pide uno nuevo.");
    }

    @ExceptionHandler(WrongCodeException.class)
    ProblemDetail wrongCode(WrongCodeException e) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "El código no es correcto.");
        problem.setProperty("remainingAttempts", e.remainingAttempts());
        return problem;
    }

    @ExceptionHandler(MessageDeliveryException.class)
    ProblemDetail delivery(MessageDeliveryException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE,
                "No hemos podido enviar el correo. Inténtalo de nuevo en unos minutos.");
    }

    @ExceptionHandler(SnapshotNotFoundException.class)
    ProblemDetail snapshotNotFound(SnapshotNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Proceso desconocido.");
    }

    @ExceptionHandler(SnapshotConflictException.class)
    ProblemDetail snapshotConflict(SnapshotConflictException e) {
        return problem(HttpStatus.CONFLICT, "El censo del proceso ya no se puede cambiar.");
    }

    @ExceptionHandler(VotingTokenNotFoundException.class)
    ProblemDetail tokenNotFound(VotingTokenNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Token de voto desconocido.");
    }

    @ExceptionHandler(VotingTokenUsedException.class)
    ProblemDetail tokenUsed(VotingTokenUsedException e) {
        return problem(HttpStatus.CONFLICT, "Ya has votado en este proceso.");
    }

    @ExceptionHandler(VotingTokenExpiredException.class)
    ProblemDetail tokenExpired(VotingTokenExpiredException e) {
        return problem(HttpStatus.GONE, "Tu sesión de voto ha caducado. Vuelve a entrar con tu enlace.");
    }

    @ExceptionHandler(ResultsTokenNotFoundException.class)
    ProblemDetail resultsTokenNotFound(ResultsTokenNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Acceso a los resultados desconocido.");
    }

    @ExceptionHandler(ResultsTokenExpiredException.class)
    ProblemDetail resultsTokenExpired(ResultsTokenExpiredException e) {
        return problem(HttpStatus.GONE, "Tu acceso a los resultados ha caducado. Vuelve a entrar con tu enlace.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException e) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Revisa los datos y vuelve a intentarlo.");
        problem.setProperty("errors", e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Valor no válido" : error.getDefaultMessage()))
                .toList());
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    ProblemDetail unreadable(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "La petición no tiene un formato válido.");
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
