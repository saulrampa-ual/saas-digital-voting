package com.saasvoting.election.infrastructure.adapter.out.persistence;

import com.saasvoting.election.application.port.out.UnitOfWork;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** One database transaction per unit of work; the adapters called inside join it. */
@Component
public class TransactionalUnitOfWork implements UnitOfWork {

    private final TransactionTemplate transactions;

    public TransactionalUnitOfWork(PlatformTransactionManager transactionManager) {
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return transactions.execute(status -> work.get());
    }
}
