package com.hotel.mappers;

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
        String notas = request.getNotas() == null ? null : request.getNotas().trim();
        if (notas != null && !notas.isBlank()) {
            entity.setNotas("-" + notas);
        }

        return entity;
    }

    public static ReservaDTO entityToDTO(Reserva entity) {
        ReservaDTO dto = new ReservaDTO();

        dto.setId(entity.getId());
        dto.setCodigoReserva(entity.getCodigo());
        dto.setCodigoEstancia(entity.getEstancia() != null ? entity.getEstancia().getCodigoFolio() : null);
        dto.setIdEstancia(entity.getEstancia() != null ? entity.getEstancia().getId() : null);
        dto.setCliente(OcupanteMapper.ocupanteToDto(entity.getCliente()));
        dto.setFechaCreacion(entity.getFechaCreacion());
        dto.setEntradaEstimada(entity.getEntradaEstimada());
        dto.setSalidaEstimada(entity.getSalidaEstimada());
        dto.setNumeroPersonas(entity.getNumeroPersonas());
        dto.setCanalReserva(entity.getCanalReserva());
        dto.setModoOcupacion(entity.getModoOcupacion());
        dto.setEstadoReserva(entity.getEstado());
        dto.setNotas(entity.getNotas());

        return dto;
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
        dto.setNotas(reserva.getNotas());
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
