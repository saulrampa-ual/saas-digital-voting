package com.saasvoting.election.domain.model;

import java.util.List;

/** RF-07, exception 2: the process is incomplete and is not scheduled. Lists every problem found (RNF-8). */
public class ProcessIncompleteException extends DomainException {

    /** A missing or incoherent piece of the process, with the field and a plain Spanish message. */
    public record Issue(String field, String message) {
    }

    private final List<Issue> issues;

    public ProcessIncompleteException(List<Issue> issues) {
        super("Process incomplete: " + issues);
        this.issues = List.copyOf(issues);
    }

    public List<Issue> issues() {
        return issues;
    }
}
