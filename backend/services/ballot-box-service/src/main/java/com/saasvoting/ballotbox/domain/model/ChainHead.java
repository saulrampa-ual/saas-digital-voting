package com.saasvoting.ballotbox.domain.model;

/** Last event of the chain of a process: the next one gets {@code sequence + 1} and links to {@code eventHash}. */
public record ChainHead(long processId, long sequence, String eventHash) {

    public static final String GENESIS_HASH = "0".repeat(64);

    public static ChainHead empty(long processId) {
        return new ChainHead(processId, 0, GENESIS_HASH);
    }
}
