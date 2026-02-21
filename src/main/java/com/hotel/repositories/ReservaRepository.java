package com.hotel.repositories;

import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoReserva;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {
    boolean existsByCodigo(String codigo);

    List<Reserva> findByCliente_NumeroDocumentoContainingIgnoreCaseAndEstadoIn(
            String numeroDocumento,
            List<EstadoReserva> estados);

    @Query("""
           select r
           from Reserva r
           where lower(r.cliente.numeroDocumento) in :documentos
           """)
    List<Reserva> findByClienteNumeroDocumentoIn(
            @Param("documentos") Set<String> documentos);


    @Query("""
           select distinct r
           from Reserva r
           join r.habitaciones h
           where h.id in :habitacionIds
             and r.entradaEstimada <= :hasta
             and r.salidaEstimada >= :desde
             and r.estado in :estados
           """)
    List<Reserva> findReservasSolapadasPorHabitacionesYFechas(
            @Param("habitacionIds") List<Long> habitacionIds,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("estados") List<EstadoReserva> estados);

    @Query("""
           select count(r) > 0
           from Reserva r
           join r.habitaciones h
           where h.id = :habitacionId
             and r.id <> :reservaId
             and r.entradaEstimada <= :hasta
             and r.salidaEstimada >= :desde
             and r.estado in :estados
           """)
    boolean existsReservaByHabitacionAndRangoAndIdNot(
            @Param("habitacionId") Long habitacionId,
            @Param("reservaId") Long reservaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("estados") List<EstadoReserva> estados);

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

    long countByEstadoAndEntradaEstimadaLessThanEqualAndSalidaEstimadaGreaterThanEqual(
            EstadoReserva estado,
            LocalDateTime hasta,
            LocalDateTime desde);

    @Query("""
           select count(r)
           from Reserva r
           where r.estado in :estados
             and r.salidaEstimada < :momento
             and r.estancia is null
           """)
    long countReservasExpiradas(
            @Param("estados") List<EstadoReserva> estados,
            @Param("momento") LocalDateTime momento);
}
