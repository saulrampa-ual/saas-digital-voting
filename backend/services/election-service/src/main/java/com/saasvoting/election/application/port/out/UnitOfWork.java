package com.saasvoting.election.application.port.out;

import java.util.function.Supplier;

/** Runs several port calls as one atomic unit (a database transaction in the infrastructure). */
public interface UnitOfWork {

    <T> T inTransaction(Supplier<T> work);
}
