package com.hotel.mappers;

import com.hotel.dtos.gasto.GastoDTO;
import com.hotel.dtos.gasto.GastoNuevoRequestDTO;
import com.hotel.models.Gasto;
import java.util.List;
import java.util.stream.Collectors;

public class GastoMapper {

    public static GastoDTO entityToDTO(Gasto gasto) {
        GastoDTO dto = new GastoDTO();
        dto.setId(gasto.getId());
        dto.setConcepto(gasto.getConcepto());
        dto.setDescripcion(gasto.getDescripcion());
        dto.setMonto(gasto.getMonto());
        dto.setFecha(gasto.getFecha());
        dto.setMetodoPago(gasto.getMetodoPago());
        dto.setReferencia(gasto.getReferencia());
        dto.setEstado(gasto.getEstado());
        return dto;
    }

    public static List<GastoDTO> entityListToDTOList(List<Gasto> gastos) {
        return gastos.stream()
                .map(GastoMapper::entityToDTO)
                .collect(Collectors.toList());
    }

    public static Gasto requestNuevoToEntity(GastoNuevoRequestDTO request) {
        Gasto gasto = new Gasto();
        gasto.setConcepto(request.getConcepto());
        gasto.setDescripcion(request.getDescripcion());
        gasto.setMonto(request.getMonto());
        gasto.setFecha(request.getFecha());
        gasto.setMetodoPago(request.getMetodoPago());
        gasto.setReferencia(request.getReferencia());
        gasto.setEstado(request.getEstado());
        return gasto;
    }

    public static void actualizarEntityDesdeRequest(Gasto gasto, GastoNuevoRequestDTO request) {
        gasto.setConcepto(request.getConcepto());
        gasto.setDescripcion(request.getDescripcion());
        gasto.setMonto(request.getMonto());
        gasto.setFecha(request.getFecha());
        gasto.setMetodoPago(request.getMetodoPago());
        gasto.setReferencia(request.getReferencia());
        gasto.setEstado(request.getEstado());
    }
}
