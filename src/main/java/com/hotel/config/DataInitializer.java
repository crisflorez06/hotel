package com.hotel.config;

import com.hotel.models.Habitacion;
import com.hotel.models.Acompanante;
import com.hotel.models.Cliente;
import com.hotel.models.Reserva;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.Piso;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.AcompananteRepository;
import com.hotel.repositories.ClienteRepository;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.repositories.UnidadRepository;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final EstadoOperativo ESTADO_DISPONIBLE = EstadoOperativo.DISPONIBLE;
    private static final TipoUnidad TIPO_APARTAMENTO = TipoUnidad.APARTAMENTO;
    private static final TipoUnidad TIPO_APARTAESTUDIO = TipoUnidad.APARTAESTUDIO;

    private final UnidadRepository unidadRepository;
    private final HabitacionRepository habitacionRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;
    private final EstanciaRepository estanciaRepository;
    private final AcompananteRepository acompananteRepository;

    public DataInitializer(
            UnidadRepository unidadRepository,
            HabitacionRepository habitacionRepository,
            ClienteRepository clienteRepository,
            ReservaRepository reservaRepository,
            EstanciaRepository estanciaRepository,
            AcompananteRepository acompananteRepository) {
        this.unidadRepository = unidadRepository;
        this.habitacionRepository = habitacionRepository;
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
        this.estanciaRepository = estanciaRepository;
        this.acompananteRepository = acompananteRepository;
    }

    @Override
    public void run(String... args) {
        if (unidadRepository.count() > 0
                || habitacionRepository.count() > 0
                || clienteRepository.count() > 0
                || reservaRepository.count() > 0
                || estanciaRepository.count() > 0
                || acompananteRepository.count() > 0) {
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

        Cliente cliente = buildCliente();
        clienteRepository.save(cliente);

        Reserva reserva = buildReserva(cliente);
        reservaRepository.save(reserva);

        List<Acompanante> acompanantes = new ArrayList<>();
        acompanantes.add(buildAcompanante("Laura", "Perez"));
        acompanantes.add(buildAcompanante("Carlos", "Gomez"));
        acompananteRepository.saveAll(acompanantes);

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

    private Cliente buildCliente() {
        Cliente cliente = new Cliente();
        cliente.setNombres("Juan");
        cliente.setApellidos("Rodriguez");
        cliente.setTipoDocumento("CC");
        cliente.setNumeroDocumento("123456789");
        cliente.setTelefono("3000000000");
        cliente.setEmail("juan.rodriguez@example.com");
        cliente.setCreadoEn(LocalDateTime.now());
        return cliente;
    }

    private Reserva buildReserva(Cliente cliente) {
        Reserva reserva = new Reserva();
        reserva.setCodigo("RES-1001");
        reserva.setCliente(cliente);
        reserva.setFechaCreacion(LocalDateTime.now());
        reserva.setEntradaEstimada(LocalDateTime.now().plusDays(1));
        reserva.setSalidaEstimada(LocalDateTime.now().plusDays(3));
        reserva.setEstado("CONFIRMADA");
        reserva.setCanalReserva("WEB");
        reserva.setObservaciones("Reserva inicial de prueba");
        return reserva;
    }

    private Acompanante buildAcompanante(String nombres, String apellidos) {
        Acompanante acompanante = new Acompanante();
        acompanante.setNombres(nombres);
        acompanante.setApellidos(apellidos);
        acompanante.setTipoDocumento("CC");
        acompanante.setNumeroDocumento("80000000");
        acompanante.setTelefono("3000000000");
        acompanante.setEmail(nombres.toLowerCase() + "." + apellidos.toLowerCase() + "@example.com");
        acompanante.setCreadoEn(LocalDateTime.now());
        return acompanante;
    }
}
