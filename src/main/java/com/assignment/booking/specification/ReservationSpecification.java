package com.assignment.booking.specification;

import com.assignment.booking.entity.Reservation;
import com.assignment.booking.entity.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class ReservationSpecification {

    private ReservationSpecification() {
    }

    public static Specification<Reservation> withStatus(ReservationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Reservation> withMinPrice(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null ? null : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Reservation> withMaxPrice(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null ? null : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Reservation> belongsToUser(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Reservation> build(ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice, Long userId) {
        return Specification.where(withStatus(status))
                .and(withMinPrice(minPrice))
                .and(withMaxPrice(maxPrice))
                .and(belongsToUser(userId));
    }
}
