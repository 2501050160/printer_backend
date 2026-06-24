package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.saipraveen.login_registration.entity.UserRewardClaim;

public interface UserRewardClaimRepository extends JpaRepository<UserRewardClaim, Long> {
    boolean existsByUserIdAndRewardId(Long userId, Long rewardId);
}
