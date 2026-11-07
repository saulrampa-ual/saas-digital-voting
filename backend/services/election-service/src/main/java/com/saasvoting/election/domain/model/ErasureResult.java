package com.saasvoting.election.domain.model;

/** RF-18: census entries deleted (no process used them) and anonymized (a process used them). */
public record ErasureResult(int deleted, int anonymized, long firstVoterId) {
}
