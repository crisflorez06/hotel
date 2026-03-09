package com.hotel.resolvers;

import com.hotel.models.*;
import com.hotel.models.enums.*;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.services.CodigoUnicoService;
import com.hotel.utils.ConstruirJsonInfo;
import com.hotel.utils.HabitacionModoKey;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EstanciaReservaResolver {

    private static final Logger logger = LoggerFactory.getLogger(EstanciaReservaResolver.class);

    private final EstanciaRepository estanciaRepository;
    private final ReservaRepository reservaRepository;
    private final CodigoUnicoService codigoUnicoService;

    public EstanciaReservaResolver(EstanciaRepository estanciaRepository,
                                  ReservaRepository reservaRepository,
                                  CodigoUnicoService codigoUnicoService) {
        this.estanciaRepository = estanciaRepository;
        this.reservaRepository = reservaRepository;
        this.codigoUnicoService = codigoUnicoService;
    }

    public Estancia crearEstanciaDesdeReserva(Reserva reserva) {

        if (!reserva.getEstado().equals(EstadoReserva.CONFIRMADA)) {
            throw new IllegalStateException("La reserva con id: " + reserva.getId() + " no está en estado CONFIRMADA, no se puede crear la estancia");
        }
        Estancia estancia = new Estancia();
        estancia.setCodigoFolio(codigoUnicoService.generarCodigoEstancia());
        estancia.setModoOcupacion(reserva.getModoOcupacion());
        estancia.setEstado(EstadoEstancia.RESERVADA);
        estancia.setNotas(null);
        estancia.setHabitaciones(reserva.getHabitaciones());
        estancia.setReserva(reserva);
        reserva.setEstancia(estancia);

        return estanciaRepository.save(estancia);
    }

    public Reserva buscarReservaPorIdDesdeEstancia(Long reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una reserva con id: " + reservaId));
    }

    public Estancia buscarEstanciaPorId(Long estanciaId) {
        return estanciaRepository.findById(estanciaId)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una estancia con id: " + estanciaId));
    }


    public void actualizarEstadoReservaDesdeEstancia(Reserva reserva, EstadoReserva nuevoEstado) {
        reserva.setEstado(nuevoEstado);
        reservaRepository.save(reserva);
    }

    public List<Reserva> buscarProximasReservas() {
        return  reservaRepository.findUltimaReservaPorHabitacionYTipoOcupacion();

    }

    public List<Estancia> buscarEstancias() {
        return estanciaRepository.findUltimaEstanciaPorHabitacionYTipoOcupacion();

    }

    public Map<HabitacionModoKey, Reserva> crearMapaReservasPorHabitacionYModo(List<Reserva> reservas) {
        Map<HabitacionModoKey, Reserva> mapa = new HashMap<>();

        for (Reserva reserva : reservas) {
            ModoOcupacion modo = reserva.getModoOcupacion();
            for (Habitacion h : reserva.getHabitaciones()) {
                mapa.put(new HabitacionModoKey(h.getCodigo(), modo), reserva);
            }
        }

        return mapa;
    }

    public Reserva buscarReservaPorHabitacionYModoOcupacion(
            Map<HabitacionModoKey, Reserva> mapa,
            String codigoHabitacion,
            ModoOcupacion modoOcupacion) {
        HabitacionModoKey key = new HabitacionModoKey(codigoHabitacion, modoOcupacion);
        return mapa.get(key);
    }

    public Map<HabitacionModoKey, Estancia> crearMapaEstanciasPorHabitacionYModo(List<Estancia> estancias) {
        Map<HabitacionModoKey, Estancia> mapa = new HashMap<>();

        for (Estancia estancia : estancias) {
            ModoOcupacion modo = estancia.getModoOcupacion();
            for (Habitacion h : estancia.getHabitaciones()) {
                mapa.put(new HabitacionModoKey(h.getCodigo(), modo), estancia);
            }
        }

        return mapa;
    }

    public Estancia buscarEstanciaPorHabitacionYModoOcupacion(
            Map<HabitacionModoKey, Estancia> mapa,
            String codigoHabitacion,
            ModoOcupacion modoOcupacion) {
        HabitacionModoKey key = new HabitacionModoKey(codigoHabitacion, modoOcupacion);
        return mapa.get(key);
    }

    public String llenarInformacionEstanciaYReserva(Estancia estancia, Reserva reserva) {
        ConstruirJsonInfo informacionAdicional = new ConstruirJsonInfo();

        agregarInfoEstancia(informacionAdicional, estancia);

        agregarInfoReserva(informacionAdicional, reserva);

        return informacionAdicional.build();
    }


    private void agregarInfoEstancia(ConstruirJsonInfo informacionAdicional, Estancia estancia) {


        informacionAdicional.agregarObjeto("ESTANCIA");
        if (estancia == null) {
            return;
        }

        int numeroPersonas;
        String nombreCliente;
        String idCliente;
        String idReserva = "N/A";

        if(estancia.getOcupantes() != null) {
            numeroPersonas = estancia.getOcupantes().size();
            Ocupante cliente = estancia.getOcupantes().stream()
                    .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                    .findFirst()
                    .orElse(null);


            nombreCliente = cliente != null ? cliente.getNombres() + " " + cliente.getApellidos() : "N/A";
            idCliente = cliente != null ? cliente.getId().toString() : "N/A";

        } else {
            logger.info("La estancia con codigo folio {} no tiene ocupantes registrados.", estancia.getCodigoFolio());
            numeroPersonas = 0;
            nombreCliente = "N/A";
            idCliente = "N/A";
        }

        if(estancia.getReserva() != null) {
            idReserva = estancia.getReserva().getId().toString();
        }

        informacionAdicional.agregarPropiedades("idEstancia", estancia.getId());
        informacionAdicional.agregarPropiedades("codigo", estancia.getCodigoFolio());
        informacionAdicional.agregarPropiedades("idCliente", idCliente);
        informacionAdicional.agregarPropiedades("nombreCliente", nombreCliente);
        informacionAdicional.agregarPropiedades("numeroPersonas", numeroPersonas);
        informacionAdicional.agregarPropiedades("fechaEntrada", estancia.getEntradaReal());
        informacionAdicional.agregarPropiedades("fechaSalida", estancia.getSalidaEstimada());
        informacionAdicional.agregarPropiedades("modoOcupacion", estancia.getModoOcupacion());
        informacionAdicional.agregarPropiedades("estado", estancia.getEstado());
        informacionAdicional.agregarPropiedades("totalPagadoAnticipo", llenarInformacionPagos(estancia, TipoPago.ANTICIPO_ESTANCIA));
        informacionAdicional.agregarPropiedades("idReserva", idReserva);
    }

    private void agregarInfoReserva(ConstruirJsonInfo informacionAdicional, Reserva reserva) {
        informacionAdicional.agregarObjeto("RESERVA");
        if (reserva == null) {
            return;
        }

        String nombreCliente;
        String idCliente;

        if(reserva.getCliente() != null) {
            nombreCliente = reserva.getCliente().getNombres() + " " + reserva.getCliente().getApellidos();
            idCliente = reserva.getCliente().getId().toString();
        } else {
            nombreCliente = "N/A";
            idCliente = "N/A";
        }

        informacionAdicional.agregarPropiedades("idReserva", reserva.getId());
        informacionAdicional.agregarPropiedades("idEstancia", reserva.getEstancia() != null ? reserva.getEstancia().getId() : "N/A");
        informacionAdicional.agregarPropiedades("codigo", reserva.getCodigo());
        informacionAdicional.agregarPropiedades("idCliente", idCliente);
        informacionAdicional.agregarPropiedades("nombreCliente", nombreCliente);
        informacionAdicional.agregarPropiedades("numeroPersonas", reserva.getNumeroPersonas());
        informacionAdicional.agregarPropiedades("fechaEntrada", reserva.getEntradaEstimada());
        informacionAdicional.agregarPropiedades("fechaSalida", reserva.getSalidaEstimada());
        informacionAdicional.agregarPropiedades("modoOcupacion", reserva.getModoOcupacion());
        informacionAdicional.agregarPropiedades("estado", reserva.getEstado());
        informacionAdicional.agregarPropiedades("totalPagadoAnticipo", llenarInformacionPagos(reserva.getEstancia(), TipoPago.ANTICIPO_RESERVA));
    }


    private String llenarInformacionPagos(Estancia estancia, TipoPago tipoPago) {

        BigDecimal montoTotal = BigDecimal.ZERO;

        if (estancia.getPagos() != null) {
            for(Pago pago : estancia.getPagos()) {
                if (pago.getTipoPago() == tipoPago) {
                    montoTotal = estancia.getPagos().stream()
                            .filter(p -> p.getTipoPago() == tipoPago && p.getEstado() == EstadoPago.COMPLETADO)
                            .map(Pago::getMonto)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
            }
        }

        return montoTotal.toString();
    }






}
