package com.bankdata.account.persistence;

import com.bankdata.account.application.AccountNotFoundException;
import com.bankdata.account.domain.AccountEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;

@ApplicationScoped //one entity for the whole application runtime - important. It will reuse in everywhere
public class AccountRepository implements PanacheRepository<AccountEntity> {

    //like @AutoWired in Spring. Scans for beans, makes dependency graph, sets correct dependencies.
    //EntityManager is not threadsafe, but Quarkus seems to proxy it and works correct in context scope query/transaction
    //Key is to use it inside @Transactional on a service level.
    @Inject
    EntityManager em;

    //this method returns AccountEntity and guarantees row block in DB for Pessimistic_Write in current Transaction.
    //the idea is to make read-edit-write transactions safe to overrun lost updates in concurrent queries
    public AccountEntity getForUpdate(String accountNumber) {
        try {
            //JPQL query. from AccountEntity a - refers to Entity not to the table itself.
            return em.createQuery("from AccountEntity a where a.accountNumber = :n",
                            AccountEntity.class
                    )
                    //setting parameter :n by the accountNumber. It is safer than strings concatenation
                    .setParameter("n", accountNumber)
                    //setting lock on a row. Pessimistic Write stands for
                    //1. In DB query takes row-level lock on a row he finds
                    //2. All other queries on THIS ROW will wait
                    //2. Lock will be on the row till the end of the transaction
                    //NOT TO FORGET: this method must be called from @Transactional method, to make it active
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    //waiting for a single result. If 0 = NoResultException, if >1 - NonUniqueResultException
                    .getSingleResult();

        } catch (NoResultException e) {
            throw new AccountNotFoundException(accountNumber);
        }
    }

    public AccountEntity getByAccountNumber(String accountNumber) {
        return find("accountNumber", accountNumber)
                .firstResultOptional()
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

}
