package com.hotel.services;

import com.hotel.dtos.ocupante.OcupanteDTO;
import com.hotel.dtos.ocupante.OcupanteNuevoRequestDTO;
import com.hotel.mappers.OcupanteMapper;
import com.hotel.models.Ocupante;
import com.hotel.models.enums.TipoOcupante;
import com.hotel.repositories.OcupanteRepository;
import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OcupanteService {

    private static final Logger logger = LoggerFactory.getLogger(OcupanteService.class);
    private final OcupanteRepository ocupanteRepository;

    public OcupanteService(OcupanteRepository ocupanteRepository) {
        this.ocupanteRepository = ocupanteRepository;
    }

    public Ocupante buscarPorId(Long id) {
        return ocupanteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ocupante no encontrado con id: " + id));
    }

    public List<OcupanteDTO> buscarPorNumeroDocumento(String numeroDocumento) {
        String termino = numeroDocumento == null ? "" : numeroDocumento.trim();
        if (termino.isEmpty()) {
            return List.of();
        }

        return ocupanteRepository.findByNumeroDocumentoContaining(termino).stream()
                .map(OcupanteMapper::ocupanteToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OcupanteDTO crearOcupante(OcupanteNuevoRequestDTO request) {
        logger.info("Creando ocupante: {} {}", request.getNombres(), request.getApellidos());
        Ocupante ocupante = OcupanteMapper.requestNuevoToEntity(request);
        Ocupante guardado = ocupanteRepository.save(ocupante);
        return OcupanteMapper.ocupanteToDto(guardado);
    }



    private Ocupante crearOcupanteConDiferenteTipo(Ocupante ocupante, TipoOcupante tipoOcupante) {
        logger.info("Creando ocupante con diferente tipo: {} {}", ocupante.getNombres(), ocupante.getApellidos());
        Ocupante nuevoOcupante = new Ocupante();
        nuevoOcupante.setNombres(ocupante.getNombres());
        nuevoOcupante.setApellidos(ocupante.getApellidos());
        nuevoOcupante.setTipoDocumento(ocupante.getTipoDocumento());
        nuevoOcupante.setNumeroDocumento(ocupante.getNumeroDocumento());
        nuevoOcupante.setTipoOcupante(tipoOcupante);
        nuevoOcupante.setEmail(ocupante.getEmail());
        nuevoOcupante.setTelefono(ocupante.getTelefono());
        nuevoOcupante.setCreadoEn(ocupante.getCreadoEn());
        return ocupanteRepository.save(nuevoOcupante);
    }

    public List<Ocupante> determinarOcupantesEstancia(Long idCliente, List<Long> idsAcompanantes) {
        logger.info("[OcupanteService.determinarOcupantesEstancia] Cargando ocupantes para la estancia");
        List<Ocupante> ocupantes = new ArrayList<>();

        Ocupante cliente = buscarPorId(idCliente);
        TipoOcupante tipoOcupanteCliente = cliente.getTipoOcupante();

        logger.info("[OcupanteService.determinarOcupantesEstancia] se valida si el cliente antes fue registrado como ACOMPAÑANTE");
        if (tipoOcupanteCliente != TipoOcupante.CLIENTE) {
            ocupantes.add(crearOcupanteConDiferenteTipo(cliente, TipoOcupante.CLIENTE));
        } else {
            ocupantes.add(cliente);
        }

        if (idsAcompanantes == null || idsAcompanantes.isEmpty()) {
            return ocupantes;
        }

        for(Long idAcompanante : idsAcompanantes) {
            if (idAcompanante == null) {
                continue;
            }
            Ocupante acompanante = buscarPorId(idAcompanante);
            TipoOcupante tipoOcupanteAcompanante = acompanante.getTipoOcupante();

            logger.info("[OcupanteService.determinarOcupantesEstancia] se valida si el acompañante antes fue registrado como CLIENTE");
            if (tipoOcupanteAcompanante != TipoOcupante.ACOMPANANTE) {
                ocupantes.add(crearOcupanteConDiferenteTipo(acompanante, TipoOcupante.ACOMPANANTE));
            } else {
                ocupantes.add(acompanante);
            }
        }
        return ocupantes;
    }

}
