package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity;

import com.saasvoting.ballotbox.domain.model.BallotOption;
import com.saasvoting.ballotbox.domain.model.Language;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Option with the identifier of election-service (it travels in the vote and in the tally). */
@Entity
@Table(name = "ballot_options")
public class BallotOptionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "position", nullable = false)
    private int position;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ballot_option_texts", joinColumns = @JoinColumn(name = "option_id"))
    @MapKeyColumn(name = "language", length = 2)
    @Column(name = "text", length = 200, nullable = false)
    private Map<String, String> texts = new HashMap<>();

    protected BallotOptionJpaEntity() {
        // Required by JPA.
    }

    BallotOptionJpaEntity(Long id, int position, Map<String, String> texts) {
        this.id = id;
        this.position = position;
        this.texts = new HashMap<>(texts);
    }

    Long id() {
        return id;
    }

    BallotOption toDomain() {
        Map<Language, String> text = new EnumMap<>(Language.class);
        texts.forEach((code, value) -> text.put(Language.fromCode(code), value));
        return new BallotOption(id, position, text);
    }
}
