package com.saasvoting.identity.infrastructure.adapter.in.rest;

import com.saasvoting.identity.application.port.PageResult;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> items, int page, int size, long totalItems) {

    static <D, T> PageResponse<T> fromDomain(PageResult<D> result, Function<D, T> mapper) {
        return new PageResponse<>(
                result.items().stream().map(mapper).toList(), result.page(), result.size(), result.totalItems());
    }
}
