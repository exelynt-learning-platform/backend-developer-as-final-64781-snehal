package com.assignment.booking.controller;

import com.assignment.booking.dto.common.PageResponse;
import com.assignment.booking.dto.reservation.ReservationRequest;
import com.assignment.booking.dto.reservation.ReservationResponse;
import com.assignment.booking.dto.reservation.ReservationStatusUpdateRequest;
import com.assignment.booking.dto.reservation.ReservationUpdateRequest;
import com.assignment.booking.entity.ReservationStatus;
import com.assignment.booking.security.CustomUserDetails;
import com.assignment.booking.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request, principal));
    }

 
    @GetMapping
    public ResponseEntity<PageResponse<ReservationResponse>> findAll(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(PageResponse.of(
                reservationService.findAll(status, minPrice, maxPrice, pageable, principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> findById(@PathVariable Long id,
                                                          @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(reservationService.findById(id, principal));
    }

    // USER may only cancel their own reservation; ADMIN may set any status.
    @PatchMapping("/{id}/status")
    public ResponseEntity<ReservationResponse> updateStatus(@PathVariable Long id,
                                                              @Valid @RequestBody ReservationStatusUpdateRequest request,
                                                              @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(reservationService.updateStatus(id, request, principal));
    }

    // ADMIN only - full update
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody ReservationUpdateRequest request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    // ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
