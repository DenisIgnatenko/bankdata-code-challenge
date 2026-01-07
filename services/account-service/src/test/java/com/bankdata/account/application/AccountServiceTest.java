package com.bankdata.account.application;

import com.bankdata.account.api.dto.*;
import com.bankdata.account.api.error.BadRequestException;
import com.bankdata.account.domain.AccountEntity;
import com.bankdata.account.messaging.AccountEventPublisher;
import com.bankdata.account.persistence.AccountRepository;
import com.bankdata.account.support.AccountNumberGenerator;
import com.bankdata.contracts.events.AccountEvent;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import jakarta.persistence.PersistenceException;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class AccountServiceTest {
    @Mock
    AccountRepository repository;

    @Mock
    AccountNumberGenerator generator;

    @Mock
    AccountEventPublisher eventPublisher;

    @Captor
    ArgumentCaptor<AccountEvent> eventCaptor;

    AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(repository, generator, eventPublisher);
    }

    @Test
    void create_happy_persistsAndPublishedCreatedEvent() {
        when(generator.next()).thenReturn("0000000001");

        CreateAccountRequest request = new CreateAccountRequest(" Denis ", " Ignatenko ", new BigDecimal("0.00"));

        CreateAccountResponse response = service.create(request);

        assertEquals("0000000001", response.accountNumber());
        assertEquals(new BigDecimal("0.00"), response.balance());

        verify(repository, times(1)).persistAndFlush(any(AccountEntity.class));

        verify(eventPublisher, times(1)).safePublish(eventCaptor.capture());
        AccountEvent event = eventCaptor.getValue();

        assertEquals("0000000001", event.accountNumber());
        assertEquals("0.00", event.amount());
        assertEquals("0.00", event.balance());
        assertNotNull(event.eventId());
        assertNotNull(event.occurredAt());
    }

    @Test
    void create_firstNameBlank_doesNotPersistOrPublish() {
        CreateAccountRequest request = new CreateAccountRequest("   ", "Ivanov", new BigDecimal("0.00"));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(request));
        assertTrue(ex.getMessage().toLowerCase().contains("firstname"));
        assertTrue(ex.getMessage().toLowerCase().contains("blank"));

        verifyNoInteractions(repository);
        verifyNoInteractions(eventPublisher);
        verifyNoInteractions(generator);
    }

    @Test
    void create_retriesOnUniqueConstraintViolation_andSucceeds() {
        when(generator.next()).thenReturn("0000000001", "0000000002");

        PersistenceException uniqueViolation = new PersistenceException(
                new ConstraintViolationException("unique", new SQLException("dup"), "account_id")
        );

        doThrow(uniqueViolation)
                .doNothing()
                .when(repository).persistAndFlush(any(AccountEntity.class));

        CreateAccountRequest request = new CreateAccountRequest("Denis", "Ignatenko", new BigDecimal("1.00"));

        CreateAccountResponse response = service.create(request);

        assertEquals("0000000002", response.accountNumber());
        assertEquals(new BigDecimal("1.00"), response.balance());

        verify(repository, times(2)).persistAndFlush(any(AccountEntity.class));
        verify(eventPublisher, times(1)).safePublish(any(AccountEvent.class));
    }

    @Test
    void deposit_happy_updatesBalance_andPublishesEvent() {
        AccountEntity entity = new AccountEntity("0000000001", "Denis", "Ignatenko", new BigDecimal("10.00"));
        when(repository.getForUpdate("0000000001")).thenReturn(entity);

        DepositRequest request = new DepositRequest(new BigDecimal("5.00"));

        BalanceResponse response = service.deposit("0000000001", request);

        assertEquals("0000000001", response.accountNumber());
        assertEquals(new BigDecimal("15.00"), response.balance());

        verify(repository, times(1)).getForUpdate("0000000001");
        verify(eventPublisher, times(1)).safePublish(eventCaptor.capture());

        AccountEvent event = eventCaptor.getValue();
        assertEquals("0000000001", event.accountNumber());
        assertEquals("5.00", event.amount());
        assertEquals("15.00", event.balance());
    }

    @Test
    void deposit_invalidAmount_throwsBadRequest_andDoesNotLockOrPublish() {
        DepositRequest request = new DepositRequest(null);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.deposit("0000000001", request));
        assertTrue(exception.getMessage().toLowerCase().contains("amount is required"));

        verifyNoInteractions(repository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void transfer_sameAccount_doesNotLockOrPublish() {
        TransferRequest request = new TransferRequest("0000000001", "0000000001", new BigDecimal("1.00"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.transfer(request));
        assertTrue(exception.getMessage().toLowerCase().contains("must be different"));

        verifyNoInteractions(repository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void transfer_locksInStableOrder_preventsDeadlocks_andTransfersCorrectly() {
        String senderAccountNumber = "0000000002";
        String receiverAccountNumber = "0000000001"; // должен быть залочен первым

        AccountEntity receiver =
                new AccountEntity(receiverAccountNumber, "Alice", "Receiver", new BigDecimal("0.00"));
        AccountEntity sender =
                new AccountEntity(senderAccountNumber, "Bob", "Sender", new BigDecimal("100.00"));

        when(repository.getForUpdate(receiverAccountNumber)).thenReturn(receiver);
        when(repository.getForUpdate(senderAccountNumber)).thenReturn(sender);

        TransferRequest request =
                new TransferRequest(senderAccountNumber, receiverAccountNumber, new BigDecimal("10.00"));

        TransferResponse response = service.transfer(request);

        assertEquals(senderAccountNumber, response.fromAccountNumber());
        assertEquals(new BigDecimal("90.00"), response.fromBalance());
        assertEquals(receiverAccountNumber, response.toAccountNumber());
        assertEquals(new BigDecimal("10.00"), response.toBalance());

        InOrder inOrder = inOrder(repository);
        inOrder.verify(repository).getForUpdate(receiverAccountNumber);
        inOrder.verify(repository).getForUpdate(senderAccountNumber);

        verify(eventPublisher, times(1)).safePublish(eventCaptor.capture());
        AccountEvent event = eventCaptor.getValue();
        assertEquals(senderAccountNumber, event.fromAccountNumber());
        assertEquals(receiverAccountNumber, event.toAccountNumber());
        assertEquals("10.00", event.amount());
    }
}

