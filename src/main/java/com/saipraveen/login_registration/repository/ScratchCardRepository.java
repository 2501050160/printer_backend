package com.saipraveen.login_registration.repository;

import com.saipraveen.login_registration.entity.ScratchCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScratchCardRepository extends JpaRepository<ScratchCard, Long> {
    List<ScratchCard> findByUserId(Long userId);
    List<ScratchCard> findByUserIdAndIsScratched(Long userId, boolean isScratched);
    ScratchCard findByIdAndUserId(Long id, Long userId);
}
