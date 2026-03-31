package com.devtrails.backend.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWorkerIdOrderByCreatedAtDesc(String workerId);
    List<WalletTransaction> findByWorkerIdAndCategoryOrderByCreatedAtDesc(String workerId, String category);
}
