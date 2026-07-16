package com.example.couponengine.strategy;

import com.example.couponengine.entity.CouponType;
import com.example.couponengine.exception.InvalidCouponException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Auto-discovers every {@link DiscountStrategy} bean in the context and
 * routes to the one that supports the given {@link CouponType}. This is the
 * piece that makes the coupon-rule set extensible: new strategies just need
 * to be added as Spring beans - nothing here needs to change.
 */
@Component
public class DiscountStrategyFactory {

    private final List<DiscountStrategy> strategies;

    public DiscountStrategyFactory(List<DiscountStrategy> strategies) {
        this.strategies = strategies;
    }

    public DiscountStrategy resolve(CouponType type) {
        return strategies.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new InvalidCouponException("No discount strategy registered for type " + type));
    }
}
