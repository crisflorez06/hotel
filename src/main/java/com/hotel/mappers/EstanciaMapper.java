package com.hotel.mappers;

import com.hotel.dtos.estancia.EstanciaCalendarioDTO;
import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.EstanciaTablaDTO;
import com.hotel.models.Estancia;
import com.hotel.models.Ocupante;
import com.hotel.models.Pago;
import com.hotel.models.enums.*;

import java.math.BigDecimal;

public class EstanciaMapper {

    public static Estancia requestToEntity(EstanciaRequestDTO request) {
        Estancia entity = new Estancia();

        entity.setEntradaReal(request.getEntradaReal());
        entity.setSalidaEstimada(request.getSalidaEstimada());
        entity.setNotas("Notas al registrar: " + request.getNotas());
        entity.setEstado(EstadoEstancia.ACTIVA);

        return entity;
    }

    public static EstanciaDTO entityToDTO(Estancia entity) {
        EstanciaDTO dto = new EstanciaDTO();

        dto.setId(entity.getId());
        dto.setCodigoFolio(entity.getCodigoFolio());
        dto.setEntradaReal(entity.getEntradaReal());
        dto.setSalidaEstimada(entity.getSalidaEstimada());
        dto.setModoOcupacion(entity.getModoOcupacion());
        dto.setNotas(entity.getNotas());
        dto.setOcupantes(OcupanteMapper.listaOcupanteToDto(entity.getOcupantes()));
        if(entity.getPagos() != null) {
            dto.setPagos(PagoMapper.entityListToDTOListConCodigos(entity.getPagos()));
        }

        return dto;
    }

    public static EstanciaCalendarioDTO entityToCalendarioDTO(Estancia estancia) {
        EstanciaCalendarioDTO dto = new EstanciaCalendarioDTO();

        dto.setId(estancia.getId());
        dto.setInicio(estancia.getEntradaReal());
        if(estancia.getSalidaReal() != null) {
            dto.setFin(estancia.getSalidaReal());
        } else {
            dto.setFin(estancia.getSalidaEstimada());
        }
        dto.setCodigoEstancia(estancia.getCodigoFolio());
        dto.setNumeroPersonas(estancia.getOcupantes().size());
        dto.setNombreCliente(estancia.getOcupantes().stream()
                .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                .findFirst()
                .map(ocupante -> ocupante.getNombres() + " " + ocupante.getApellidos())
                .orElse("Cliente sin nombre"));

        dto.setIdCliente(estancia.getOcupantes().stream()
                .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                .findFirst()
                .map(Ocupante::getId)
                .orElse(null));

        if (estancia.getPagos() != null) {
            dto.setTotalPagado(estancia.getPagos().stream()
                    .filter(pago -> pago.getTipoPago() == TipoPago.ANTICIPO_ESTANCIA
                            || pago.getTipoPago() == TipoPago.ESTANCIA_COMPLETADA)
                    .map(Pago::getMonto)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        }
        dto.setEstadoEstancia(estancia.getEstado());

        return dto;
    }

    public static EstanciaTablaDTO entityToTablaDTO(Estancia estancia) {
        EstanciaTablaDTO dto = new EstanciaTablaDTO();
        dto.setId(estancia.getId());
        dto.setCodigoEstancia(estancia.getCodigoFolio());
        dto.setEstadoEstancia(estancia.getEstado());
        dto.setModoOcupacion(estancia.getModoOcupacion());
        dto.setEntradaReal(estancia.getEntradaReal());
        dto.setSalidaEstimada(estancia.getSalidaEstimada());
        dto.setSalidaReal(estancia.getSalidaReal());
        dto.setTotalPersonas(estancia.getOcupantes() != null ? estancia.getOcupantes().size() : 0);
        dto.setTieneReservaAsociada(estancia.getReserva() != null);
        dto.setIdReservaAsociada(estancia.getReserva() != null ? estancia.getReserva().getId() : null);
        dto.setCodigoReservaAsociada(estancia.getReserva() != null ? estancia.getReserva().getCodigo() : null);
        dto.setTotalPagoEstancia(BigDecimal.ZERO);
        dto.setCantidadPagosModificadosOEliminados(0);

        if (estancia.getOcupantes() != null) {
            Ocupante cliente = estancia.getOcupantes().stream()
                    .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                    .findFirst()
                    .orElse(null);
            if (cliente != null) {
                dto.setIdCliente(cliente.getId());
                dto.setTipoDocumentoCliente(cliente.getTipoDocumento());
                dto.setNumeroDocumentoCliente(cliente.getNumeroDocumento());
                dto.setNombreCliente(String.format("%s %s", cliente.getNombres(), cliente.getApellidos()).trim());
            }
        }

        if (estancia.getHabitaciones() != null && !estancia.getHabitaciones().isEmpty()) {
            if (estancia.getModoOcupacion() == ModoOcupacion.INDIVIDUAL) {
                dto.setCodigoUnidad(estancia.getHabitaciones().getFirst().getCodigo());
                dto.setTipoUnidad(TipoUnidad.HABITACION);
            } else if (estancia.getHabitaciones().getFirst().getUnidad() != null) {
                dto.setCodigoUnidad(estancia.getHabitaciones().getFirst().getUnidad().getCodigo());
                dto.setTipoUnidad(estancia.getHabitaciones().getFirst().getUnidad().getTipo());
            }
        }

        if (estancia.getPagos() != null) {
            dto.setTotalPagoEstancia(estancia.getPagos().stream()
                    .filter(pago -> pago.getEstado() == EstadoPago.PENDIENTE
                            || pago.getEstado() == EstadoPago.COMPLETADO)
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            int cantidadPagosModificadosOEliminados = (int) estancia.getPagos().stream()
                    .filter(pago ->  pago.getEstado() == EstadoPago.ELIMINADO)
                    .count();
            dto.setCantidadPagosModificadosOEliminados(cantidadPagosModificadosOEliminados);
        }

        return dto;
    }
}
