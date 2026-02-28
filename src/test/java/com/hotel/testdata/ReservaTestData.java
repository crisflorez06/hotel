package com.hotel.testdata;

import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.dtos.reserva.ReservaRequestDTO;
import com.hotel.models.*;
import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ReservaTestData {

    private ReservaTestData(){}

    public static Reserva reservaData(Ocupante cliente, ModoOcupacion modoOcupacion, EstadoReserva estadoReserva, List<Habitacion> habitaciones, Estancia estancia){
        Reserva reserva = new Reserva();

        reserva.setCodigo(TestDataUtils.randomCodigo("RES-"));
        reserva.setCliente(cliente);
        reserva.setNumeroPersonas(2);
        reserva.setFechaCreacion(LocalDateTime.now());
        reserva.setEntradaEstimada(reserva.getFechaCreacion().plusDays(2));
        reserva.setSalidaEstimada(reserva.getEntradaEstimada().plusDays(2));
        reserva.setModoOcupacion(modoOcupacion);
        reserva.setEstado(estadoReserva);
        reserva.setCanalReserva(CanalReserva.MOSTRADOR);
        reserva.setNotas("prueba para nota de reserva");
        reserva.setHabitaciones(habitaciones == null ? null : new ArrayList<>(habitaciones));
        reserva.setEstancia(estancia);

        return reserva;

    }

    public static ReservaRequestDTO reservaRequestDTO(
            TipoUnidad tipoUnidad,
            String codigo,
            Ocupante cliente,
            LocalDate entradaEstimada,
            PagoNuevoRequestDTO pago) {
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setTipoUnidad(tipoUnidad);
        request.setCodigo(codigo);
        request.setIdOcupante(cliente.getId());
        request.setNumeroPersonas(2);

        if(entradaEstimada == null) {
            request.setEntradaEstimada(LocalDate.now().plusDays(2));
            request.setSalidaEstimada(request.getEntradaEstimada().plusDays(2));
        } else {
                request.setEntradaEstimada(entradaEstimada);
                request.setSalidaEstimada(entradaEstimada.plusDays(2));
            }


        request.setCanalReserva(CanalReserva.MOSTRADOR);
        request.setNotas("Reserva de prueba");
        request.setPago(pago);
        return request;
    }

    public static ReservaRequestDTO errorFechasReservaRequestDTO(Unidad unidad) {
        ReservaRequestDTO request = new ReservaRequestDTO();
        request.setTipoUnidad(unidad.getTipo());
        request.setCodigo(unidad.getCodigo());

        LocalDate entrada = LocalDate.now();
        request.setEntradaEstimada(entrada.plusDays(3));
        request.setSalidaEstimada(entrada);

        return request;
    }
}
