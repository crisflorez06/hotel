package com.hotel.services;

import com.hotel.dtos.ReservaNuevaRequestDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.mappers.ReservaMapper;
import com.hotel.models.*;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.ReservaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReservaService {

    private static final Logger logger = LoggerFactory.getLogger(EstanciaService.class);

    private final ReservaRepository reservaRepository;
    private final ClienteService clienteService;
    private final HabitacionService habitacionService;

    public ReservaService(ReservaRepository reservaRepository,  ClienteService clienteService, HabitacionService habitacionService) {
        this.clienteService = clienteService;
        this.reservaRepository = reservaRepository;
        this.habitacionService = habitacionService;
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrado con id: " + id));
    }

    @Transactional
    public Reserva crearReserva(ReservaNuevaRequestDTO request) {
        logger.info("Verificando disponibilidad para la reserva solicitada");
        if (!habitacionService.verificarDisponiblidad(request.getCodigo(), request.getTipoUnidad())) {
            throw new IllegalStateException("La unidad o habitacion no está disponible para la reserva.");
        }

        logger.info("Validando fechas de entrada y salida estimada");
        if (request.getSalidaEstimada().isBefore(request.getEntradaEstimada())) {
            throw new IllegalArgumentException("salida estimada debe ser posterior a entrada estimada");
        }

        logger.info("Creando reserva para el cliente con ID: {}", request.getIdCliente());
        Reserva reserva = ReservaMapper.requestNuevoToEntity(request);

        logger.info("Buscando cliente con ID: {}", request.getIdCliente());
        reserva.setCliente(clienteService.buscarPorId(request.getIdCliente()));

        List<Habitacion> habitaciones = new ArrayList<>( habitacionService.clasificarHabitacionesPorTipoUnidad(request.getCodigo(), request.getTipoUnidad()));
        reserva.setHabitaciones(habitaciones);
        habitacionService.cambiarEstadoHabitaciones(request.getCodigo(), EstadoOperativo.RESERVADO, request.getTipoUnidad());
        return reservaRepository.save(reserva);
    }


}
