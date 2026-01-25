package com.hotel.services;

import com.hotel.models.Acompanante;
import com.hotel.repositories.AcompananteRepository;
import com.hotel.repositories.EstanciaAcompananteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AcompananteService {

    private final AcompananteRepository acompananteRepository;
    private final EstanciaAcompananteRepository estanciaAcompananteRepository;
    private static final Logger logger = LoggerFactory.getLogger(AcompananteService.class);


    public AcompananteService(AcompananteRepository acompananteRepository,
                              EstanciaAcompananteRepository estanciaAcompananteRepository) {
        this.acompananteRepository = acompananteRepository;
        this.estanciaAcompananteRepository = estanciaAcompananteRepository;
    }

    public Acompanante buscarPorId(Long id) {
        return acompananteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Acompanante no encontrado con id: " + id));
    }

    public List<Acompanante> buscarPorEstanciaId(Long estanciaId) {
        logger.info("[buscarPorEstanciaId] Buscando acompañantes para la estancia con id: {}", estanciaId);
        return estanciaAcompananteRepository.findAcompanantesByEstanciaId(estanciaId);
    }

}
