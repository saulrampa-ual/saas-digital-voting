package com.saasvoting.election.application.port.in;

import com.saasvoting.election.domain.model.Census;

/** Census with the figures shown in the census screens (A2). */
public record CensusView(Census census, long voterCount, boolean frozen) {
}
