package com.saasvoting.election.infrastructure.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CensusRequest(@NotBlank @Size(max = 100) String name) {
}
