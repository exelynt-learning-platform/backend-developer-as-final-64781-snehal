package com.assignment.booking.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.assignment.booking.dto.reservation.ReservationRequest;
import com.assignment.booking.dto.reservation.ReservationResponse;
import com.assignment.booking.dto.reservation.ReservationStatusUpdateRequest;
import com.assignment.booking.dto.reservation.ReservationUpdateRequest;
import com.assignment.booking.entity.Reservation;
import com.assignment.booking.entity.ReservationStatus;
import com.assignment.booking.entity.Resource;
import com.assignment.booking.entity.Role;
import com.assignment.booking.entity.User;
import com.assignment.booking.exception.BadRequestException;
import com.assignment.booking.exception.ForbiddenAccessException;
import com.assignment.booking.exception.ResourceNotFoundException;
import com.assignment.booking.repository.ReservationRepository;
import com.assignment.booking.repository.UserRepository;
import com.assignment.booking.security.CustomUserDetails;
import com.assignment.booking.specification.ReservationSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ResourceService resourceService;

   
    @Transactional
    public ReservationResponse create(ReservationRequest request, CustomUserDetails principal) {
        validateTimeWindow(request.getStartTime(), request.getEndTime());

        Resource resource = resourceService.getEntity(request.getResourceId());
        validateNoOverlap(resource.getId(), request.getStartTime(), request.getEndTime(), null);

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(user)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ReservationStatus.PENDING)
                .price(request.getPrice())
                .build();

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation {} created by user {} for resource {}", saved.getId(), user.getUsername(), resource.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> findAll(ReservationStatus status,
                                              BigDecimal minPrice,
                                              BigDecimal maxPrice,
                                              Pageable pageable,
                                              CustomUserDetails principal) {
        validatePriceRange(minPrice, maxPrice);

        // ADMIN sees everything; USER is scoped to their own reservations only.
        Long scopedUserId = principal.getUser().getRole() == Role.ADMIN ? null : principal.getId();

        var spec = ReservationSpecification.build(status, minPrice, maxPrice, scopedUserId);
        return reservationRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReservationResponse findById(Long id, CustomUserDetails principal) {
        Reservation reservation = getEntity(id);
        assertOwnershipOrAdmin(reservation, principal);
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationStatusUpdateRequest request, CustomUserDetails principal) {
        Reservation reservation = getEntity(id);
        assertOwnershipOrAdmin(reservation, principal);

        // A regular USER may only cancel their own reservation; any other transition is admin-only.
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        if (!isAdmin && request.getStatus() != ReservationStatus.CANCELLED) {
            throw new ForbiddenAccessException("Users may only cancel their own reservations");
        }

        reservation.setStatus(request.getStatus());
        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation {} status changed to {} by user {}", id, request.getStatus(), principal.getUsername());
        return toResponse(saved);
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationUpdateRequest request) {
        // Full update is ADMIN-only; enforced at the controller/security layer as well.
        Reservation reservation = getEntity(id);
        validateTimeWindow(request.getStartTime(), request.getEndTime());

        Resource resource = resourceService.getEntity(request.getResourceId());
        if (request.getStatus() != ReservationStatus.CANCELLED) {
            validateNoOverlap(resource.getId(), request.getStartTime(), request.getEndTime(), id);
        }
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setStatus(request.getStatus());
        reservation.setPrice(request.getPrice());

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id) {
        Reservation reservation = getEntity(id);
        reservationRepository.delete(reservation);
        log.info("Reservation {} deleted", id);
    }

    private void validateNoOverlap(Long resourceId, java.time.LocalDateTime start,
                                   java.time.LocalDateTime end, Long excludeReservationId) {
        boolean overlapping = excludeReservationId == null
                ? reservationRepository.existsOverlappingActiveReservation(resourceId, start, end)
                : reservationRepository.existsOverlappingActiveReservationExcludingId(
                        resourceId, start, end, excludeReservationId);

        if (overlapping) {
            throw new BadRequestException(
                    "Resource is already booked for the selected time window");
        }
    }

    private Reservation getEntity(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private void assertOwnershipOrAdmin(Reservation reservation, CustomUserDetails principal) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        boolean isOwner = reservation.getUser().getId().equals(principal.getId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenAccessException("You do not have access to this reservation");
        }
    }

    private void validateTimeWindow(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null) {
            throw new BadRequestException("startTime and endTime are required");
        }
        if (!end.isAfter(start)) {
            throw new BadRequestException("endTime must be after startTime");
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must not be greater than maxPrice");
        }
    }

    private ReservationResponse toResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .resourceId(r.getResource().getId())
                .resourceName(r.getResource().getName())
                .userId(r.getUser().getId())
                .username(r.getUser().getUsername())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .status(r.getStatus())
                .price(r.getPrice())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
