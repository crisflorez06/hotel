package com.hotel.mappers;

import com.hotel.dtos.pago.PagoDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.models.Pago;
import com.hotel.models.enums.TipoPago;

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
        entity.setEstado(request.getEstado());

        return entity;
    }

    public static PagoDTO entityToDTO(Pago pago) {
        PagoDTO dto = new PagoDTO();
        dto.setId(pago.getId());
        dto.setFecha(pago.getFecha());
        dto.setEstado(pago.getEstado());
        dto.setMedioPago(pago.getMedioPago());
        dto.setMonto(pago.getMonto());
        dto.setTipoPago(pago.getTipoPago());

        return dto;
    }

    public static List<PagoDTO> entityListToDTOList(List<Pago> pagos) {
        return pagos.stream()
                .map(PagoMapper::entityToDTO)
                .collect(Collectors.toList());
    }
}
