package com.saasvoting.ballotbox.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/** RI-8 with D-18: only the hash of the receipt code; no time and no link to the vote. */
@Entity
@Table(name = "receipts")
public class ReceiptJpaEntity {

    @Embeddable
    public static class Key implements Serializable {

        private static final long serialVersionUID = 1L;

        @Column(name = "process_id", nullable = false, updatable = false)
        private long processId;

        @Column(name = "receipt_hash", length = 64, nullable = false, updatable = false)
        private String receiptHash;

        protected Key() {
            // Required by JPA.
        }

        public Key(long processId, String receiptHash) {
            this.processId = processId;
            this.receiptHash = receiptHash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && key.processId == processId && key.receiptHash.equals(receiptHash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(processId, receiptHash);
        }
    }

    @EmbeddedId
    private Key key;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    protected ReceiptJpaEntity() {
        // Required by JPA.
    }

    public ReceiptJpaEntity(long processId, String receiptHash) {
        this.key = new Key(processId, receiptHash);
        this.status = "VALID";
    }
}
