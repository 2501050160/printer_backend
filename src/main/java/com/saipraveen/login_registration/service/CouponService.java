package com.saipraveen.login_registration.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.saipraveen.login_registration.entity.Coupon;
import com.saipraveen.login_registration.repository.CouponRepository;

@Service
public class CouponService {

    @Autowired
    private CouponRepository repository;

    public Coupon createCoupon(
            Coupon coupon) {

        if (coupon.getDiscountPercentage() > 95) {
            throw new RuntimeException("Maximum allowed discount limit is 95%");
        }

        coupon.setUsedCount(0);
        coupon.setActive(true);

        return repository.save(
                coupon
        );
    }

    @org.springframework.transaction.annotation.Transactional
    public List<Coupon> getAllCoupons() {
        autoDeleteInvalidCoupons();
        return repository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public Coupon validateCoupon(
            String couponCode) {

        Coupon coupon =
                repository.findByCouponCode(
                        couponCode
                );

        if (coupon == null) {
            throw new RuntimeException(
                    "Coupon Not Found"
            );
        }

        if (!coupon.getActive()) {
            throw new RuntimeException(
                    "Coupon Disabled"
            );
        }

        boolean expired = coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now());
        boolean fullyUsed = coupon.getUsedCount() != null && coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses();

        if (expired || fullyUsed) {
            repository.delete(coupon);
            throw new RuntimeException(expired ? "Coupon Expired" : "Coupon Usage Limit Reached");
        }

        return coupon;
    }

    @org.springframework.transaction.annotation.Transactional
    public Coupon useCoupon(
            String couponCode) {

        Coupon coupon =
                repository.findByCouponCode(
                        couponCode
                );

        if (coupon == null) {
            throw new RuntimeException(
                    "Coupon Not Found"
            );
        }

        coupon.setUsedCount(
                coupon.getUsedCount() + 1
        );

        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            repository.delete(coupon);
            return coupon;
        }

        return repository.save(
                coupon
        );
    }

    public void deleteCoupon(
            Long id) {

        repository.deleteById(id);
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * *")
    @org.springframework.transaction.annotation.Transactional
    public void autoDeleteInvalidCoupons() {
        List<Coupon> allCoupons = repository.findAll();
        LocalDate today = LocalDate.now();
        for (Coupon coupon : allCoupons) {
            boolean expired = coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(today);
            boolean fullyUsed = coupon.getUsedCount() != null && coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses();
            if (expired || fullyUsed) {
                repository.delete(coupon);
                System.out.println("Auto-deleted invalid coupon: " + coupon.getCouponCode() + " (Reason: " + (expired ? "Expired" : "Fully Used") + ")");
            }
        }
    }
}