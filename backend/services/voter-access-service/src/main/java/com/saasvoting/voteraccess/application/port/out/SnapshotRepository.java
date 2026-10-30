package com.saasvoting.voteraccess.application.port.out;

import com.saasvoting.voteraccess.domain.model.ProcessSnapshot;

import java.util.Optional;

public interface SnapshotRepository {

    Optional<ProcessSnapshot> find(long processId);

    ProcessSnapshot save(ProcessSnapshot snapshot);
}
