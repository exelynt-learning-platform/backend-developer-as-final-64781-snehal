package com.assignment.booking.dto.reservation;

import com.assignment.booking.entity.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Used by ADMIN to fully update a reservation (resource, time window, status, price).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationUpdateRequest {

    @NotNull(message = "Resource id is required")
    private Long resourceId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Status is required")
    private ReservationStatus status;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must not be negative")
    private BigDecimal price;
}
