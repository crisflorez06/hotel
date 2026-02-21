package com.hotel.models.enums;

public enum TipoEvento {
    CREACION_RESERVA,
    MODIFICACION_RESERVA,
    ELIMINACION_RESERVA,
    CREACION_ESTANCIA,
    ACTIVACION_ESTANCIA,
    MODIFICACION_ESTANCIA,
    ELIMINACION_ESTANCIA,
    FINALIZACION_ESTANCIA,
    CREACION_PAGO,
    MODIFICACION_PAGO,
    ELIMINACION_PAGO;

    public boolean contiene(String palabra) {
        if (palabra == null || palabra.isBlank()) {
            return false;
        }
        return this.name().contains(palabra.toUpperCase());
    }
}
