package com.hotel.mappers;


import com.hotel.dtos.ReservaCalendarioDTO;
import com.hotel.dtos.ReservaNuevaRequestDTO;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;

import java.time.LocalDateTime;
import java.util.List;

public class ReservaMapper {

    public static Reserva requestNuevoToEntity(ReservaNuevaRequestDTO request) {
        Reserva entity = new Reserva();

        entity.setCodigo(generateCodigoReserva());
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setEntradaEstimada(request.getEntradaEstimada());
        entity.setSalidaEstimada(request.getSalidaEstimada());
        entity.setNumeroPersonas(request.getNumeroPersonas());
        entity.setEstado(EstadoReserva.CONFIRMADA);
        entity.setCanalReserva(request.getCanalReserva());
        if(request.getTipoUnidad() == TipoUnidad.HABITACION) {
            entity.setModoOcupacion(ModoOcupacion.INDIVIDUAL);
        } else {
            entity.setModoOcupacion(ModoOcupacion.COMPLETO);
        }
        entity.setNotas("Notas al registrar: " + request.getNotas());


        return entity;
    }

    public static ReservaCalendarioDTO entityToCalendarioDTO(Reserva reserva) {
        ReservaCalendarioDTO dto = new ReservaCalendarioDTO();

        dto.setId(reserva.getId());
        dto.setInicio(reserva.getEntradaEstimada());
        dto.setFin(reserva.getSalidaEstimada());
        dto.setEstado(reserva.getEstado());
        dto.setNumeroPersonas(reserva.getNumeroPersonas());
        dto.setNombreCliente(reserva.getOcupante().getNombres() + " " + reserva.getOcupante().getApellidos());
        return dto;
    }

    public static List<ReservaCalendarioDTO> entityListaToCalendarioDTOList(List<Reserva> reservas) {
        return reservas.stream()
                .map(ReservaMapper::entityToCalendarioDTO)
                .toList();
    }

    private static String generateCodigoReserva() {
        return "RES-" + System.currentTimeMillis();
    }
}
