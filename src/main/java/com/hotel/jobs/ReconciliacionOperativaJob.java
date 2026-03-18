package com.hotel.jobs;

import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.repositories.UnidadRepository;
import com.hotel.services.ReservaService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "jobs.reconciliacion.enabled", havingValue = "true", matchIfMissing = true)
public class ReconciliacionOperativaJob {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliacionOperativaJob.class);

    private final EstanciaRepository estanciaRepository;
    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;
    private final UnidadRepository unidadRepository;
    private final ReservaService reservaService;

    public ReconciliacionOperativaJob(
            EstanciaRepository estanciaRepository,
            ReservaRepository reservaRepository,
            HabitacionRepository habitacionRepository,
            UnidadRepository unidadRepository,
            ReservaService reservaService) {
        this.estanciaRepository = estanciaRepository;
        this.reservaRepository = reservaRepository;
        this.habitacionRepository = habitacionRepository;
        this.unidadRepository = unidadRepository;
        this.reservaService = reservaService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ejecutarAlIniciar() {
        logger.info("[Job Reconciliacion] Ejecutando reconciliacion al iniciar aplicacion");
        ejecutarReconciliacion();
    }

    @Scheduled(cron = "${jobs.reconciliacion.cron:0 */5 * * * *}")
    @Transactional
    public void ejecutarProgramado() {
        ejecutarReconciliacion();
    }

    private void ejecutarReconciliacion() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia = hoy.atTime(LocalTime.MAX);

        logger.info("[Job Reconciliacion] Inicio de ejecucion: {}", ahora);

        int estanciasExcedidas = actualizarEstanciasExcedidas(ahora);
        int reservasExpiradas = actualizarReservasExpiradas(inicioDia);
        int reservasEliminadas = eliminarReservasExpiradasVencidas(inicioDia);
        ResultadoOperativo resultadoOperativo = actualizarEstadosOperativos(inicioDia, finDia);

        logger.info(
                "[Job Reconciliacion] Fin. Estancias excedidas: {}, reservas expiradas: {}, reservas eliminadas: {}, habitaciones actualizadas: {}, unidades actualizadas: {}",
                estanciasExcedidas,
                reservasExpiradas,
                reservasEliminadas,
                resultadoOperativo.habitacionesActualizadas(),
                resultadoOperativo.unidadesActualizadas());
    }

    private int actualizarEstanciasExcedidas(LocalDateTime ahora) {
        List<Estancia> estancias = estanciaRepository.findByEstadoAndSalidaEstimadaLessThanEqual(EstadoEstancia.ACTIVA, ahora);
        if (estancias.isEmpty()) {
            return 0;
        }

        estancias.forEach(estancia -> estancia.setEstado(EstadoEstancia.EXCEDIDA));
        estanciaRepository.saveAll(estancias);
        return estancias.size();
    }

    private int actualizarReservasExpiradas(LocalDateTime inicioDia) {
        List<Reserva> reservas = reservaRepository.findByEstadoAndEntradaEstimadaBefore(EstadoReserva.CONFIRMADA, inicioDia);
        if (reservas.isEmpty()) {
            return 0;
        }

        reservas.forEach(reserva -> reserva.setEstado(EstadoReserva.EXPIRADA));
        reservaRepository.saveAll(reservas);
        return reservas.size();
    }

    private int eliminarReservasExpiradasVencidas(LocalDateTime inicioDia) {
        List<Reserva> reservas = reservaRepository.findByEstadoAndSalidaEstimadaBefore(EstadoReserva.EXPIRADA, inicioDia);
        if (reservas.isEmpty()) {
            return 0;
        }

        for (Reserva reserva : reservas) {
            reservaService.eliminarReserva(reserva.getId());
        }
        return reservas.size();
    }

    private ResultadoOperativo actualizarEstadosOperativos(LocalDateTime inicioDia, LocalDateTime finDia) {
        List<Habitacion> habitaciones = habitacionRepository.findAll();
        if (habitaciones.isEmpty()) {
            return new ResultadoOperativo(0, 0);
        }

        Set<Long> habitacionesConEstanciaActivaOExcedida = new HashSet<>(
                estanciaRepository.findHabitacionIdsByEstadoIn(List.of(EstadoEstancia.ACTIVA, EstadoEstancia.EXCEDIDA)));
        Set<Long> habitacionesConReservaEnHoy = new HashSet<>(
                reservaRepository.findHabitacionIdsConReservaEnDia(
                        List.of(EstadoReserva.CONFIRMADA, EstadoReserva.EXPIRADA),
                        inicioDia,
                        finDia));

        List<Habitacion> habitacionesActualizar = new ArrayList<>();
        Map<Long, Unidad> unidadesPorId = new HashMap<>();
        Map<Long, List<Habitacion>> habitacionesPorUnidad = new HashMap<>();

        for (Habitacion habitacion : habitaciones) {
            EstadoOperativo nuevoEstado = resolverEstadoHabitacion(
                    habitacion,
                    habitacionesConEstanciaActivaOExcedida,
                    habitacionesConReservaEnHoy);

            if (habitacion.getEstadoOperativo() != nuevoEstado) {
                habitacion.setEstadoOperativo(nuevoEstado);
                habitacionesActualizar.add(habitacion);
            }

            Unidad unidad = habitacion.getUnidad();
            unidadesPorId.put(unidad.getId(), unidad);
            habitacionesPorUnidad.computeIfAbsent(unidad.getId(), key -> new ArrayList<>()).add(habitacion);
        }

        if (!habitacionesActualizar.isEmpty()) {
            habitacionRepository.saveAll(habitacionesActualizar);
        }

        List<Unidad> unidadesActualizar = new ArrayList<>();
        for (Map.Entry<Long, List<Habitacion>> entry : habitacionesPorUnidad.entrySet()) {
            Unidad unidad = unidadesPorId.get(entry.getKey());
            EstadoOperativo estadoUnidad = calcularEstadoUnidad(entry.getValue());

            if (unidad.getEstadoOperativo() != estadoUnidad) {
                unidad.setEstadoOperativo(estadoUnidad);
                unidadesActualizar.add(unidad);
            }
        }

        if (!unidadesActualizar.isEmpty()) {
            unidadRepository.saveAll(unidadesActualizar);
        }

        return new ResultadoOperativo(habitacionesActualizar.size(), unidadesActualizar.size());
    }

    private EstadoOperativo resolverEstadoHabitacion(
            Habitacion habitacion,
            Set<Long> habitacionesConEstanciaActivaOExcedida,
            Set<Long> habitacionesConReservaEnHoy) {
        Long habitacionId = habitacion.getId();

        if (habitacionesConEstanciaActivaOExcedida.contains(habitacionId)) {
            return EstadoOperativo.OCUPADO;
        }

        if (habitacionesConReservaEnHoy.contains(habitacionId)) {
            return EstadoOperativo.RESERVADO;
        }

        return EstadoOperativo.DISPONIBLE;
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

    private record ResultadoOperativo(int habitacionesActualizadas, int unidadesActualizadas) {
    }
}
