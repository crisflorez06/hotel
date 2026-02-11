package com.hotel.services.support;

import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Ocupante;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.TipoPago;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.repositories.OcupanteRepository;
import com.hotel.repositories.PagoRepository;
import com.hotel.repositories.UnidadRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

import static com.hotel.testdata.HabitacionTestData.habitacionData;
import static com.hotel.testdata.OcupanteTestData.clienteData;
import static com.hotel.testdata.PagoTestData.pagoData;
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

    protected Unidad crearApartamento(EstadoOperativo estadoOperativo) {

        // 1. Crear y persistir la unidadData
        Unidad unidad = unidadRepository.save(
                unidadData(TipoUnidad.APARTAMENTO, estadoOperativo)
        );

        // 2. Crear habitaciones asociadas
        Habitacion h1 = habitacionData(unidad, randomCodigo("HB-"), estadoOperativo);
        Habitacion h2 = habitacionData(unidad, randomCodigo("HB-"), estadoOperativo);
        Habitacion h3 = habitacionData(unidad, randomCodigo("HB-"), estadoOperativo);

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

    protected void crearPagoInicialEnEstancia(Estancia estancia, TipoPago tipoPago) {
        pagoRepository.save(pagoData(estancia, tipoPago));
    }

    protected Ocupante crearOcupante() {
        return ocupanteRepository.save(clienteData());
    }
}
