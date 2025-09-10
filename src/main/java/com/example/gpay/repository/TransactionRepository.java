package com.example.gpay.repository;

import com.example.gpay.model.Transaction;
import com.example.gpay.model.TransactionType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Transaction> findByReference(String reference);
    Optional<Transaction> findByExternalReference(String externalReference);
    List<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, TransactionType type);
}