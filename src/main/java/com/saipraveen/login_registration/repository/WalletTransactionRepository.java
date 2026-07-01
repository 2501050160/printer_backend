package com.saipraveen.login_registration.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.saipraveen.login_registration.entity.WalletTransaction;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserIdOrderByTimestampDesc(Long userId);
}
