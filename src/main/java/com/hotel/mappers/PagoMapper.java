package com.hotel.mappers;

import com.hotel.dtos.pago.CalcularPagoDTO;
import com.hotel.dtos.pago.PagoDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.models.Estancia;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.TipoCalculo;
import com.hotel.models.enums.TipoPago;
import com.hotel.models.enums.TipoUnidad;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class PagoMapper {

    public static Pago requestNuevoToEntity(PagoNuevoRequestDTO request) {
        Pago entity = new Pago();

        entity.setTipoPago(request.getTipoPago());
        entity.setMonto(request.getMonto());
        entity.setMedioPago(request.getMedioPago());
        entity.setFecha(request.getFecha());
        entity.setFechaCreacion(LocalDateTime.now());
        String notas = request.getNotas() == null ? null : request.getNotas().trim();
        if (notas != null && !notas.isBlank()) {
            entity.setNotas("-" + notas);
        }
        entity.setEstado(EstadoPago.COMPLETADO);

        return entity;
    }

    public static Pago cambioUnidadToEntity(BigDecimal monto, LocalDateTime fecha, Estancia estancia) {
        Pago entity = new Pago();

        entity.setTipoPago(TipoPago.CAMBIO_UNIDAD);
        entity.setMonto(monto);
        entity.setMedioPago(null);
        entity.setFecha(fecha);
        entity.setFechaCreacion(fecha);
        entity.setEstado(EstadoPago.PENDIENTE);
        entity.setEstancia(estancia);

        return entity;
    }

    public static PagoDTO entityToDTO(Pago pago) {
        PagoDTO dto = new PagoDTO();
        dto.setId(pago.getId());
        dto.setFecha(pago.getFecha());
        dto.setEstado(pago.getEstado());
        dto.setMedioPago(pago.getMedioPago());
        dto.setMonto(pago.getMonto());
        dto.setNotas(pago.getNotas());
        dto.setTipoPago(pago.getTipoPago());

        return dto;
    }

    public static PagoDTO entityToDTOConCodigos(Pago pago) {
        PagoDTO dto = entityToDTO(pago);
        Estancia estancia = pago.getEstancia();
        if (estancia == null) {
            return dto;
        }

        dto.setCodigoEstancia(estancia.getCodigoFolio());

        Reserva reserva = estancia.getReserva();
        if (reserva != null) {
            dto.setCodigoReserva(reserva.getCodigo());
        }

        if (pago.getTipoPago() == TipoPago.ANTICIPO_RESERVA
                && estancia.getEstado() == EstadoEstancia.RESERVADA) {
            dto.setCodigoEstancia(null);
        }

        return dto;
    }

    public static CalcularPagoDTO entityToCalcularPagoDTO(Long idEstancia, TipoUnidad tipoUnidad, Integer numerosPersonas, LocalDateTime fechaEntrada, LocalDateTime fechaSalida) {
        CalcularPagoDTO dto = new CalcularPagoDTO();

        dto.setIdEstancia(idEstancia);
        dto.setTipoUnidad(tipoUnidad);
        dto.setNumeroPersonas(numerosPersonas);
        dto.setFechaEntrada(fechaEntrada);
        dto.setFechaSalida(fechaSalida);

        long dias = fechaSalida.toLocalDate().toEpochDay() - fechaEntrada.toLocalDate().toEpochDay();
        if (dias < 30) {
            dto.setTipoCalculo(TipoCalculo.ESTANDAR);
        }
        else {
            dto.setTipoCalculo(TipoCalculo.ESTADIA_CORTA);
        }
        return dto;
    }

    public static List<PagoDTO> entityListToDTOList(List<Pago> pagos) {
        return pagos.stream()
                .map(PagoMapper::entityToDTO)
                .collect(Collectors.toList());
    }

    public static List<PagoDTO> entityListToDTOListConCodigos(List<Pago> pagos) {
        return pagos.stream()
                .map(PagoMapper::entityToDTOConCodigos)
                .collect(Collectors.toList());
    }
}
