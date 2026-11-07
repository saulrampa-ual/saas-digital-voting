package com.saasvoting.election.infrastructure.adapter.in.rest;

import com.saasvoting.election.domain.model.CensusFrozenException;
import com.saasvoting.election.domain.model.ErasureBlockedException;
import com.saasvoting.election.domain.model.PrivacyServiceUnavailableException;
import com.saasvoting.election.domain.model.PrivacySubjectNotFoundException;
import com.saasvoting.election.domain.model.VoterAnonymizedException;
import com.saasvoting.election.domain.model.CensusNotFoundException;
import com.saasvoting.election.domain.model.DuplicateReason;
import com.saasvoting.election.domain.model.DuplicateVoterException;
import com.saasvoting.election.domain.model.InvalidProcessDataException;
import com.saasvoting.election.domain.model.InvalidTransitionException;
import com.saasvoting.election.domain.model.InvalidVoterDataException;
import com.saasvoting.election.domain.model.OperationNotAllowedException;
import com.saasvoting.election.domain.model.ProcessIncompleteException;
import com.saasvoting.election.domain.model.ProcessNotEditableException;
import com.saasvoting.election.domain.model.ProcessNotFoundException;
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

    @ExceptionHandler(PrivacySubjectNotFoundException.class)
    ProblemDetail privacySubjectNotFound(PrivacySubjectNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND,
                "No hay ninguna persona con ese correo o teléfono en los censos de tu organización.");
    }

    @ExceptionHandler(ErasureBlockedException.class)
    ProblemDetail erasureBlocked(ErasureBlockedException e) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Esta persona está en el censo de un proceso programado "
                + "o abierto. Podrás borrar sus datos cuando termine la votación.");
        problem.setProperty("processIds", e.processIds());
        return problem;
    }

    @ExceptionHandler(VoterAnonymizedException.class)
    ProblemDetail voterAnonymized(VoterAnonymizedException e) {
        return problem(HttpStatus.CONFLICT, "Los datos de esta persona se borraron por una solicitud de privacidad "
                + "y no se pueden editar.");
    }

    @ExceptionHandler(PrivacyServiceUnavailableException.class)
    ProblemDetail privacyUnavailable(PrivacyServiceUnavailableException e) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE,
                "No se ha podido reunir todos los datos de la persona. Inténtalo de nuevo en unos minutos.");
    }

    @ExceptionHandler(ProcessNotFoundException.class)
    ProblemDetail processNotFound(ProcessNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "No existe ese proceso en tu organización.");
    }

    @ExceptionHandler(ProcessNotEditableException.class)
    ProblemDetail processNotEditable(ProcessNotEditableException e) {
        return problem(HttpStatus.CONFLICT,
                "Solo se puede cambiar la configuración de un proceso en borrador.");
    }

    @ExceptionHandler(ProcessIncompleteException.class)
    ProblemDetail processIncomplete(ProcessIncompleteException e) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT,
                "El proceso no se puede programar todavía. Completa lo que falta y vuelve a intentarlo.");
        problem.setProperty("errors", e.issues().stream()
                .map(issue -> Map.of("field", issue.field(), "message", issue.message()))
                .toList());
        return problem;
    }

    @ExceptionHandler(InvalidTransitionException.class)
    ProblemDetail invalidTransition(InvalidTransitionException e) {
        String detail = switch (e.to()) {
            case SCHEDULED -> "Solo se puede programar un proceso en borrador.";
            case ARCHIVED -> "Solo se puede archivar un proceso escrutado.";
            case TALLIED -> "Solo se puede escrutar un proceso cerrado.";
            default -> "El proceso ha cambiado de estado; recarga la página.";
        };
        ProblemDetail problem = problem(HttpStatus.CONFLICT, detail);
        problem.setProperty("currentStatus", e.from().name());
        return problem;
    }

    @ExceptionHandler(InvalidProcessDataException.class)
    ProblemDetail invalidProcess(InvalidProcessDataException e) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, e.userMessage());
        problem.setProperty("errors", List.of(Map.of("field", e.field(), "message", e.userMessage())));
        return problem;
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
