package com.hotel.repositories;

import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoReserva;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {

    @Query("""
           select count(r) > 0
           from Reserva r
           join r.habitaciones h
           where h.id = :habitacionId
             and r.entradaEstimada <= :hasta
             and r.salidaEstimada >= :desde
           """)
    boolean existsReservaByHabitacionAndRango(
            @Param("habitacionId") Long habitacionId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
           select count(r) > 0
           from Reserva r
           join r.habitaciones h
           where h.id = :habitacionId
             and r.entradaEstimada <= :momento
             and r.salidaEstimada > :momento
             and r.estado in :estados
           """)
    boolean existsReservaActivaByHabitacionIdAndMomento(
            @Param("habitacionId") Long habitacionId,
            @Param("momento") LocalDateTime momento,
            @Param("estados") List<EstadoReserva> estados);

    @Query("""
           select r
           from Reserva r
           where r.estado in :estados
             and r.salidaEstimada < :momento
             and r.estancia is null
           """)
    List<Reserva> findReservasExpiradas(
            @Param("estados") List<EstadoReserva> estados,
            @Param("momento") LocalDateTime momento);
}
