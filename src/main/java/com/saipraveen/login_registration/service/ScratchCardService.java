package com.saipraveen.login_registration.service;

import com.saipraveen.login_registration.entity.ScratchCard;
import com.saipraveen.login_registration.repository.ScratchCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class ScratchCardService {

    @Autowired
    private ScratchCardRepository repository;

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private UserService userService;

    public ScratchCard createScratchCard(Long userId, String orderId, Double transactionAmount) {
        double minReward = systemSettingService.getSettingDouble("scratch_card_min_reward", 1.0);
        double maxReward = systemSettingService.getSettingDouble("scratch_card_max_reward", 10.0);

        if (minReward > maxReward) {
            double temp = minReward;
            minReward = maxReward;
            maxReward = temp;
        }

        // Generate random win amount between minReward and maxReward
        double winAmount = minReward;
        if (maxReward > minReward) {
            winAmount = minReward + (maxReward - minReward) * new Random().nextDouble();
        }
        winAmount = Math.round(winAmount * 100.0) / 100.0;

        ScratchCard card = new ScratchCard();
        card.setUserId(userId);
        card.setOrderId(orderId);
        card.setTransactionAmount(transactionAmount);
        card.setMaxWinAmount(maxReward);
        card.setWinAmount(winAmount);
        card.setScratched(false);
        
        return repository.save(card);
    }

    public List<ScratchCard> getUserCards(Long userId) {
        return repository.findByUserId(userId);
    }

    public ScratchCard scratchCard(Long id, Long userId) {
        ScratchCard card = repository.findByIdAndUserId(id, userId);
        if (card == null) {
            throw new RuntimeException("Scratch card not found");
        }
        if (card.isScratched()) {
            throw new RuntimeException("Scratch card already scratched");
        }

        card.setScratched(true);
        card.setScratchedAt(LocalDateTime.now());
        ScratchCard saved = repository.save(card);

        // Credit wallet with the win amount!
        if (saved.getWinAmount() > 0) {
            userService.creditWallet(
                userId, 
                saved.getWinAmount(), 
                "REWARD", 
                "Scratch Card Reward for order: " + card.getOrderId()
            );
        }

        return saved;
    }
}
