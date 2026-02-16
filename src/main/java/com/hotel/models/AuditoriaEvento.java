package com.hotel.models;

import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auditoria_eventos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoEntidad entidad;

    @Column(name = "id_entidad", nullable = false)
    private Long idEntidad;

    @Column(name = "codigo_estancia", length = 30)
    private String codigoEstancia;

    @Column(name = "codigo_reserva", length = 30)
    private String codigoReserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEvento tipoEvento;

    @Column(columnDefinition = "JSON")
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
        validarCodigos();
    }

    @PreUpdate
    protected void onUpdate() {
        validarCodigos();
    }

    private void validarCodigos() {
        boolean sinCodigoEstancia = codigoEstancia == null || codigoEstancia.isBlank();
        boolean sinCodigoReserva = codigoReserva == null || codigoReserva.isBlank();
        if (sinCodigoEstancia && sinCodigoReserva) {
            throw new IllegalStateException("AuditoriaEvento requiere codigoEstancia o codigoReserva");
        }
    }
}
