package com.hotel.services;

import com.hotel.models.Cliente;
import com.hotel.repositories.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private static final Logger logger = LoggerFactory.getLogger(ClienteService.class);


    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con id: " + id));
    }

    public Cliente buscarClientePorEstancia(Long idEstancia) {
        logger.info("[buscarClientePorEstancia] Buscando cliente para la estancia con id: {}", idEstancia);
        return clienteRepository.findByEstancias_Id(idEstancia)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado para la estancia con id: " + idEstancia));
    }
}
