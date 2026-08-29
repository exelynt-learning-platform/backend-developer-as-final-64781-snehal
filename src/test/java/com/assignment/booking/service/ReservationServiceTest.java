package com.assignment.booking.service;

import com.assignment.booking.dto.reservation.ReservationRequest;
import com.assignment.booking.entity.*;
import com.assignment.booking.exception.BadRequestException;
import com.assignment.booking.exception.ForbiddenAccessException;
import com.assignment.booking.repository.ReservationRepository;
import com.assignment.booking.repository.UserRepository;
import com.assignment.booking.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private ReservationService reservationService;

    private User owner;
    private User otherUser;
    private User admin;
    private Resource resource;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").password("x").role(Role.USER).build();
        otherUser = User.builder().id(2L).username("other").password("x").role(Role.USER).build();
        admin = User.builder().id(3L).username("admin").password("x").role(Role.ADMIN).build();
        resource = Resource.builder().id(10L).name("Room").type("ROOM").available(true).build();
    }

    @Test
    void create_usesAuthenticatedPrincipalAsOwner_ignoringAnyClientSuppliedIdentity() {
        ReservationRequest request = new ReservationRequest(10L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                BigDecimal.TEN);

        when(resourceService.getEntity(10L)).thenReturn(resource);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(100L);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });

        var response = reservationService.create(request, new CustomUserDetails(owner));

        assertThat(response.getUsername()).isEqualTo("owner");
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void create_rejectsEndTimeBeforeStartTime() {
        ReservationRequest request = new ReservationRequest(10L,
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(1),
                BigDecimal.TEN);

        assertThatThrownBy(() -> reservationService.create(request, new CustomUserDetails(owner)))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void findById_deniesAccessToNonOwnerNonAdmin() {
        Reservation reservation = Reservation.builder()
                .id(50L).resource(resource).user(owner)
                .startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1))
                .status(ReservationStatus.PENDING).price(BigDecimal.ONE)
                .createdAt(LocalDateTime.now())
                .build();

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.findById(50L, new CustomUserDetails(otherUser)))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void findById_allowsAdminRegardlessOfOwnership() {
        Reservation reservation = Reservation.builder()
                .id(51L).resource(resource).user(owner)
                .startTime(LocalDateTime.now()).endTime(LocalDateTime.now().plusHours(1))
                .status(ReservationStatus.PENDING).price(BigDecimal.ONE)
                .createdAt(LocalDateTime.now())
                .build();

        when(reservationRepository.findById(51L)).thenReturn(Optional.of(reservation));

        var response = reservationService.findById(51L, new CustomUserDetails(admin));

        assertThat(response.getId()).isEqualTo(51L);
    }
}
