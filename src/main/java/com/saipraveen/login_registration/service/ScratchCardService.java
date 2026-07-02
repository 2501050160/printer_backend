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
        double maxPercent = systemSettingService.getSettingDouble("scratch_card_max_percent", 10.0);
        double maxWin = transactionAmount * (maxPercent / 100.0);
        
        // Generate random win amount up to maxWin, rounded to 2 decimal places
        double winAmount = 0.0;
        if (maxWin > 0.1) {
            winAmount = 0.1 + (maxWin - 0.1) * new Random().nextDouble();
            winAmount = Math.round(winAmount * 100.0) / 100.0;
        }

        ScratchCard card = new ScratchCard();
        card.setUserId(userId);
        card.setOrderId(orderId);
        card.setTransactionAmount(transactionAmount);
        card.setMaxWinAmount(Math.round(maxWin * 100.0) / 100.0);
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
