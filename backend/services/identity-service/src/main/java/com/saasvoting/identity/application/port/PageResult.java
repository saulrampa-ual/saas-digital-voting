package com.saasvoting.identity.application.port;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** One page of results, shared by inbound and outbound ports. */
public record PageResult<T>(List<T> items, int page, int size, long totalItems) {

    public PageResult {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalItems);
    }
}
