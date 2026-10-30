package com.saasvoting.ballotbox.infrastructure.adapter.in.rest;

import com.saasvoting.ballotbox.domain.model.BallotNotFoundException;
import com.saasvoting.ballotbox.domain.model.InvalidSelectionException;
import com.saasvoting.ballotbox.domain.model.SnapshotConflictException;
import com.saasvoting.ballotbox.domain.model.VoteNotRecordedException;
import com.saasvoting.ballotbox.domain.model.VotingClosedException;
import com.saasvoting.ballotbox.domain.model.VotingTokenRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** RFC 9457 problem details with plain Spanish messages for voters (RNF-6, RNF-8). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BallotNotFoundException.class)
    ProblemDetail ballotNotFound(BallotNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "No existe esa votación.");
    }

    @ExceptionHandler(InvalidSelectionException.class)
    ProblemDetail invalidSelection(InvalidSelectionException e) {
        return problem(HttpStatus.BAD_REQUEST, "Elige una de las opciones de la papeleta.");
    }

    @ExceptionHandler(VotingClosedException.class)
    ProblemDetail closed(VotingClosedException e) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, switch (e.status()) {
            case SCHEDULED -> "La votación todavía no está abierta.";
            default -> "La votación ya está cerrada. Tu voto no se ha registrado.";
        });
        problem.setProperty("processStatus", e.status().name());
        return problem;
    }

    @ExceptionHandler(VotingTokenRejectedException.class)
    ProblemDetail tokenRejected(VotingTokenRejectedException e) {
        return switch (e.reason()) {
            case UNKNOWN -> problem(HttpStatus.NOT_FOUND, "Tu sesión de voto no es válida. Vuelve a entrar con tu enlace.");
            case ALREADY_USED -> problem(HttpStatus.CONFLICT, "Ya has votado en este proceso.");
            case EXPIRED -> problem(HttpStatus.GONE, "Tu sesión de voto ha caducado. Vuelve a entrar con tu enlace.");
            case PROCESS_NOT_OPEN -> problem(HttpStatus.UNPROCESSABLE_CONTENT, "La votación no está abierta.");
        };
    }

    @ExceptionHandler(VoteNotRecordedException.class)
    ProblemDetail notRecorded(VoteNotRecordedException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE,
                "Tu voto no se ha registrado. Puedes intentarlo de nuevo sin riesgo de votar dos veces.");
    }

    @ExceptionHandler(SnapshotConflictException.class)
    ProblemDetail snapshotConflict(SnapshotConflictException e) {
        return problem(HttpStatus.CONFLICT, "La papeleta ya no se puede cambiar.");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    ProblemDetail invalid(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "La petición no tiene un formato válido.");
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
