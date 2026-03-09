package com.hotel.services;

import com.hotel.dtos.HabitacionDTO;
import com.hotel.dtos.UnidadDTO;
import com.hotel.mappers.HabitacionMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.enums.*;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.resolvers.EstanciaReservaResolver;
import com.hotel.specifications.HabitacionSpecification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.hotel.utils.ConstruirJsonInfo;
import com.hotel.utils.HabitacionModoKey;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HabitacionService {

    private static final Logger logger = LoggerFactory.getLogger(HabitacionService.class);

    private final HabitacionRepository habitacionRepository;
    private final EstanciaReservaResolver estanciaReservaResolver;


    public HabitacionService(HabitacionRepository habitacionRepository, EstanciaReservaResolver estanciaReservaResolver) {
        this.estanciaReservaResolver = estanciaReservaResolver;
        this.habitacionRepository = habitacionRepository;
    }

    public List<HabitacionDTO> buscarHabitaciones(
            List<EstadoOperativo> estados,
            List<Piso> pisos,
            String codigo) {
        List<HabitacionDTO> habitacionDTOS = habitacionRepository.findAll(HabitacionSpecification.byFilters(estados, pisos, codigo)).stream()
                .map(HabitacionMapper::entityToDto)
                .toList();

        return agregarInformacionAdicional(habitacionDTOS);

    }

    public Habitacion buscarPorCodigo(String codigo) {
        return habitacionRepository.findByCodigo(codigo)
                .orElseThrow( () -> new EntityNotFoundException("Habitacion no encontrada con codigo: " + codigo));
    }

    private List<HabitacionDTO> agregarInformacionAdicional(List<HabitacionDTO> habitacionDTOS) {

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
                    habitacion.setInformacionAdicional(estanciaReservaResolver.llenarInformacionEstanciaYReserva(estanciaHabitacion, reservaHabitacion));
        }
        return habitacionDTOS;
    }




}
