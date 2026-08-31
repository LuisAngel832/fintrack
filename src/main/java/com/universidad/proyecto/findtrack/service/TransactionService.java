package com.universidad.proyecto.findtrack.service;

import java.util.Map;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.function.Function;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.universidad.proyecto.findtrack.dto.CategorySpent;
import com.universidad.proyecto.findtrack.dto.request.TransactionRequestDTO;
import com.universidad.proyecto.findtrack.dto.response.CategoryResponseDTO;
import com.universidad.proyecto.findtrack.dto.response.TransactionResponseDTO;
import com.universidad.proyecto.findtrack.exceptions.ResourceNotFoundException;
import com.universidad.proyecto.findtrack.model.Category;
import com.universidad.proyecto.findtrack.model.Transaction;
import com.universidad.proyecto.findtrack.model.TransactionType;
import com.universidad.proyecto.findtrack.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final CategoryService categoryService;

    private final TransactionRepository transactionRepository;

    public TransactionResponseDTO createTransaction(TransactionRequestDTO transactionRequest, UUID userId) {
        Category category = categoryService.getUsableCategory(transactionRequest.getCategoryId(), userId);

        TransactionType transactionType = TransactionType.valueOf(transactionRequest.getType());

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .categoryId(category.getId())
                .amount(transactionRequest.getAmount())
                .type(transactionType)
                .description(transactionRequest.getDescription())
                .date(transactionRequest.getDate())
                .build();

        Transaction transactionSaved = transactionRepository.save(transaction);

        CategoryResponseDTO categoryResponse = new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getType(),
                category.isDefault());

        return new TransactionResponseDTO(
                transactionSaved.getId(),
                transactionSaved.getAmount(),
                transactionSaved.getType(),
                transactionSaved.getDescription(),
                transactionSaved.getDate(),
                transactionSaved.getCreatedAt(),
                categoryResponse);
    }

    @Transactional
    public TransactionResponseDTO updateTransaction(TransactionRequestDTO transactionRequest, UUID transactionId,
            UUID userId) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción no encontrada"));

        if (!userId.equals(transaction.getUserId())) {
            throw new AccessDeniedException("No tiene permiso para actualizar esta transacción");
        } 

        Category category = categoryService.getUsableCategory(transactionRequest.getCategoryId(), userId);

        TransactionType transactionType = TransactionType.valueOf(transactionRequest.getType());

        transaction.update(
                category.getId(),
                transactionRequest.getAmount(),
                transactionType,
                transactionRequest.getDescription(),
                transactionRequest.getDate());

        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getDate(),
                transaction.getCreatedAt(),
                new CategoryResponseDTO(
                        category.getId(),
                        category.getName(),
                        category.getIcon(),
                        category.getType(),
                        category.isDefault()));

    }

    public List<TransactionResponseDTO> getTransactions(UUID userId, TransactionType type, UUID categoryId,
            Integer year, Integer month) {
        List<Transaction> transactions = transactionRepository.findWithFilters(userId, type, categoryId, year, month);

        List<UUID> categoryIds = transactions.stream()
                .map(Transaction::getCategoryId)
                .distinct()
                .toList();

        List<Category> categories = categoryService.findCategoriesByIds(categoryIds);

        Map<UUID, Category> categoriesById = categories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        return transactions.stream()
                .map(transaction -> {
                    Category category = categoriesById.get(transaction.getCategoryId());

                    CategoryResponseDTO categoryResponse = new CategoryResponseDTO(
                            category.getId(),
                            category.getName(),
                            category.getIcon(),
                            category.getType(),
                            category.isDefault());

                    return new TransactionResponseDTO(
                            transaction.getId(),
                            transaction.getAmount(),
                            transaction.getType(),
                            transaction.getDescription(),
                            transaction.getDate(),
                            transaction.getCreatedAt(),
                            categoryResponse);
                })
                .toList();
    }

    @Transactional
    public void deleteTransaction(UUID transactionId, UUID userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transacción no encontrada"));

        if (!userId.equals(transaction.getUserId())) {
            throw new AccessDeniedException("No tiene permiso para eliminar esta transacción");
        }

        transactionRepository.delete(transaction);
    }

    public List<CategorySpent> getSpentByCategory(UUID userId, Integer month, Integer year) {
        LocalDate inicioMes = LocalDate.of(year, month, 1);
        LocalDate inicioMesSiguiente = inicioMes.plusMonths(1);

        return transactionRepository.findSpentByMonth(userId, inicioMes, inicioMesSiguiente);
    }

}
