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

    public List<Coupon> getAllCoupons() {

        return repository.findAll();
    }

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

        if (coupon.getUsedCount()
                >= coupon.getMaxUses()) {

            throw new RuntimeException(
                    "Coupon Usage Limit Reached"
            );
        }

        if (coupon.getExpiryDate()
                .isBefore(
                        LocalDate.now()
                )) {

            throw new RuntimeException(
                    "Coupon Expired"
            );
        }

        return coupon;
    }

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

        return repository.save(
                coupon
        );
    }

public void deleteCoupon(
        Long id) {

    repository.deleteById(id);
}


}