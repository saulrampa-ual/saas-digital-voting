package com.saasvoting.election.application.port;

import java.util.List;
import java.util.Objects;

/** One page of results, shared by inbound and outbound ports. */
public record PageResult<T>(List<T> items, int page, int size, long totalItems) {

    public PageResult {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
    }
}
