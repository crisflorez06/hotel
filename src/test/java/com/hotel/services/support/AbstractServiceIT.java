package com.hotel.services.support;

import com.hotel.models.*;
import com.hotel.models.enums.*;
import com.hotel.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

import static com.hotel.testdata.EstanciaTestData.estanciaData;
import static com.hotel.testdata.EstanciaTestData.estanciaReservaData;
import static com.hotel.testdata.HabitacionTestData.habitacionData;
import static com.hotel.testdata.OcupanteTestData.*;
import static com.hotel.testdata.PagoTestData.pagoData;
import static com.hotel.testdata.ReservaTestData.reservaData;
import static com.hotel.testdata.TestDataUtils.randomCodigo;
import static com.hotel.testdata.UnidadTestData.unidadData;

public abstract class AbstractServiceIT {

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected UnidadRepository unidadRepository;

    @Autowired
    protected HabitacionRepository habitacionRepository;

    @Autowired
    protected PagoRepository pagoRepository;

    @Autowired
    protected OcupanteRepository ocupanteRepository;

    @Autowired
    protected EstanciaRepository estanciaRepository;

    @Autowired
    protected ReservaRepository reservaRepository;

    protected Unidad crearApartamento(EstadoOperativo estadoOperativo) {

        // 1. Crear y persistir la unidadData
        Unidad unidad = unidadRepository.save(
                unidadData(TipoUnidad.APARTAMENTO, estadoOperativo)
        );

        Habitacion h1 = null;
        Habitacion h2 = null;
        Habitacion h3 = null;

        if(estadoOperativo.equals(EstadoOperativo.PARCIALMENTE)){
            // 2. Crear habitaciones asociadas
           h1 = habitacionData(unidad, randomCodigo("HB-"), EstadoOperativo.OCUPADO);
           h2 = habitacionData(unidad, randomCodigo("HB-"), EstadoOperativo.DISPONIBLE);
           h3 = habitacionData(unidad, randomCodigo("HB-"), EstadoOperativo.DISPONIBLE);
        } else {
            // 2. Crear habitaciones asociadas
           h1 = habitacionData(unidad, randomCodigo("HB-"), estadoOperativo);
           h2 = habitacionData(unidad, randomCodigo("HB-"), estadoOperativo);
           h3 = habitacionData(unidad, randomCodigo("HB-"), estadoOperativo);
        }


        List<Habitacion> habitaciones = new ArrayList<>(List.of(h1, h2, h3));

        // 3. Persistir habitaciones (lado dueño)
        habitacionRepository.saveAll(habitaciones);

        // 4. (Opcional pero recomendado) coherencia en memoria
        unidad.setHabitaciones(habitaciones);

        return unidad;
    }

    protected Unidad crearApartaestudio(EstadoOperativo estadoOperativo) {

        // 1. Crear y persistir la unidadData
        Unidad unidad = unidadRepository.save(
                unidadData(TipoUnidad.APARTAESTUDIO, estadoOperativo)
        );

        // 2. Crear habitaciones asociadas
        Habitacion h1 = habitacionData(unidad, randomCodigo("HB-"), estadoOperativo);

        List<Habitacion> habitaciones = new ArrayList<>(List.of(h1));

        // 3. Persistir habitaciones (lado dueño)
        habitacionRepository.saveAll(habitaciones);

        // 4. (Opcional pero recomendado) coherencia en memoria
        unidad.setHabitaciones(habitaciones);

        return unidad;
    }

    protected Pago crearPagoInicialEnEstancia(Estancia estancia, TipoPago tipoPago) {
        return pagoRepository.save(pagoData(estancia, tipoPago));
    }

    protected Ocupante crearCliente(Ocupante cliente) {
        return ocupanteRepository.save(cliente);
    }

    protected List<Ocupante> crearAcompanantesSinCliente(List<Ocupante> acompanantes) {
        return ocupanteRepository.saveAll(acompanantes);
    }

    protected List<Ocupante> crearAcompanantesConCliente() {
        return ocupanteRepository.saveAll(acompanantesDataConCliente());
    }

