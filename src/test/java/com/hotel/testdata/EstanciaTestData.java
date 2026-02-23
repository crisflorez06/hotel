package com.hotel.testdata;

import com.hotel.dtos.estancia.ActivarEstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.SalidaEstanciaDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.models.*;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class EstanciaTestData {

    private EstanciaTestData(){};

    public static Estancia estanciaData(
            Reserva reserva,
            List<Ocupante> ocupantes,
            ModoOcupacion modoOcupacion,
            EstadoEstancia estadoEstancia,
            List<Habitacion> habitaciones,
            List<Pago> pagos
    ) {
        Estancia estancia = new Estancia();

        estancia.setCodigoFolio(TestDataUtils.randomCodigo("EST-"));
        estancia.setReserva(reserva);
        estancia.setOcupantes(ocupantes);
        estancia.setEntradaReal(LocalDateTime.now().plusDays(-1));
        estancia.setSalidaEstimada(estancia.getEntradaReal().plusDays(2));
        estancia.setModoOcupacion(modoOcupacion);
        estancia.setEstado(estadoEstancia);
        estancia.setNotas("Estancia de prueba");
        estancia.setHabitaciones(habitaciones == null ? null : new ArrayList<>(habitaciones));
        estancia.setPagos(pagos);

        return estancia;
    }

    public static Estancia estanciaReservaData(
            Reserva reserva,
            List<Ocupante> ocupantes,
            ModoOcupacion modoOcupacion,
            EstadoEstancia estadoEstancia,
            List<Habitacion> habitaciones,
            List<Pago> pagos
    ) {
        Estancia estancia = new Estancia();

        estancia.setCodigoFolio(TestDataUtils.randomCodigo("EST-"));
        estancia.setReserva(reserva);
        estancia.setOcupantes(ocupantes);
        estancia.setEntradaReal(null);
        estancia.setSalidaEstimada(null);
        estancia.setModoOcupacion(modoOcupacion);
        estancia.setEstado(estadoEstancia);
        estancia.setNotas("Estancia reserva de prueba");
        estancia.setHabitaciones(habitaciones == null ? null : new ArrayList<>(habitaciones));
        estancia.setPagos(pagos);

        return estancia;
    }

    public static EstanciaRequestDTO estanciaRequestDTO(TipoUnidad tipoUnidad, String codigo, Ocupante cliente, List<Ocupante> acompanantes, LocalDateTime fecha, PagoNuevoRequestDTO pago) {
        EstanciaRequestDTO request = new EstanciaRequestDTO();

        request.setTipoUnidad(tipoUnidad);
        request.setCodigo(codigo);

        if(acompanantes != null) {
            List<Long> acompanantesIds = acompanantes.stream()
                    .map(Ocupante::getId)
                    .toList();
            request.setIdAcompanantes(acompanantesIds);
        } else {
            request.setIdAcompanantes(null);
        }

        request.setIdCliente(cliente.getId());

        if(fecha != null){
            request.setEntradaReal(fecha);
            request.setSalidaEstimada(fecha.plusDays(3));
        } else {
            LocalDateTime entrada = LocalDateTime.now();
            request.setEntradaReal(entrada);
            request.setSalidaEstimada(entrada.plusDays(3));
        }
        request.setNotas("Estancia de prueba");
        request.setPago(pago);
        return request;
    }

    public static EstanciaRequestDTO errorFechasEstanciaRequestDTO(Unidad unidad) {
        EstanciaRequestDTO request = new EstanciaRequestDTO();
        request.setTipoUnidad(unidad.getTipo());
        request.setCodigo(unidad.getCodigo());

        LocalDateTime entrada = LocalDateTime.now();
        request.setEntradaReal(entrada.plusDays(3));
        request.setSalidaEstimada(entrada);

        return request;
    }

    public static ActivarEstanciaDTO activarEstanciaRequestDTO(Long idReserva, Ocupante cliente, List<Ocupante> acompanantes, LocalDateTime fecha, PagoNuevoRequestDTO pago) {
        ActivarEstanciaDTO request = new ActivarEstanciaDTO();



        request.setIdReserva(idReserva);
        request.setIdCliente(cliente.getId());
        request.setEntradaReal(fecha);
        request.setSalidaEstimada(fecha.plusDays(3));

        if (acompanantes == null) {
            request.setIdAcompanantes(null);
        } else {
            List<Long> acompanantesIds = acompanantes.stream()
                    .map(Ocupante::getId)
                    .toList();
            request.setIdAcompanantes(acompanantesIds);
        }

        request.setPago(pago);
        request.setNotas("Salida de prueba activar");
        return request;
    }

    public static ActivarEstanciaDTO errorFechasActivarEstanciaRequestDTO(Long idReserva) {
        ActivarEstanciaDTO request = new ActivarEstanciaDTO();

        request.setIdReserva(idReserva);
        request.setEntradaReal(LocalDateTime.now().plusDays(3));
        request.setSalidaEstimada(LocalDateTime.now());


        request.setNotas("Salida de prueba activar");
        return request;
    }

    public static SalidaEstanciaDTO salidaEstanciaRequestDTO(Long idEstancia, LocalDateTime fechaSalidaReal, PagoNuevoRequestDTO pagoEstancia) {
        SalidaEstanciaDTO request = new SalidaEstanciaDTO();
        request.setIdEstancia(idEstancia);
        request.setFechaSalidaReal(fechaSalidaReal);
        request.setPagoEstancia(pagoEstancia);
        request.setNotasSalida("Salida de prueba");
        return request;
    }

}
