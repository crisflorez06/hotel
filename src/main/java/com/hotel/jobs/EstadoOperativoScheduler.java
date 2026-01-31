package com.hotel.jobs;

import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.repositories.EstanciaHabitacionRepository;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.repositories.UnidadRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EstadoOperativoScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EstadoOperativoScheduler.class);

    private final HabitacionRepository habitacionRepository;
    private final UnidadRepository unidadRepository;
    private final ReservaRepository reservaRepository;
    private final EstanciaRepository estanciaRepository;
    private final EstanciaHabitacionRepository estanciaHabitacionRepository;

    public EstadoOperativoScheduler(
            HabitacionRepository habitacionRepository,
            UnidadRepository unidadRepository,
            ReservaRepository reservaRepository,
            EstanciaRepository estanciaRepository,
            EstanciaHabitacionRepository estanciaHabitacionRepository) {
        this.habitacionRepository = habitacionRepository;
        this.unidadRepository = unidadRepository;
        this.reservaRepository = reservaRepository;
        this.estanciaRepository = estanciaRepository;
        this.estanciaHabitacionRepository = estanciaHabitacionRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ejecutarAlIniciar() {
        logger.info("[Scheduler] Reconciliando estados al iniciar");
        reconciliarEstados(LocalDateTime.now());
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void ejecutarCambioDia() {
        logger.info("[Scheduler] Reconciliando estados por cambio de dia");
        reconciliarEstados(LocalDateTime.now());
    }

    private void reconciliarEstados(LocalDateTime ahora) {
        actualizarReservasExpiradas(ahora);
        actualizarEstadosEstancias(ahora);
        actualizarHabitacionesYUnidades(ahora);
    }

    private void actualizarReservasExpiradas(LocalDateTime ahora) {
        List<EstadoReserva> estados = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);
        List<Reserva> reservas = reservaRepository.findReservasExpiradas(estados, ahora);
        if (reservas.isEmpty()) {
            return;
        }
        reservas.forEach(reserva -> reserva.setEstado(EstadoReserva.EXPIRADA));
        reservaRepository.saveAll(reservas);
        logger.info("[Scheduler] Reservas expiradas: {}", reservas.size());
    }

    private void actualizarEstadosEstancias(LocalDateTime ahora) {
        List<Estancia> estancias = estanciaRepository.findByActivoTrue();
        if (estancias.isEmpty()) {
            return;
        }

        List<Reserva> reservasActualizar = new ArrayList<>();
        for (Estancia estancia : estancias) {
            if (estancia.getSalidaReal() != null) {
                estancia.setEstado(EstadoEstancia.FINALIZADA);
                estancia.setActivo(false);
                Reserva reserva = estancia.getReserva();
                if (reserva != null && reserva.getEstado() != EstadoReserva.COMPLETADA) {
                    reserva.setEstado(EstadoReserva.COMPLETADA);
                    reservasActualizar.add(reserva);
                }
                continue;
            }

            if (estancia.getSalidaEstimada() != null && estancia.getSalidaEstimada().isBefore(ahora)) {
                estancia.setEstado(EstadoEstancia.EXCEDIDA);
            } else {
                estancia.setEstado(EstadoEstancia.ACTIVA);
            }
        }

        estanciaRepository.saveAll(estancias);
        if (!reservasActualizar.isEmpty()) {
            reservaRepository.saveAll(reservasActualizar);
        }
    }

    private void actualizarHabitacionesYUnidades(LocalDateTime ahora) {
        List<Habitacion> habitaciones = habitacionRepository.findAll();
        if (habitaciones.isEmpty()) {
            return;
        }

        List<Habitacion> habitacionesActualizar = new ArrayList<>();
        Map<Unidad, List<Habitacion>> habitacionesPorUnidad = new HashMap<>();

        for (Habitacion habitacion : habitaciones) {
            EstadoOperativo nuevoEstado = resolverEstadoHabitacion(habitacion, ahora);
            if (habitacion.getEstadoOperativo() != nuevoEstado) {
                habitacion.setEstadoOperativo(nuevoEstado);
                habitacionesActualizar.add(habitacion);
            }
            habitacionesPorUnidad
                    .computeIfAbsent(habitacion.getUnidad(), unidad -> new ArrayList<>())
                    .add(habitacion);
        }

        if (!habitacionesActualizar.isEmpty()) {
            habitacionRepository.saveAll(habitacionesActualizar);
        }

        List<Unidad> unidadesActualizar = new ArrayList<>();
        for (Map.Entry<Unidad, List<Habitacion>> entry : habitacionesPorUnidad.entrySet()) {
            Unidad unidad = entry.getKey();
            EstadoOperativo estadoUnidad = calcularEstadoUnidad(entry.getValue());
            if (unidad.getEstadoOperativo() != estadoUnidad) {
                unidad.setEstadoOperativo(estadoUnidad);
                unidadesActualizar.add(unidad);
            }
        }

        if (!unidadesActualizar.isEmpty()) {
            unidadRepository.saveAll(unidadesActualizar);
        }
    }

    private EstadoOperativo resolverEstadoHabitacion(Habitacion habitacion, LocalDateTime ahora) {
        Optional<Estancia> estanciaOpt = estanciaHabitacionRepository.findActiveEstanciaByHabitacionId(habitacion.getId());
        if (estanciaOpt.isPresent()) {

            return EstadoOperativo.OCUPADO;
        }

        boolean reservada = reservaRepository.existsReservaActivaByHabitacionIdAndMomento(
                habitacion.getId(),
                ahora,
                List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA));

        return reservada ? EstadoOperativo.RESERVADO : EstadoOperativo.DISPONIBLE;
    }

    private EstadoOperativo calcularEstadoUnidad(List<Habitacion> habitaciones) {
        if (habitaciones.isEmpty()) {
            return EstadoOperativo.DISPONIBLE;
        }

        EstadoOperativo primerEstado = habitaciones.getFirst().getEstadoOperativo();
        boolean todosIguales = habitaciones.stream()
                .allMatch(habitacion -> habitacion.getEstadoOperativo() == primerEstado);

        return todosIguales ? primerEstado : EstadoOperativo.PARCIALMENTE;
    }
}