    protected Estancia crearEstanciaExistente(List<Habitacion> habitaciones, boolean conPago){

        ModoOcupacion modoOcupacion;
        if(habitaciones.size() == 1) {
            if(habitaciones.getFirst().getUnidad().getTipo().equals(TipoUnidad.APARTAESTUDIO)){
                modoOcupacion = ModoOcupacion.COMPLETO;
            } else {
                modoOcupacion = ModoOcupacion.INDIVIDUAL;
            }
        } else {
            modoOcupacion = ModoOcupacion.COMPLETO;
        }

        Estancia estanciaExistente = estanciaData(
                null,
                crearAcompanantesConCliente(),
                modoOcupacion,
                EstadoEstancia.ACTIVA,
                habitaciones,
                null);

        Estancia estancia = estanciaRepository.save(estanciaExistente);

        if(conPago) {
            Pago pago = crearPagoInicialEnEstancia(estanciaExistente, TipoPago.ANTICIPO_ESTANCIA);
            estanciaExistente.setPagos(new ArrayList<>(List.of(pago)));
            estanciaRepository.save(estanciaExistente);
        }

        return estancia;

    }

    protected Estancia crearEstanciaConReservaExistente(List<Habitacion> habitaciones, boolean conPagoEstancia, boolean conPagoReserva){

        Ocupante cliente = crearCliente(clienteData());

        Reserva reservaExistente = reservaRepository.save(
                reservaData(
                        cliente,
                        ModoOcupacion.COMPLETO,
                        EstadoReserva.CONFIRMADA,
                        habitaciones,
                        null
                )
        );

        Estancia estancia = estanciaData(
                reservaExistente,
                new ArrayList<>(List.of(cliente)),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                habitaciones,
                null
        );

        if(conPagoEstancia  && conPagoReserva) {
            Pago pagoEstancia = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
            Pago pagoReserva = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
            estancia.setPagos(new ArrayList<>(List.of(pagoEstancia, pagoReserva)));
        } else if(conPagoEstancia) {
            Pago pagoEstancia = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
            estancia.setPagos(new ArrayList<>(List.of(pagoEstancia)));
        } else if(conPagoReserva) {
            Pago pagoReserva = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
            estancia.setPagos(new ArrayList<>(List.of(pagoReserva)));
        }

        reservaExistente.setEstancia(estancia);
        estanciaRepository.save(estancia);
        reservaRepository.save(reservaExistente);



        return estancia;

    }

    protected Reserva crearReservaExistente(List<Habitacion> habitaciones, boolean conPago){
        Ocupante cliente = crearCliente(clienteData());

        ModoOcupacion modoOcupacion;
        if(habitaciones.size() == 1) {
            if(habitaciones.getFirst().getUnidad().getTipo().equals(TipoUnidad.APARTAESTUDIO)){
                modoOcupacion = ModoOcupacion.COMPLETO;
            } else {
                modoOcupacion = ModoOcupacion.INDIVIDUAL;
            }
        } else {
            modoOcupacion = ModoOcupacion.COMPLETO;
        }

        Reserva reservaExistente = reservaRepository.save(
                reservaData(
                        cliente,
                        modoOcupacion,
                        EstadoReserva.CONFIRMADA,
                        habitaciones,
                        null
                )
        );



        Estancia estanciaDeReserva = estanciaReservaData(
                reservaExistente,
                modoOcupacion,
                EstadoEstancia.RESERVADA,
                habitaciones,
                null
        );

        reservaExistente.setEstancia(estanciaDeReserva);
        estanciaRepository.save(estanciaDeReserva);
        Reserva reserva = reservaRepository.save(reservaExistente);

        if(conPago) {
            Pago pago = crearPagoInicialEnEstancia(estanciaDeReserva, TipoPago.ANTICIPO_RESERVA);
            estanciaDeReserva.setPagos(new ArrayList<>(List.of(pago)));
            estanciaRepository.save(estanciaDeReserva);
        }

        return reserva;

    }
}
