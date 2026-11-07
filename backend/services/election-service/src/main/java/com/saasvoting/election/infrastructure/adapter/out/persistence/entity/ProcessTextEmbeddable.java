package com.saasvoting.election.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Title and description of a process in one language (table {@code process_texts}). */
@Embeddable
public class ProcessTextEmbeddable {

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    protected ProcessTextEmbeddable() {
        // Required by JPA.
    }

    ProcessTextEmbeddable(String title, String description) {
        this.title = title;
        this.description = description;
    }

    String title() {
        return title;
    }

    String description() {
        return description;
    }
}
