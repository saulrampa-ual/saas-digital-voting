package com.saasvoting.election.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;

/** Option of a ballot (table {@code ballot_options}) with its texts per language ({@code ballot_option_texts}). */
@Entity
@Table(name = "ballot_options")
public class BallotOptionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "process_id", nullable = false, updatable = false)
    private ProcessJpaEntity process;

    @Column(name = "position", nullable = false)
    private int position;

    @ElementCollection
    @CollectionTable(name = "ballot_option_texts", joinColumns = @JoinColumn(name = "option_id"))
    @MapKeyColumn(name = "language", length = 2)
    @Column(name = "text", length = 200, nullable = false)
    private Map<String, String> texts = new HashMap<>();

    protected BallotOptionJpaEntity() {
        // Required by JPA.
    }

    BallotOptionJpaEntity(ProcessJpaEntity process, int position, Map<String, String> texts) {
        this.process = process;
        this.position = position;
        this.texts = new HashMap<>(texts);
    }

    Long id() {
        return id;
    }

    int position() {
        return position;
    }

    Map<String, String> texts() {
        return texts;
    }
}
