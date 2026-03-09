package com.hotel.mappers;


import com.hotel.dtos.reserva.ReservaCalendarioDTO;
import com.hotel.dtos.reserva.ReservaDTO;
import com.hotel.dtos.reserva.ReservaRequestDTO;
import com.hotel.dtos.reserva.ReservaTablaDTO;
import com.hotel.models.Estancia;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.enums.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReservaMapper {

    public static Reserva requestNuevoToEntity(ReservaRequestDTO request) {
        Reserva entity = new Reserva();

        entity.setFechaCreacion(LocalDateTime.now());
        entity.setEntradaEstimada(request.getEntradaEstimada().atStartOfDay());
        entity.setSalidaEstimada(request.getSalidaEstimada().atStartOfDay());
        entity.setNumeroPersonas(request.getNumeroPersonas());
        entity.setEstado(EstadoReserva.CONFIRMADA);
        entity.setCanalReserva(request.getCanalReserva());
        if(request.getTipoUnidad() == TipoUnidad.HABITACION) {
            entity.setModoOcupacion(ModoOcupacion.INDIVIDUAL);
        } else {
            entity.setModoOcupacion(ModoOcupacion.COMPLETO);
        }
        entity.setNotas("Reserva al registrar: " + request.getNotas());


        return entity;
    }

    public static ReservaDTO entityToDTO(Reserva reserva) {
        ReservaDTO dto = new ReservaDTO();
        dto.setId(reserva.getId());
        dto.setCodigoReserva(reserva.getCodigo());
        dto.setFechaCreacion(reserva.getFechaCreacion());
        dto.setEntradaEstimada(reserva.getEntradaEstimada());
        dto.setSalidaEstimada(reserva.getSalidaEstimada());
        dto.setNumeroPersonas(reserva.getNumeroPersonas());
        dto.setCanalReserva(reserva.getCanalReserva());
        dto.setModoOcupacion(reserva.getModoOcupacion());
        dto.setEstadoReserva(reserva.getEstado());
        dto.setNotas(reserva.getNotas());

        if (reserva.getCliente() != null) {
            dto.setIdCliente(reserva.getCliente().getId());
            dto.setNombreCliente(
                    String.format("%s %s",
                            reserva.getCliente().getNombres(),
                            reserva.getCliente().getApellidos()).trim());
        }

        if (reserva.getEstancia() != null) {
            dto.setIdEstancia(reserva.getEstancia().getId());
            dto.setCodigoEstancia(reserva.getEstancia().getCodigoFolio());
            dto.setEstadoEstancia(reserva.getEstancia().getEstado());
        }

        return dto;
    }

    public static ReservaCalendarioDTO entityToCalendarioDTO(Reserva reserva) {
        ReservaCalendarioDTO dto = new ReservaCalendarioDTO();

        dto.setId(reserva.getId());
        if(reserva.getEstancia() != null && reserva.getEstancia().getEntradaReal() != null) {
            dto.setInicio(reserva.getEstancia().getEntradaReal());
        } else {
            dto.setInicio(reserva.getEntradaEstimada());
        }
        dto.setFin(reserva.getSalidaEstimada());
        dto.setCodigoReserva(reserva.getCodigo());
        dto.setEstadoReserva(reserva.getEstado());
        dto.setNumeroPersonas(reserva.getNumeroPersonas());
        dto.setNombreCliente(reserva.getCliente().getNombres() + " " + reserva.getCliente().getApellidos());
        dto.setIdCliente(reserva.getCliente().getId());
        dto.setIdEstancia(reserva.getEstancia() != null ? reserva.getEstancia().getId() : null);

        if (reserva.getEstancia() != null && reserva.getEstancia().getPagos() != null) {
            dto.setTotalAnticipo(reserva.getEstancia().getPagos().stream()
                    .filter(pago -> pago.getTipoPago() == TipoPago.ANTICIPO_RESERVA)
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return dto;
    }

    public static List<ReservaCalendarioDTO> entityListaToCalendarioDTOList(List<Reserva> reservas) {
        return reservas.stream()
                .map(ReservaMapper::entityToCalendarioDTO)
                .toList();
    }

    public static ReservaTablaDTO entityToTablaDTO(Reserva reserva) {
        ReservaTablaDTO dto = new ReservaTablaDTO();
        Estancia estancia = reserva.getEstancia();
        boolean estanciaCanceladaSinActivar = estancia != null
                && estancia.getEstado() == EstadoEstancia.CANCELADA
                && estancia.getEntradaReal() == null;
        boolean estanciaReservada = estancia != null
                && estancia.getEstado() == EstadoEstancia.RESERVADA;
        boolean estanciaAsociada = estancia != null
                && !estanciaCanceladaSinActivar
                && !estanciaReservada;
        dto.setId(reserva.getId());
        dto.setCodigoReserva(reserva.getCodigo());
        dto.setCodigoEstancia(estanciaAsociada ? estancia.getCodigoFolio() : null);
        dto.setFechaCreacion(reserva.getFechaCreacion());
        dto.setEntradaEstimada(reserva.getEntradaEstimada());
        dto.setSalidaEstimada(reserva.getSalidaEstimada());
        dto.setNumeroPersonas(reserva.getNumeroPersonas());
        dto.setCanalReserva(reserva.getCanalReserva());
        dto.setModoOcupacion(reserva.getModoOcupacion());
        dto.setEstadoReserva(reserva.getEstado());
        dto.setTieneEstanciaAsociada(estanciaAsociada);
        dto.setTotalPagoReserva(BigDecimal.ZERO);
        dto.setCantidadPagosModificadosOEliminados(0);

        if (reserva.getCliente() != null) {
            dto.setIdCliente(reserva.getCliente().getId());
            dto.setTipoDocumentoCliente(reserva.getCliente().getTipoDocumento());
            dto.setNumeroDocumentoCliente(reserva.getCliente().getNumeroDocumento());
            dto.setNombreCliente(
                    String.format("%s %s",
                            reserva.getCliente().getNombres(),
                            reserva.getCliente().getApellidos()).trim());
        }

        if (reserva.getHabitaciones() != null && !reserva.getHabitaciones().isEmpty()) {
            if (reserva.getModoOcupacion() == ModoOcupacion.INDIVIDUAL) {
                dto.setCodigoUnidad(reserva.getHabitaciones().getFirst().getCodigo());
                dto.setTipoUnidad(TipoUnidad.HABITACION);
            } else if (reserva.getHabitaciones().getFirst().getUnidad() != null) {
                dto.setCodigoUnidad(reserva.getHabitaciones().getFirst().getUnidad().getCodigo());
                dto.setTipoUnidad(reserva.getHabitaciones().getFirst().getUnidad().getTipo());
            }
        }

        if (reserva.getEstancia() != null && reserva.getEstancia().getPagos() != null) {
            dto.setTotalPagoReserva(reserva.getEstancia().getPagos().stream()
                    .filter(pago -> pago.getTipoPago() == TipoPago.ANTICIPO_RESERVA)
                    .filter(pago -> pago.getEstado() == EstadoPago.COMPLETADO)
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            int cantidadPagosModificadosOEliminados = (int) reserva.getEstancia().getPagos().stream()
                    .filter(pago ->  pago.getEstado() == EstadoPago.ELIMINADO)
                    .count();
            dto.setCantidadPagosModificadosOEliminados(cantidadPagosModificadosOEliminados);
        }

        return dto;
    }

}
