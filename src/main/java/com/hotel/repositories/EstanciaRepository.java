package com.hotel.repositories;

import com.hotel.models.Estancia;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.ModoOcupacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstanciaRepository extends JpaRepository<Estancia, Long>, JpaSpecificationExecutor<Estancia> {
    boolean existsByCodigoFolio(String codigoFolio);

    Optional<Estancia> findByReserva_Id(Long reservaId);

    @Query("""
           select distinct e
           from Estancia e
           join fetch e.ocupantes o
           where lower(o.numeroDocumento) in :documentos
             and e.estado <> :estadoExcluido
           """)
    List<Estancia> findByOcupantesNumeroDocumentoInAndEstadoNot(
            @Param("documentos") Set<String> documentos,
            @Param("estadoExcluido") EstadoEstancia estadoExcluido);

    @Query("""
           select e
           from Estancia e
           join e.habitaciones h
           where h.id = :habitacionId
             and (e.estado = com.hotel.models.enums.EstadoEstancia.ACTIVA
                  or e.estado = com.hotel.models.enums.EstadoEstancia.EXCEDIDA)
           """)
    Optional<Estancia> findActivaOExcedidaPorHabitacionId(
            @Param("habitacionId") Long habitacionId
    );

    @Query("""
           select e
           from Estancia e
           where e.estado = :estado
             and e.salidaEstimada <= :momento
           """)
    List<Estancia> findByEstadoAndSalidaEstimadaLessThanEqual(
            @Param("estado") EstadoEstancia estado,
            @Param("momento") LocalDateTime momento);

    @Query("""
           select distinct h.id
           from Estancia e
           join e.habitaciones h
           where e.estado in :estados
           """)
    List<Long> findHabitacionIdsByEstadoIn(@Param("estados") List<EstadoEstancia> estados);

    @Query(value = """
            select e.*
            from (
                select e.id,
                       row_number() over (
                           partition by h.codigo, e.modo_ocupacion
                           order by coalesce(e.entrada_real, e.salida_estimado) asc, e.id asc
                       ) as rn
                from estancias e
                join estancia_habitaciones eh on eh.id_estancia = e.id
                join habitaciones h on h.id = eh.id_habitacion
                where e.modo_ocupacion in ('INDIVIDUAL', 'COMPLETO')
                  and e.estado in ('ACTIVA', 'EXCEDIDA')
            ) ult
            join estancias e on e.id = ult.id
            where ult.rn = 1
            """, nativeQuery = true)
    List<Estancia> findUltimaEstanciaPorHabitacionYTipoOcupacion();

    @Query("""
           select distinct e
           from Estancia e
           join e.habitaciones h
           where h.id in :habitacionIds
             and e.estado in :estados
           """)
    List<Estancia> findActivasOExcedidasPorHabitaciones(
            @Param("habitacionIds") List<Long> habitacionIds,
            @Param("estados") List<EstadoEstancia> estados
    );

    long countByEstadoIn(List<EstadoEstancia> estados);

    long countByEstado(EstadoEstancia estado);

    @Query("""
           select count(distinct h.id)
           from Estancia e
           join e.habitaciones h
           where e.estado in :estados
           """)
    long countHabitacionesOcupadasPorEstados(@Param("estados") List<EstadoEstancia> estados);

    @Query("""
           select e
           from Estancia e
           left join fetch e.reserva r
           where e.estado = :estado
           order by e.salidaEstimada asc
           """)
    List<Estancia> findByEstadoConReserva(@Param("estado") EstadoEstancia estado);

    @Query("""
           select count(e)
           from Estancia e
           where e.estado = :estado
             and e.salidaEstimada < :momento
           """)
    long countByEstadoAndSalidaEstimadaBefore(
            @Param("estado") EstadoEstancia estado,
            @Param("momento") LocalDateTime momento);

    @Query("""
           select function('year', e.entradaReal), function('month', e.entradaReal), count(e)
           from Estancia e
           where e.estado in :estados
             and e.entradaReal is not null
             and e.entradaReal between :desde and :hasta
           group by function('year', e.entradaReal), function('month', e.entradaReal)
           order by function('year', e.entradaReal), function('month', e.entradaReal)
           """)
    List<Object[]> contarEstanciasPorMesSegunEntradaRealYEstados(
            @Param("estados") List<EstadoEstancia> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);


}
