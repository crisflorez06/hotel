package com.hotel.services;

import com.hotel.dtos.auditoria.AuditoriaEventoDTO;
import com.hotel.mappers.AuditoriaEventoMapper;
import com.hotel.models.AuditoriaEvento;
import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import com.hotel.repositories.AuditoriaEventoRepository;
import com.hotel.specifications.AuditoriaEventoSpecification;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaEventoService {

    private static final Logger logger = LoggerFactory.getLogger(AuditoriaEventoService.class);

    private final AuditoriaEventoRepository auditoriaEventoRepository;

    public AuditoriaEventoService(AuditoriaEventoRepository auditoriaEventoRepository) {
        this.auditoriaEventoRepository = auditoriaEventoRepository;
    }

    @Transactional
    public AuditoriaEvento crearEvento(TipoEvento tipoEvento,
                                       TipoEntidad entidad,
                                       Long idEntidad,
                                       String detalle,
                                       String codigoEstancia,
                                       String codigoReserva)
    {
        if (tipoEvento == null) {
            throw new IllegalArgumentException("tipoEvento es obligatorio");
        }
        if (entidad == null ) {
            throw new IllegalArgumentException("entidad es obligatoria");
        }
        if (idEntidad == null) {
            throw new IllegalArgumentException("idEntidad es obligatorio");
        }

        logger.info("[crearEvento] Registrando evento {} para entidad {} con id {}", tipoEvento, entidad, idEntidad);

        AuditoriaEvento evento = new AuditoriaEvento();
        evento.setTipoEvento(tipoEvento);
        evento.setEntidad(entidad);
        evento.setIdEntidad(idEntidad);
        evento.setDetalle(detalle);
        if(codigoEstancia != null && !codigoEstancia.isBlank()) {
            evento.setCodigoEstancia(codigoEstancia);
        }
        if(codigoReserva != null && !codigoReserva.isBlank()) {
            evento.setCodigoReserva(codigoReserva);
        }

        return auditoriaEventoRepository.save(evento);
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaEventoDTO> buscarEventos(
            List<TipoEvento> tiposEvento,
            List<TipoEntidad> entidades,
            Long idEntidad,
            String codigoEstancia,
            String codigoReserva,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            String detalle,
            Pageable pageable) {
        Pageable pageableConOrden = pageable;
        if (pageable.getSort().isUnsorted()) {
            pageableConOrden = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "fecha"));
        }

        return auditoriaEventoRepository.findAll(
                        AuditoriaEventoSpecification.byFilters(
                                tiposEvento,
                                entidades,
                                idEntidad,
                                codigoEstancia,
                                codigoReserva,
                                fechaDesde,
                                fechaHasta,
                                detalle),
                        pageableConOrden)
                .map(AuditoriaEventoMapper::entityToDTO);
    }

}
