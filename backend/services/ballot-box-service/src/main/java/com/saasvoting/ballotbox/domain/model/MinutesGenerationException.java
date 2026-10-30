package com.saasvoting.ballotbox.domain.model;

/** The PDF could not be generated (RF-16 exception 3): nothing is stored and it can be asked again. */
public class MinutesGenerationException extends DomainException {

    public MinutesGenerationException(long processId, Throwable cause) {
        super("Minutes of process " + processId + " could not be generated", cause);
    }
}
