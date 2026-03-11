package com.hotel.services;

import com.hotel.dtos.HabitacionDTO;
import com.hotel.dtos.UnidadDTO;
import com.hotel.mappers.HabitacionMapper;
import com.hotel.mappers.UnidadMapper;
import com.hotel.models.*;
import com.hotel.models.enums.*;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.repositories.UnidadRepository;
import com.hotel.resolvers.EstanciaReservaResolver;
import com.hotel.specifications.HabitacionSpecification;
import com.hotel.specifications.UnidadSpecification;
import com.hotel.utils.ConstruirJsonInfo;
import com.hotel.utils.HabitacionModoKey;
import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadService {

    private static final Logger logger = LoggerFactory.getLogger(UnidadService.class);

    private final UnidadRepository unidadRepository;
    private final HabitacionRepository habitacionRepository;
    private final EstanciaReservaResolver estanciaReservaResolver;

    public UnidadService(
            UnidadRepository unidadRepository,
            HabitacionRepository habitacionRepository,
            EstanciaReservaResolver estanciaReservaResolver) {
        this.unidadRepository = unidadRepository;
        this.habitacionRepository = habitacionRepository;
        this.estanciaReservaResolver = estanciaReservaResolver;
    }

    public List<UnidadDTO> buscarUnidades(
            TipoUnidad tipo,
            List<EstadoOperativo> estados,
            List<Piso> pisos,
            String codigo) {
        if (tipo == TipoUnidad.HABITACION) {
            return buscarUnidadesDesdeHabitaciones(estados, pisos, codigo);
        }

        List<UnidadDTO> unidadDTOS = unidadRepository.findAll(UnidadSpecification.byFilters(tipo, estados, pisos, codigo)).stream()
                .map(UnidadMapper::entityToDto)
                .toList();

        return agregarInformacionAdicional(unidadDTOS);

    }

    public Unidad buscarPorCodigo(String codigo) {
        return unidadRepository.findByCodigo(codigo)
                .orElseThrow(() -> new EntityNotFoundException("Unidad no encontrada con codigo: " + codigo));
    }



    public List<Habitacion> buscarHabitacionesPorCodigoUnidad(String codigoUnidad) {
        Unidad unidad = buscarPorCodigo(codigoUnidad);
        return unidad.getHabitaciones();
    }

    @Transactional
    public void cambiarEstadoUnidad(Unidad unidad) {


        EstadoOperativo primerEstado = unidad.getHabitaciones().getFirst().getEstadoOperativo();

        boolean todosIguales = unidad.getHabitaciones().stream()
                .allMatch(h -> h.getEstadoOperativo() == primerEstado);

        EstadoOperativo estadoCalculado = todosIguales
                ? primerEstado
                : EstadoOperativo.PARCIALMENTE;


        unidad.setEstadoOperativo(estadoCalculado);
        unidadRepository.save(unidad);
    }

    private List<UnidadDTO> agregarInformacionAdicional(List<UnidadDTO> unidadDTOS) {

        List<Reserva> reservas = estanciaReservaResolver.buscarProximasReservas();
        List<Estancia> estancias = estanciaReservaResolver.buscarEstancias();
        Map<HabitacionModoKey, Reserva> reservaPorHabitacionYModo =
                estanciaReservaResolver.crearMapaReservasPorHabitacionYModo(reservas);
        Map<HabitacionModoKey, Estancia> estanciaPorHabitacionYModo =
                estanciaReservaResolver.crearMapaEstanciasPorHabitacionYModo(estancias);

        for(UnidadDTO unidad : unidadDTOS) {
            TipoUnidad tipoUnidad = unidad.getTipo();



            if(tipoUnidad.equals(TipoUnidad.APARTAMENTO)) {
                String codigoPrimeraHabitacion = unidad.getHabitaciones().getFirst().getCodigo();
                Reserva reservaUnidad = estanciaReservaResolver.buscarReservaPorHabitacionYModoOcupacion(
                        reservaPorHabitacionYModo,
                        codigoPrimeraHabitacion,
                        ModoOcupacion.COMPLETO);
                Estancia estanciaUnidad = estanciaReservaResolver.buscarEstanciaPorHabitacionYModoOcupacion(
                        estanciaPorHabitacionYModo,
                        codigoPrimeraHabitacion,
                        ModoOcupacion.COMPLETO);

                unidad.setInformacionAdicional(estanciaReservaResolver.llenarInformacionEstanciaYReserva(estanciaUnidad, reservaUnidad));
                for (HabitacionDTO habitacion : unidad.getHabitaciones()) {
                    Reserva reservaHabitacion = estanciaReservaResolver.buscarReservaPorHabitacionYModoOcupacion(
                            reservaPorHabitacionYModo,
                            habitacion.getCodigo(),
                            ModoOcupacion.INDIVIDUAL);
                    Estancia estanciaHabitacion = estanciaReservaResolver.buscarEstanciaPorHabitacionYModoOcupacion(
                            estanciaPorHabitacionYModo,
                            habitacion.getCodigo(),
                            ModoOcupacion.INDIVIDUAL);

                    habitacion.setInformacionAdicional(estanciaReservaResolver.llenarInformacionEstanciaYReserva(estanciaHabitacion, reservaHabitacion));
                }
            } else {
                String codigo = unidad.getCodigo();
                Reserva reservaUnidad = estanciaReservaResolver.buscarReservaPorHabitacionYModoOcupacion(
                        reservaPorHabitacionYModo,
                        codigo,
                        ModoOcupacion.COMPLETO);
                Estancia estanciaUnidad = estanciaReservaResolver.buscarEstanciaPorHabitacionYModoOcupacion(
                        estanciaPorHabitacionYModo,
                        codigo,
                        ModoOcupacion.COMPLETO);

                unidad.setInformacionAdicional(estanciaReservaResolver.llenarInformacionEstanciaYReserva(estanciaUnidad, reservaUnidad));

            }

        }

        return unidadDTOS;
    }

    private List<UnidadDTO> buscarUnidadesDesdeHabitaciones(
            List<EstadoOperativo> estados,
            List<Piso> pisos,
            String codigo) {
        List<HabitacionDTO> habitaciones = habitacionRepository.findAll(HabitacionSpecification.byFilters(estados, pisos, codigo))
                .stream()
                .map(HabitacionMapper::entityToDto)
                .toList();

        List<HabitacionDTO> habitacionesConInfo = agregarInformacionAdicionalHabitaciones(habitaciones);
        List<UnidadDTO> unidades = new ArrayList<>();

        for (HabitacionDTO habitacion : habitacionesConInfo) {
            UnidadDTO unidad = new UnidadDTO();
            unidad.setId(habitacion.getId());
            unidad.setCodigo(habitacion.getCodigo());
            unidad.setTipo(TipoUnidad.HABITACION);
            unidad.setPiso(habitacion.getPiso());
            unidad.setEstado(habitacion.getEstado());
            unidad.setHabitaciones(List.of(habitacion));
            unidad.setInformacionAdicional(habitacion.getInformacionAdicional());
            unidades.add(unidad);
        }

        return unidades;
    }

    private List<HabitacionDTO> agregarInformacionAdicionalHabitaciones(List<HabitacionDTO> habitacionDTOS) {
        List<Reserva> reservas = estanciaReservaResolver.buscarProximasReservas();
        List<Estancia> estancias = estanciaReservaResolver.buscarEstancias();
        Map<HabitacionModoKey, Reserva> reservaPorHabitacionYModo =
                estanciaReservaResolver.crearMapaReservasPorHabitacionYModo(reservas);
        Map<HabitacionModoKey, Estancia> estanciaPorHabitacionYModo =
                estanciaReservaResolver.crearMapaEstanciasPorHabitacionYModo(estancias);

        for (HabitacionDTO habitacion : habitacionDTOS) {
            Reserva reservaHabitacion = estanciaReservaResolver.buscarReservaPorHabitacionYModoOcupacion(
                    reservaPorHabitacionYModo,
                    habitacion.getCodigo(),
                    ModoOcupacion.INDIVIDUAL);
            Estancia estanciaHabitacion = estanciaReservaResolver.buscarEstanciaPorHabitacionYModoOcupacion(
                    estanciaPorHabitacionYModo,
                    habitacion.getCodigo(),
                    ModoOcupacion.INDIVIDUAL);
            habitacion.setInformacionAdicional(
                    estanciaReservaResolver.llenarInformacionEstanciaYReserva(estanciaHabitacion, reservaHabitacion));
        }
        return habitacionDTOS;
    }


}
