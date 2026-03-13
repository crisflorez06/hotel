package com.hotel.services;

import com.hotel.dtos.ocupante.OcupanteDTO;
import com.hotel.dtos.ocupante.OcupanteNuevoRequestDTO;
import com.hotel.dtos.ocupante.ClienteEstanciaResumenDTO;
import com.hotel.dtos.ocupante.ClienteReservaResumenDTO;
import com.hotel.dtos.ocupante.ClienteTablaDTO;
import com.hotel.mappers.OcupanteMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Ocupante;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.TipoDocumento;
import com.hotel.models.enums.TipoOcupante;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.OcupanteRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.specifications.OcupanteSpecification;
import jakarta.persistence.EntityNotFoundException;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OcupanteService {

    private static final Logger logger = LoggerFactory.getLogger(OcupanteService.class);
    private final OcupanteRepository ocupanteRepository;
    private final ReservaRepository reservaRepository;
    private final EstanciaRepository estanciaRepository;

    public OcupanteService(OcupanteRepository ocupanteRepository,
                           ReservaRepository reservaRepository,
                           EstanciaRepository estanciaRepository) {
        this.ocupanteRepository = ocupanteRepository;
        this.reservaRepository = reservaRepository;
        this.estanciaRepository = estanciaRepository;
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

    @Transactional(readOnly = true)
    public Page<ClienteTablaDTO> buscarClientesTabla(
            String nombre,
            String apellido,
            TipoDocumento tipoDocumento,
            String numeroDocumento,
            String telefono,
            String email,
            Pageable pageable) {
        Sort sort = pageable.getSort().isUnsorted()
                ? Sort.by(Sort.Direction.DESC, "creadoEn")
                : pageable.getSort();

        List<Ocupante> ocupantesFiltrados = ocupanteRepository.findAll(
                OcupanteSpecification.byTablaFilters(nombre, apellido, tipoDocumento, numeroDocumento, telefono, email),
                sort);

        Map<String, List<Ocupante>> agrupados = ocupantesFiltrados.stream()
                .collect(Collectors.groupingBy(
                        this::construirLlaveConsolidacion,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<List<Ocupante>> grupos = List.copyOf(agrupados.values());
        int inicio = Math.min((int) pageable.getOffset(), grupos.size());
        int fin = Math.min(inicio + pageable.getPageSize(), grupos.size());
        List<List<Ocupante>> gruposPagina = grupos.subList(inicio, fin);

        Set<String> documentos = gruposPagina.stream()
                .map(lista -> normalizarDocumento(lista.getFirst().getNumeroDocumento()))
                .filter(doc -> !doc.isBlank())
                .collect(Collectors.toSet());

        Map<String, List<Reserva>> reservasPorDocumento = construirReservasPorDocumento(documentos);
        Map<String, List<Estancia>> estanciasPorDocumento = construirEstanciasPorDocumento(documentos);

        List<ClienteTablaDTO> contenido = gruposPagina.stream()
                .map(ocupantes -> mapearClienteTablaDTO(ocupantes, reservasPorDocumento, estanciasPorDocumento))
                .toList();

        return new PageImpl<>(contenido, pageable, grupos.size());
    }

    @Transactional
    public OcupanteDTO crearOcupante(OcupanteNuevoRequestDTO request) {
        logger.info("Creando ocupante: {} {}", request.getNombres(), request.getApellidos());
        Ocupante ocupante = OcupanteMapper.requestNuevoToEntity(request);
        Ocupante guardado = ocupanteRepository.save(ocupante);
        return OcupanteMapper.ocupanteToDto(guardado);
    }

    @Transactional
    public OcupanteDTO editarOcupante(Long id, OcupanteNuevoRequestDTO request) {
        logger.info("Editando ocupante con id: {}", id);
        Ocupante ocupante = buscarPorId(id);
        String documentoOriginal = ocupante.getNumeroDocumento();
        TipoDocumento tipoDocumentoOriginal = ocupante.getTipoDocumento();

        ocupante.setNombres(request.getNombres());
        ocupante.setApellidos(request.getApellidos());
        ocupante.setTipoDocumento(request.getTipoDocumento());
        ocupante.setNumeroDocumento(request.getNumeroDocumento());
        ocupante.setTelefono(request.getTelefono());
        ocupante.setEmail(request.getEmail());
        ocupante.setTipoOcupante(request.getTipoOcupante());

        List<Ocupante> ocupantesRelacionados =
                documentoOriginal == null || documentoOriginal.trim().isEmpty() || tipoDocumentoOriginal == null
                ? List.of()
                : ocupanteRepository.findByTipoDocumentoAndNumeroDocumentoNormalizado(tipoDocumentoOriginal, documentoOriginal).stream()
                .filter(item -> !item.getId().equals(ocupante.getId()))
                .toList();

        for (Ocupante relacionado : ocupantesRelacionados) {
            relacionado.setNombres(request.getNombres());
            relacionado.setApellidos(request.getApellidos());
            relacionado.setTipoDocumento(request.getTipoDocumento());
            relacionado.setNumeroDocumento(request.getNumeroDocumento());
            relacionado.setTelefono(request.getTelefono());
            relacionado.setEmail(request.getEmail());
        }

        ocupanteRepository.saveAll(ocupantesRelacionados);
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

    public List<Ocupante> determinarOcupantes(Long idCliente, List<Long> idsAcompanantes) {
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

    public Optional<Ocupante> determinarCliente(List<Ocupante> ocupantes) {
        return ocupantes.stream()
                .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                .findFirst();
    }

    public Ocupante buscarCliente(long idCliente) {
        logger.info("[buscarCliente] Determinando cliente para la reserva");

        return ocupanteRepository.findById(idCliente)
                .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con id: " + idCliente));

    }

    public String obtenerNombre(Long idCliente) {
        Ocupante cliente = buscarPorId(idCliente);
        return cliente.getNombres() + " " + cliente.getApellidos();
    }

    public String obtenerNombresAcompanantes(Set<Long> idOcupantes) {
        return idOcupantes.stream()
                .map(this::buscarPorId)
                .map(ocupante -> ocupante.getNombres() + " " + ocupante.getApellidos())
                .collect(Collectors.joining(", "));
    }

    private ClienteTablaDTO mapearClienteTablaDTO(
            List<Ocupante> ocupantes,
            Map<String, List<Reserva>> reservasPorDocumento,
            Map<String, List<Estancia>> estanciasPorDocumento) {
        Ocupante referencia = ocupantes.getFirst();
        String documento = normalizarDocumento(referencia.getNumeroDocumento());
        Set<TipoOcupante> tipos = ocupantes.stream()
                .map(Ocupante::getTipoOcupante)
                .collect(Collectors.toSet());

        ClienteTablaDTO dto = new ClienteTablaDTO();
        dto.setNombres(referencia.getNombres());
        dto.setApellidos(referencia.getApellidos());
        dto.setTipoDocumento(referencia.getTipoDocumento());
        dto.setNumeroDocumento(referencia.getNumeroDocumento());
        dto.setTelefono(referencia.getTelefono());
        dto.setEmail(referencia.getEmail());
        dto.setTipoOcupante(determinarTipoConsolidado(tipos));

        List<Reserva> reservas = reservasPorDocumento.getOrDefault(documento, List.of());
        dto.setReservas(reservas.stream()
                .sorted(Comparator.comparing(Reserva::getFechaCreacion).reversed())
                .map(this::mapearReservaResumen)
                .toList());

        List<Estancia> estancias = estanciasPorDocumento.getOrDefault(documento, List.of());
        dto.setEstancias(estancias.stream()
                .sorted(Comparator.comparing(Estancia::getSalidaEstimada, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::mapearEstanciaResumen)
                .toList());

        return dto;
    }

    private Map<String, List<Reserva>> construirReservasPorDocumento(Set<String> documentos) {
        if (documentos.isEmpty()) {
            return Map.of();
        }
        return reservaRepository.findByClienteNumeroDocumentoIn(documentos).stream()
                .filter(reserva -> reserva.getCliente() != null)
                .filter(reserva -> reserva.getCliente().getNumeroDocumento() != null)
                .collect(Collectors.groupingBy(
                        reserva -> normalizarDocumento(reserva.getCliente().getNumeroDocumento())));
    }

    private Map<String, List<Estancia>> construirEstanciasPorDocumento(Set<String> documentos) {
        if (documentos.isEmpty()) {
            return Map.of();
        }
        Map<String, List<Estancia>> resultado = new LinkedHashMap<>();
        List<Estancia> estancias = estanciaRepository.findByOcupantesNumeroDocumentoInAndEstadoNot(
                documentos, EstadoEstancia.RESERVADA);

        for (Estancia estancia : estancias) {
            for (Ocupante ocupante : estancia.getOcupantes()) {
                String documento = normalizarDocumento(ocupante.getNumeroDocumento());
                if (!documentos.contains(documento)) {
                    continue;
                }
                resultado.computeIfAbsent(documento, key -> new ArrayList<>());
                List<Estancia> lista = resultado.get(documento);
                boolean yaExiste = lista.stream().anyMatch(item -> item.getId().equals(estancia.getId()));
                if (!yaExiste) {
                    lista.add(estancia);
                }
            }
        }
        return resultado;
    }

    private ClienteReservaResumenDTO mapearReservaResumen(Reserva reserva) {
        ClienteReservaResumenDTO dto = new ClienteReservaResumenDTO();
        dto.setId(reserva.getId());
        dto.setCodigoReserva(reserva.getCodigo());
        dto.setEstado(reserva.getEstado());
        dto.setEntradaEstimada(reserva.getEntradaEstimada());
        dto.setSalidaEstimada(reserva.getSalidaEstimada());
        return dto;
    }

    private ClienteEstanciaResumenDTO mapearEstanciaResumen(Estancia estancia) {
        ClienteEstanciaResumenDTO dto = new ClienteEstanciaResumenDTO();
        dto.setId(estancia.getId());
        dto.setCodigoEstancia(estancia.getCodigoFolio());
        dto.setEstado(estancia.getEstado());
        dto.setEntradaReal(estancia.getEntradaReal());
        dto.setSalidaEstimada(estancia.getSalidaEstimada());
        dto.setSalidaReal(estancia.getSalidaReal());
        return dto;
    }

    private String determinarTipoConsolidado(Set<TipoOcupante> tipos) {
        if (tipos.contains(TipoOcupante.CLIENTE) && tipos.contains(TipoOcupante.ACOMPANANTE)) {
            return "CLIENTE_Y_ACOMPANANTE";
        }
        if (tipos.contains(TipoOcupante.CLIENTE)) {
            return "CLIENTE";
        }
        return "ACOMPANANTE";
    }

    private String construirLlaveConsolidacion(Ocupante ocupante) {
        String documento = normalizarDocumento(ocupante.getNumeroDocumento());
        if (!documento.isBlank()) {
            return documento;
        }
        return "id:" + ocupante.getId();
    }

    private String normalizarDocumento(String numeroDocumento) {
        return numeroDocumento == null ? "" : numeroDocumento.trim().toLowerCase();
    }

}
