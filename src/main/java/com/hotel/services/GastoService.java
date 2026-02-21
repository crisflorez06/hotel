package com.hotel.services;

import com.hotel.dtos.gasto.GastoDTO;
import com.hotel.dtos.gasto.GastoNuevoRequestDTO;
import com.hotel.mappers.GastoMapper;
import com.hotel.models.Gasto;
import com.hotel.repositories.GastoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GastoService {

    private final GastoRepository gastoRepository;

    public GastoService(GastoRepository gastoRepository) {
        this.gastoRepository = gastoRepository;
    }

    @Transactional(readOnly = true)
    public List<GastoDTO> listar() {
        return GastoMapper.entityListToDTOList(gastoRepository.findAll());
    }

    @Transactional(readOnly = true)
    public GastoDTO obtenerPorId(Long id) {
        return GastoMapper.entityToDTO(buscarEntidadPorId(id));
    }

    @Transactional
    public GastoDTO crear(GastoNuevoRequestDTO request) {
        Gasto gasto = GastoMapper.requestNuevoToEntity(request);
        gasto.setId(null);
        return GastoMapper.entityToDTO(gastoRepository.save(gasto));
    }

    @Transactional
    public GastoDTO actualizar(Long id, GastoNuevoRequestDTO request) {
        Gasto gastoExistente = buscarEntidadPorId(id);
        GastoMapper.actualizarEntityDesdeRequest(gastoExistente, request);
        return GastoMapper.entityToDTO(gastoRepository.save(gastoExistente));
    }

    @Transactional
    public void eliminar(Long id) {
        Gasto gasto = buscarEntidadPorId(id);
        gastoRepository.delete(gasto);
    }

    private Gasto buscarEntidadPorId(Long id) {
        return gastoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gasto no encontrado con id: " + id));
    }
}
