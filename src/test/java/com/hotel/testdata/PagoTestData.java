package com.hotel.testdata;

import com.hotel.models.Estancia;
import com.hotel.models.Pago;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.MedioPago;
import com.hotel.models.enums.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PagoTestData {

    private PagoTestData() {
    }

    public static Pago pagoData(TipoPago tipoPago, Estancia estancia) {
        Pago pago = new Pago();

        pago.setTipoPago(tipoPago);
        pago.setMonto(BigDecimal.valueOf(1000.00));
        pago.setMedioPago(MedioPago.EFECTIVO);
        pago.setFecha(LocalDateTime.now());
        pago.setFechaCreacion(LocalDateTime.now());
        pago.setEstado(EstadoPago.COMPLETADO);
        pago.setEstancia(estancia);
        return pago;
    }

}
