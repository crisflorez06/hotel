package com.hotel.services;

import com.hotel.dtos.tarifa.TarifaBaseActualizacionDTO;
import com.hotel.dtos.tarifa.TarifaBaseDTO;
import com.hotel.models.TarifaBase;
import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.TarifaBaseRepository;
import com.hotel.utils.EventoModificadoJsonBuilder;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarifaBaseAdminService {

    private static final List<TipoUnidad> ORDEN_TIPOS_BASE = List.of(
            TipoUnidad.HABITACION,
            TipoUnidad.APARTAESTUDIO,
            TipoUnidad.APARTAMENTO
    );

    private static final Set<TipoUnidad> TIPOS_BASE = EnumSet.copyOf(ORDEN_TIPOS_BASE);

    private final TarifaBaseRepository tarifaBaseRepository;
    private final AuditoriaEventoService auditoriaEventoService;

    public TarifaBaseAdminService(
            TarifaBaseRepository tarifaBaseRepository,
            AuditoriaEventoService auditoriaEventoService) {
        this.tarifaBaseRepository = tarifaBaseRepository;
        this.auditoriaEventoService = auditoriaEventoService;
    }

    public List<TarifaBaseDTO> listarTarifasBase() {
        return ORDEN_TIPOS_BASE.stream()
                .map(this::buscarTarifaBasePorTipo)
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<TarifaBaseDTO> actualizarTarifasBase(TarifaBaseActualizacionDTO actualizacion) {
        validarActualizacion(actualizacion);

        if (!TIPOS_BASE.contains(actualizacion.getTipoUnidad())) {
            throw new IllegalArgumentException(
                    "Solo se permiten tarifas base para: HABITACION, APARTAESTUDIO y APARTAMENTO.");
        }

        List<TarifaBase> tarifasActualizadas = new ArrayList<>();
        TarifaBase tarifaBase = buscarTarifaBasePorTipo(actualizacion.getTipoUnidad());
        EventoModificadoJsonBuilder detalleCambios = new EventoModificadoJsonBuilder();

        if (actualizarTarifaSiCambio(tarifaBase, actualizacion, detalleCambios)) {
            tarifasActualizadas.add(tarifaBase);
        }

        if (!tarifasActualizadas.isEmpty()) {
            tarifaBaseRepository.saveAll(tarifasActualizadas);
            registrarEventoActualizacionTarifa(tarifaBase, detalleCambios.build());
        }

        return listarTarifasBase();
    }

    private void validarActualizacion(TarifaBaseActualizacionDTO actualizacion) {
        if (actualizacion == null) {
            throw new IllegalArgumentException(
                    "Debes enviar la tarifa base a actualizar.");
        }
    }

    private TarifaBase buscarTarifaBasePorTipo(TipoUnidad tipoUnidad) {
        return tarifaBaseRepository.findByTipoUnidad(tipoUnidad)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe la tarifa base para tipo de unidad: " + tipoUnidad));
    }

    private boolean actualizarTarifaSiCambio(
            TarifaBase tarifaBase,
            TarifaBaseActualizacionDTO actualizacion,
            EventoModificadoJsonBuilder detalleCambios) {
        boolean cambio = false;

        if (precioCambio(tarifaBase.getPrecioDiaTemBaja(), actualizacion.getPrecioDiaTemBaja())) {
            detalleCambios.agregarCambio(
                    "precioDiaTemBaja",
                    tarifaBase.getPrecioDiaTemBaja(),
                    actualizacion.getPrecioDiaTemBaja());
            tarifaBase.setPrecioDiaTemBaja(actualizacion.getPrecioDiaTemBaja());
            cambio = true;
        }
        if (precioCambio(tarifaBase.getPrecioDiaTemAlta(), actualizacion.getPrecioDiaTemAlta())) {
            detalleCambios.agregarCambio(
                    "precioDiaTemAlta",
                    tarifaBase.getPrecioDiaTemAlta(),
                    actualizacion.getPrecioDiaTemAlta());
            tarifaBase.setPrecioDiaTemAlta(actualizacion.getPrecioDiaTemAlta());
            cambio = true;
        }
        if (precioCambio(tarifaBase.getPrecioEstadiaCorta(), actualizacion.getPrecioEstadiaCorta())) {
            detalleCambios.agregarCambio(
                    "precioEstadiaCorta",
                    tarifaBase.getPrecioEstadiaCorta(),
                    actualizacion.getPrecioEstadiaCorta());
            tarifaBase.setPrecioEstadiaCorta(actualizacion.getPrecioEstadiaCorta());
            cambio = true;
        }
        if (precioCambio(
                tarifaBase.getPrecioPersonaAdicionalTemBaja(),
                actualizacion.getPrecioPersonaAdicionalTemBaja())) {
            detalleCambios.agregarCambio(
                    "precioPersonaAdicionalTemBaja",
                    tarifaBase.getPrecioPersonaAdicionalTemBaja(),
                    actualizacion.getPrecioPersonaAdicionalTemBaja());
            tarifaBase.setPrecioPersonaAdicionalTemBaja(actualizacion.getPrecioPersonaAdicionalTemBaja());
            cambio = true;
        }
        if (precioCambio(
                tarifaBase.getPrecioPersonaAdicionalTemAlta(),
                actualizacion.getPrecioPersonaAdicionalTemAlta())) {
            detalleCambios.agregarCambio(
                    "precioPersonaAdicionalTemAlta",
                    tarifaBase.getPrecioPersonaAdicionalTemAlta(),
                    actualizacion.getPrecioPersonaAdicionalTemAlta());
            tarifaBase.setPrecioPersonaAdicionalTemAlta(actualizacion.getPrecioPersonaAdicionalTemAlta());
            cambio = true;
        }
        if (precioCambio(
                tarifaBase.getPrecioEstadiaPersonaAdicionalCorta(),
                actualizacion.getPrecioEstadiaPersonaAdicionalCorta())) {
            detalleCambios.agregarCambio(
                    "precioEstadiaPersonaAdicionalCorta",
                    tarifaBase.getPrecioEstadiaPersonaAdicionalCorta(),
                    actualizacion.getPrecioEstadiaPersonaAdicionalCorta());
            tarifaBase.setPrecioEstadiaPersonaAdicionalCorta(actualizacion.getPrecioEstadiaPersonaAdicionalCorta());
            cambio = true;
        }

        return cambio;
    }

    private void registrarEventoActualizacionTarifa(TarifaBase tarifaBase, String detalle) {
        auditoriaEventoService.crearEvento(
                TipoEvento.MODIFICACION_TARIFA_BASE,
                TipoEntidad.TARIFA_BASE,
                tarifaBase.getId(),
                detalle,
                null,
                null);
    }

    private boolean precioCambio(BigDecimal valorActual, BigDecimal nuevoValor) {
        if (valorActual == null) {
            return nuevoValor != null;
        }

        return nuevoValor == null || valorActual.compareTo(nuevoValor) != 0;
    }

    private TarifaBaseDTO toDto(TarifaBase tarifaBase) {
        TarifaBaseDTO dto = new TarifaBaseDTO();
        dto.setTipoUnidad(tarifaBase.getTipoUnidad());
        dto.setPrecioDiaTemBaja(tarifaBase.getPrecioDiaTemBaja());
        dto.setPrecioDiaTemAlta(tarifaBase.getPrecioDiaTemAlta());
        dto.setPrecioEstadiaCorta(tarifaBase.getPrecioEstadiaCorta());
        dto.setPrecioPersonaAdicionalTemBaja(tarifaBase.getPrecioPersonaAdicionalTemBaja());
        dto.setPrecioPersonaAdicionalTemAlta(tarifaBase.getPrecioPersonaAdicionalTemAlta());
        dto.setPrecioEstadiaPersonaAdicionalCorta(tarifaBase.getPrecioEstadiaPersonaAdicionalCorta());
        return dto;
    }
}
