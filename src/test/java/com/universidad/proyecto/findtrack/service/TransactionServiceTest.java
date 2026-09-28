package com.universidad.proyecto.findtrack.service;

import com.universidad.proyecto.findtrack.exceptions.ResourceNotFoundException;
import com.universidad.proyecto.findtrack.model.Transaction;
import com.universidad.proyecto.findtrack.model.TransactionType;
import com.universidad.proyecto.findtrack.repository.TransactionRepository;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private CategoryService categoryService;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void deleteTransaction_cuandoLaTransaccionNoExiste_lanzaResourceNotFound() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.deleteTransaction(transactionId, userId));

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void deleteTransaction_cuandoElUsuarioNoEsElPropietario_lanzaAccessDenied() {
        UUID transactionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .userId(ownerId)
                .categoryId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.EXPENSE)
                .date(LocalDate.now())
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        assertThrows(AccessDeniedException.class,
                () -> transactionService.deleteTransaction(transactionId, otherUserId));

        verify(transactionRepository, never()).delete(any(Transaction.class));

    }

    @Test
    void deleteTransaction_cuandoElUsuarioEsElPropietario_invocaDeleteConLaEntidad() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .userId(userId)
                .categoryId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.EXPENSE)
                .date(LocalDate.now())
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(transactionId, userId);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).delete(captor.capture());

        assertEquals(transaction, captor.getValue());
    }

}
