package com.hotel.config;

import com.hotel.models.Habitacion;
import com.hotel.models.Ocupante;
import com.hotel.models.Unidad;
import com.hotel.models.enums.*;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.repositories.OcupanteRepository;
import com.hotel.repositories.UnidadRepository;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final EstadoOperativo ESTADO_DISPONIBLE = EstadoOperativo.DISPONIBLE;
    private static final TipoUnidad TIPO_APARTAMENTO = TipoUnidad.APARTAMENTO;
    private static final TipoUnidad TIPO_APARTAESTUDIO = TipoUnidad.APARTAESTUDIO;

    private final UnidadRepository unidadRepository;
    private final HabitacionRepository habitacionRepository;
    private final OcupanteRepository ocupanteRepository;
    private final EstanciaRepository estanciaRepository;

    public DataInitializer(
            UnidadRepository unidadRepository,
            HabitacionRepository habitacionRepository,
            OcupanteRepository ocupanteRepository,
            EstanciaRepository estanciaRepository) {
        this.unidadRepository = unidadRepository;
        this.habitacionRepository = habitacionRepository;
        this.ocupanteRepository = ocupanteRepository;
        this.estanciaRepository = estanciaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (unidadRepository.count() > 0
                || habitacionRepository.count() > 0
                || ocupanteRepository.count() > 0
                || estanciaRepository.count() > 0) {
            return;
        }

        List<Unidad> unidades = new ArrayList<>();
        unidades.add(buildUnidad("APT-101", TIPO_APARTAMENTO, Piso.PISO_1));
        unidades.add(buildUnidad("APT-201", TIPO_APARTAMENTO, Piso.PISO_2));
        unidades.add(buildUnidad("APT-301", TIPO_APARTAMENTO, Piso.PISO_3));
        unidades.add(buildUnidad("AE-201", TIPO_APARTAESTUDIO, Piso.PISO_2));
        unidades.add(buildUnidad("AE-202", TIPO_APARTAESTUDIO, Piso.PISO_2));
        unidades.add(buildUnidad("AE-301", TIPO_APARTAESTUDIO, Piso.PISO_3));
        unidades.add(buildUnidad("AE-302", TIPO_APARTAESTUDIO, Piso.PISO_3));
        unidadRepository.saveAll(unidades);

        List<Habitacion> habitaciones = new ArrayList<>();
        for (Unidad unidad : unidades) {
            if (TIPO_APARTAMENTO.equals(unidad.getTipo())) {
                addHabitacionesApartamento(unidad, habitaciones);
            } else {
                addHabitacionApartaestudio(unidad, habitaciones);
            }
        }
        habitacionRepository.saveAll(habitaciones);

        List<Ocupante> ocupantes = new ArrayList<>();
        ocupantes.add(buildOcupante("Juan", "Rodriguez", "123456789", TipoOcupante.CLIENTE));
        ocupantes.add(buildOcupante("Laura", "Perez", "80000000", TipoOcupante.ACOMPANANTE));
        ocupantes.add(buildOcupante("Carlos", "Gomez", "80000001", TipoOcupante.ACOMPANANTE));
        ocupanteRepository.saveAll(ocupantes);

    }

    private Unidad buildUnidad(String codigo, TipoUnidad tipo, Piso piso) {
        Unidad unidad = new Unidad();
        unidad.setCodigo(codigo);
        unidad.setTipo(tipo);
        unidad.setPiso(piso);
        unidad.setEstadoOperativo(ESTADO_DISPONIBLE);
        return unidad;
    }

    private void addHabitacionesApartamento(Unidad unidad, List<Habitacion> habitaciones) {
        for (int i = 1; i <= 3; i++) {
            Habitacion habitacion = new Habitacion();
            habitacion.setUnidad(unidad);
            habitacion.setCodigo(unidad.getCodigo() + "-H" + i);
            habitacion.setEstadoOperativo(ESTADO_DISPONIBLE);
            habitaciones.add(habitacion);
        }
    }

    private void addHabitacionApartaestudio(Unidad unidad, List<Habitacion> habitaciones) {
        Habitacion habitacion = new Habitacion();
        habitacion.setUnidad(unidad);
        habitacion.setCodigo(unidad.getCodigo());
        habitacion.setEstadoOperativo(ESTADO_DISPONIBLE);
        habitaciones.add(habitacion);
    }

    private Ocupante buildOcupante(String nombres, String apellidos, String documento, TipoOcupante tipoOcupante) {
        Ocupante ocupante = new Ocupante();
        ocupante.setNombres(nombres);
        ocupante.setApellidos(apellidos);
        ocupante.setTipoDocumento(TipoDocumento.CC);
        ocupante.setNumeroDocumento(documento);
        ocupante.setTelefono("3000000000");
        ocupante.setEmail(nombres.toLowerCase() + "." + apellidos.toLowerCase() + "@example.com");
        ocupante.setTipoOcupante(tipoOcupante);
        ocupante.setCreadoEn(LocalDateTime.now());
        return ocupante;
    }

}
