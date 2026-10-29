package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.domain.model.EmailAlreadyInUseException;
import com.saasvoting.identity.domain.model.LastAdministratorException;
import com.saasvoting.identity.domain.model.ManagementUserNotFoundException;
import com.saasvoting.identity.domain.model.OperationNotAllowedException;
import com.saasvoting.identity.domain.model.OrganizationAlreadyExistsException;
import com.saasvoting.identity.domain.model.OrganizationNotFoundException;
import com.saasvoting.identity.infrastructure.adapter.out.identityprovider.IdentityProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

/** Maps errors to RFC 9457 problem details with plain Spanish messages (RNF-8). Replaces the old 409 catch-all (IMP-01). */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(OrganizationAlreadyExistsException.class)
    ProblemDetail organizationExists(OrganizationAlreadyExistsException e) {
        return problem(HttpStatus.CONFLICT, "Ya existe una organización con ese identificador.");
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    ProblemDetail emailInUse(EmailAlreadyInUseException e) {
        return problem(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo electrónico.");
    }

    @ExceptionHandler(LastAdministratorException.class)
    ProblemDetail lastAdministrator(LastAdministratorException e) {
        return problem(HttpStatus.CONFLICT, "La organización debe tener al menos un administrador.");
    }

    @ExceptionHandler(OrganizationNotFoundException.class)
    ProblemDetail organizationNotFound(OrganizationNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "No existe esa organización.");
    }

    @ExceptionHandler(ManagementUserNotFoundException.class)
    ProblemDetail userNotFound(ManagementUserNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "No existe ese usuario en la organización.");
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    ProblemDetail notAllowed(OperationNotAllowedException e) {
        return problem(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta operación.");
    }

    @ExceptionHandler(IdentityProviderUnavailableException.class)
    ProblemDetail identityProviderUnavailable(IdentityProviderUnavailableException e) {
        log.error("Identity provider error", e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE,
                "No se ha podido contactar con el servicio de identidad. Inténtalo de nuevo más tarde.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidRequest(MethodArgumentNotValidException e) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Revisa los datos marcados y vuelve a intentarlo.");
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Valor no válido" : error.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", errors);
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
