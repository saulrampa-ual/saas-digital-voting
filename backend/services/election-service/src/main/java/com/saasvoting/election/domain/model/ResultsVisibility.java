package com.saasvoting.election.domain.model;

/** Who can see the published result of a process (RF-13, D-16). The final hash is always public. */
public enum ResultsVisibility {
    /** Only voters of the census, after access with personal link and code. Default. */
    CENSUS_ONLY,
    /** Anyone with the public link, built from a random UUID. */
    PUBLIC_LINK
}
