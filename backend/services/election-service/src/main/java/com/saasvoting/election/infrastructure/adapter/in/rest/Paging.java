package com.saasvoting.election.infrastructure.adapter.in.rest;

/** Normalizes page parameters: page &gt;= 0 and 1 &lt;= size &lt;= 100. */
record Paging(int page, int size) {

    static Paging of(Integer page, Integer size) {
        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null ? 20 : Math.min(100, Math.max(1, size));
        return new Paging(safePage, safeSize);
    }
}
