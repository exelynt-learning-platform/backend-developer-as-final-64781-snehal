package com.assignment.booking.dto.reservation;

import com.assignment.booking.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Used for status-only transitions, e.g. a USER cancelling their own reservation,
 * or an ADMIN confirming/cancelling a reservation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ReservationStatus status;
}
