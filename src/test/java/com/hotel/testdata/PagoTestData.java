package com.hotel.testdata;

import com.hotel.dtos.estancia.ActivarEstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.SalidaEstanciaDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.models.Estancia;
import com.hotel.models.Ocupante;
import com.hotel.models.Pago;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.MedioPago;
import com.hotel.models.enums.TipoPago;
import com.hotel.models.enums.TipoUnidad;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PagoTestData {

    private PagoTestData() {
    }

    public static Pago pagoData(Estancia estancia, TipoPago tipoPago) {
        Pago pago = new Pago();

        pago.setTipoPago(tipoPago);
        if(tipoPago == TipoPago.ANTICIPO_RESERVA){
            pago.setMonto(BigDecimal.valueOf(500.00));
        } else if (tipoPago == TipoPago.ANTICIPO_ESTANCIA) {
            pago.setMonto(BigDecimal.valueOf(800.00));
        } else {
            pago.setMonto(BigDecimal.valueOf(1000.00));
        }
        pago.setMedioPago(MedioPago.EFECTIVO);
        pago.setFecha(LocalDateTime.now());
        pago.setFechaCreacion(LocalDateTime.now());
        pago.setEstado(EstadoPago.COMPLETADO);
        pago.setEstancia(estancia);
        return pago;
    }

    public static PagoNuevoRequestDTO pagoNuevoRequestDTO(TipoPago tipoPago) {
        PagoNuevoRequestDTO pago = new PagoNuevoRequestDTO();

        pago.setTipoPago(tipoPago);
        pago.setMonto(java.math.BigDecimal.valueOf(500000));
        pago.setMedioPago(MedioPago.EFECTIVO);
        pago.setFecha(LocalDateTime.now());
        pago.setEstado(EstadoPago.COMPLETADO);

        return pago;
    }

}
