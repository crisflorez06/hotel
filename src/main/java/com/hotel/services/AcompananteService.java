package com.hotel.services;

import com.hotel.models.Acompanante;
import com.hotel.repositories.AcompananteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AcompananteService {

    private final AcompananteRepository acompananteRepository;

    public AcompananteService(AcompananteRepository acompananteRepository) {
        this.acompananteRepository = acompananteRepository;
    }

    public Acompanante buscarPorId(Long id) {
        return acompananteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Acompanante no encontrado con id: " + id));
    }
}
