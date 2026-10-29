package com.saasvoting.election.domain.model;

/** The caller is not allowed to perform the operation. */
public class OperationNotAllowedException extends DomainException {

    public OperationNotAllowedException(String operation) {
        super("Operation not allowed: " + operation);
    }
}
