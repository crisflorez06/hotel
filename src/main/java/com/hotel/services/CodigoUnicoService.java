package com.hotel.services;

import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.ReservaRepository;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class CodigoUnicoService {

    private static final String LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LONGITUD_NUMERICA = 5;
    private static final int MAX_REINTENTOS = 10;

    private final ReservaRepository reservaRepository;
    private final EstanciaRepository estanciaRepository;

    public CodigoUnicoService(ReservaRepository reservaRepository, EstanciaRepository estanciaRepository) {
        this.reservaRepository = reservaRepository;
        this.estanciaRepository = estanciaRepository;
    }

    public String generarCodigoReserva() {
        for (int i = 0; i < MAX_REINTENTOS; i++) {
            String codigo = "RES-" + generarSufijo();
            if (!reservaRepository.existsByCodigo(codigo)) {
                return codigo;
            }
        }
        throw new IllegalStateException("No se pudo generar un codigo unico para reserva");
    }

    public String generarCodigoEstancia() {
        for (int i = 0; i < MAX_REINTENTOS; i++) {
            String codigo = "EST-" + generarSufijo();
            if (!estanciaRepository.existsByCodigoFolio(codigo)) {
                return codigo;
            }
        }
        throw new IllegalStateException("No se pudo generar un codigo unico para estancia");
    }

    private String generarSufijo() {
        StringBuilder builder = new StringBuilder(LONGITUD_NUMERICA + 1);
        int primerDigito = ThreadLocalRandom.current().nextInt(1, 10);
        builder.append(primerDigito);
        for (int i = 1; i < LONGITUD_NUMERICA; i++) {
            int digito = ThreadLocalRandom.current().nextInt(10);
            builder.append(digito);
        }
        int indexLetra = ThreadLocalRandom.current().nextInt(LETRAS.length());
        builder.append(LETRAS.charAt(indexLetra));
        return builder.toString();
    }
}
