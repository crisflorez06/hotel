package com.hotel.testdata;

import com.hotel.models.Ocupante;
import com.hotel.models.enums.TipoDocumento;
import com.hotel.models.enums.TipoOcupante;
import com.hotel.repositories.OcupanteRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

public final class OcupanteTestData {

    private OcupanteTestData() {
    }

    public static Ocupante clienteData() {
        Ocupante ocupante = new Ocupante();
        ocupante.setNombres("Juan");
        ocupante.setApellidos("Perez");
        ocupante.setTipoDocumento(TipoDocumento.CC);
        ocupante.setNumeroDocumento("123456789");
        ocupante.setTelefono("3000000000");
        ocupante.setEmail("juan.perez@example.com");
        ocupante.setTipoOcupante(TipoOcupante.CLIENTE);
        ocupante.setCreadoEn(LocalDateTime.now());
        return ocupante;
    }

    public static Ocupante clienteEditarData() {
        Ocupante ocupante = new Ocupante();
        ocupante.setNombres("Carlos");
        ocupante.setApellidos("Gomez");
        ocupante.setTipoDocumento(TipoDocumento.CC);
        ocupante.setNumeroDocumento("987654321");
        ocupante.setTelefono("1230000000");
        ocupante.setEmail("carlosgomez@example.com");
        ocupante.setTipoOcupante(TipoOcupante.CLIENTE);
        ocupante.setCreadoEn(LocalDateTime.now());
        return ocupante;
    }

    public static List<Ocupante> acompanantesDataConCliente() {

        Ocupante cliente = clienteData();

        Ocupante a1 = new Ocupante();
        a1.setNombres("Ana");
        a1.setApellidos("Lopez");
        a1.setTipoDocumento(TipoDocumento.CC);
        a1.setNumeroDocumento("987654321");
        a1.setTelefono("3010000000");
        a1.setEmail("ana.lopez@example.com");
        a1.setTipoOcupante(TipoOcupante.ACOMPANANTE);
        a1.setCreadoEn(LocalDateTime.now());

        Ocupante a2 = new Ocupante();
        a2.setNombres("Luis");
        a2.setApellidos("Gomez");
        a2.setTipoDocumento(TipoDocumento.CE);
        a2.setNumeroDocumento("555666777");
        a2.setTelefono("3020000000");
        a2.setEmail("luis.gomez@example.com");
        a2.setTipoOcupante(TipoOcupante.ACOMPANANTE);
        a2.setCreadoEn(LocalDateTime.now());

        return List.of(a1, a2, cliente);
    }

    public static List<Ocupante> acompanantesData() {

        Ocupante a1 = new Ocupante();
        a1.setNombres("Ana");
        a1.setApellidos("Lopez");
        a1.setTipoDocumento(TipoDocumento.CC);
        a1.setNumeroDocumento("987654321");
        a1.setTelefono("3010000000");
        a1.setEmail("ana.lopez@example.com");
        a1.setTipoOcupante(TipoOcupante.ACOMPANANTE);
        a1.setCreadoEn(LocalDateTime.now());

        Ocupante a2 = new Ocupante();
        a2.setNombres("Luis");
        a2.setApellidos("Gomez");
        a2.setTipoDocumento(TipoDocumento.CE);
        a2.setNumeroDocumento("555666777");
        a2.setTelefono("3020000000");
        a2.setEmail("luis.gomez@example.com");
        a2.setTipoOcupante(TipoOcupante.ACOMPANANTE);
        a2.setCreadoEn(LocalDateTime.now());

        return List.of(a1, a2);
    }

    public static List<Ocupante> acompanantesDataEditar() {

        Ocupante a1 = new Ocupante();
        a1.setNombres("Maria");
        a1.setApellidos("Lopez");
        a1.setTipoDocumento(TipoDocumento.CC);
        a1.setNumeroDocumento("987654321");
        a1.setTelefono("3010000000");
        a1.setEmail("marialopez@example.com");
        a1.setTipoOcupante(TipoOcupante.ACOMPANANTE);
        a1.setCreadoEn(LocalDateTime.now());

        return List.of(a1);
    }
}
