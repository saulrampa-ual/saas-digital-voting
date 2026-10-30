package com.saasvoting.ballotbox.domain.model;

/** What the voter chose: one option or a blank vote (RF-09). Exactly one of both. */
public record Selection(Long optionId, boolean blank) {

    public Selection {
        if ((optionId == null) == !blank) {
            throw new InvalidSelectionException();
        }
    }

    public static Selection option(long optionId) {
        return new Selection(optionId, false);
    }

    public static Selection blankVote() {
        return new Selection(null, true);
    }
}
