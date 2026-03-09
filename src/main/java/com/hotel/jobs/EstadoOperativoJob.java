package com.hotel.jobs;

import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoReserva;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "jobs.estado-operativo.enabled", havingValue = "true")
public class EstadoOperativoJob {

    //recuerda que si la reserva expirada supera su fecha de salida se elimina

    private static final Logger logger = LoggerFactory.getLogger(EstadoOperativoJob.class);

    private final HabitacionRepository habitacionRepository;
    private final UnidadRepository unidadRepository;
    private final ReservaRepository reservaRepository;
    private final EstanciaRepository estanciaRepository;

    public EstadoOperativoJob(
            HabitacionRepository habitacionRepository,
            UnidadRepository unidadRepository,
            ReservaRepository reservaRepository,
            EstanciaRepository estanciaRepository) {
        this.habitacionRepository = habitacionRepository;
        this.unidadRepository = unidadRepository;
        this.reservaRepository = reservaRepository;
        this.estanciaRepository = estanciaRepository;
    }

    @Scheduled(cron = "${jobs.estado-operativo.cron:0 */15 * * * *}")
    @Transactional
    public void ejecutar() {
        LocalDateTime ahora = LocalDateTime.now();
        logger.info("[Job EstadoOperativo] Inicio de ejecucion: {}", ahora);

        // Paso 1: cerrar reservas confirmadas que ya vencieron y no se convirtieron en estancia.
        int reservasExpiradas = actualizarReservasExpiradas(ahora);

        // Paso 2: recalcular estados de habitaciones y unidades segun ocupacion real.
        ResultadoReconciliacion resultado = actualizarHabitacionesYUnidades(ahora);

        logger.info(
                "[Job EstadoOperativo] Fin. Reservas expiradas: {}, habitaciones actualizadas: {}, unidades actualizadas: {}",
                reservasExpiradas,
                resultado.habitacionesActualizadas(),
                resultado.unidadesActualizadas());
    }

    private int actualizarReservasExpiradas(LocalDateTime ahora) {
        List<Reserva> reservas = reservaRepository.findReservasExpiradas(
                List.of(EstadoReserva.CONFIRMADA),
                ahora);

        if (reservas.isEmpty()) {
            return 0;
        }

        reservas.forEach(reserva -> reserva.setEstado(EstadoReserva.EXPIRADA));
        reservaRepository.saveAll(reservas);
        return reservas.size();
    }

    private ResultadoReconciliacion actualizarHabitacionesYUnidades(LocalDateTime ahora) {
        List<Habitacion> habitaciones = habitacionRepository.findAll();
        if (habitaciones.isEmpty()) {
            return new ResultadoReconciliacion(0, 0);
        }

        List<Habitacion> habitacionesActualizar = new ArrayList<>();
        Map<Unidad, List<Habitacion>> habitacionesPorUnidad = new HashMap<>();

        for (Habitacion habitacion : habitaciones) {
            EstadoOperativo nuevoEstado = resolverEstadoHabitacion(habitacion, ahora);

            // Solo guardamos cuando hay cambio real para evitar escrituras innecesarias.
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
            EstadoOperativo nuevoEstadoUnidad = calcularEstadoUnidad(entry.getValue());

            if (unidad.getEstadoOperativo() != nuevoEstadoUnidad) {
                unidad.setEstadoOperativo(nuevoEstadoUnidad);
                unidadesActualizar.add(unidad);
            }
        }

        if (!unidadesActualizar.isEmpty()) {
            unidadRepository.saveAll(unidadesActualizar);
        }

        return new ResultadoReconciliacion(habitacionesActualizar.size(), unidadesActualizar.size());
    }

    private EstadoOperativo resolverEstadoHabitacion(Habitacion habitacion, LocalDateTime ahora) {
        Optional<Estancia> estanciaOpt = estanciaRepository.findActivaOExcedidaPorHabitacionId(habitacion.getId());
        if (estanciaOpt.isPresent()) {
            return EstadoOperativo.OCUPADO;
        }

        boolean reservada = reservaRepository.existsReservaActivaByHabitacionIdAndMomento(
                habitacion.getId(),
                ahora,
                List.of(EstadoReserva.CONFIRMADA));

        return reservada ? EstadoOperativo.OCUPADO : EstadoOperativo.DISPONIBLE;
    }

    private EstadoOperativo calcularEstadoUnidad(List<Habitacion> habitaciones) {
        if (habitaciones.isEmpty()) {
            return EstadoOperativo.DISPONIBLE;
        }

        EstadoOperativo primerEstado = habitaciones.get(0).getEstadoOperativo();
        boolean todosIguales = habitaciones.stream()
                .allMatch(habitacion -> habitacion.getEstadoOperativo() == primerEstado);

        return todosIguales ? primerEstado : EstadoOperativo.PARCIALMENTE;
    }

    private record ResultadoReconciliacion(int habitacionesActualizadas, int unidadesActualizadas) {
    }
}
