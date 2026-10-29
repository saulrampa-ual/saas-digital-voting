package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.CensusFrozenException;
import com.saasvoting.election.domain.model.CensusNotFoundException;
import com.saasvoting.election.domain.model.DuplicateReason;
import com.saasvoting.election.domain.model.DuplicateVoterException;
import com.saasvoting.election.domain.model.InvalidVoterDataException;
import com.saasvoting.election.domain.model.OperationNotAllowedException;
import com.saasvoting.election.domain.model.VoterNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;
import java.util.Map;

/** Maps errors to RFC 9457 problem details with plain Spanish messages (RNF-8). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CensusNotFoundException.class)
    ProblemDetail censusNotFound(CensusNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "No existe ese censo en tu organización.");
    }

    @ExceptionHandler(VoterNotFoundException.class)
    ProblemDetail voterNotFound(VoterNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "No existe esa persona en el censo.");
    }

    @ExceptionHandler(CensusFrozenException.class)
    ProblemDetail frozen(CensusFrozenException e) {
        return problem(HttpStatus.CONFLICT,
                "El censo no se puede modificar porque lo usa un proceso programado o abierto.");
    }

    @ExceptionHandler(DuplicateVoterException.class)
    ProblemDetail duplicate(DuplicateVoterException e) {
        return problem(HttpStatus.CONFLICT, e.reason() == DuplicateReason.EMAIL
                ? "Ya hay una persona con ese correo electrónico en el censo."
                : "Ya hay una persona con ese identificador institucional en el censo.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException e) {
        return problem(HttpStatus.CONFLICT,
                "Algún correo o identificador institucional ya existe en el censo. Revisa los datos e inténtalo de nuevo.");
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    ProblemDetail notAllowed(OperationNotAllowedException e) {
        return problem(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta operación.");
    }

    @ExceptionHandler(InvalidVoterDataException.class)
    ProblemDetail invalidVoter(InvalidVoterDataException e) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, e.userMessage());
        problem.setProperty("errors", List.of(Map.of("field", e.field(), "message", e.userMessage())));
        return problem;
    }

    @ExceptionHandler(VoterCsvReader.InvalidCsvException.class)
    ProblemDetail invalidCsv(VoterCsvReader.InvalidCsvException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tooLarge(MaxUploadSizeExceededException e) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "El fichero es demasiado grande (máximo 5 MB).");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidRequest(MethodArgumentNotValidException e) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Revisa los datos marcados y vuelve a intentarlo.");
        problem.setProperty("errors", e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Valor no válido" : error.getDefaultMessage()))
                .toList());
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class, IllegalArgumentException.class})
    ProblemDetail unreadable(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "La petición no tiene un formato válido.");
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
