package com.assignment.booking.repository;

import com.assignment.booking.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    @Query("""
            select case when count(r) > 0 then true else false end
            from Reservation r
            where r.resource.id = :resourceId
              and r.status <> com.assignment.booking.entity.ReservationStatus.CANCELLED
              and r.startTime < :endTime
              and r.endTime > :startTime
            """)
    boolean existsOverlappingActiveReservation(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
            select case when count(r) > 0 then true else false end
            from Reservation r
            where r.resource.id = :resourceId
              and r.id <> :reservationId
              and r.status <> com.assignment.booking.entity.ReservationStatus.CANCELLED
              and r.startTime < :endTime
              and r.endTime > :startTime
            """)
    boolean existsOverlappingActiveReservationExcludingId(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("reservationId") Long reservationId);
}
