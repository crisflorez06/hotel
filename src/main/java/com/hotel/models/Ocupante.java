package com.hotel.models;

import com.hotel.models.enums.TipoDocumento;
import com.hotel.models.enums.TipoOcupante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ocupantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ocupante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombres;

    @Column(nullable = false, length = 120)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 20)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", length = 40)
    private String numeroDocumento;

    @Column(length = 30)
    private String telefono;

    @Column(length = 180)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ocupante", nullable = false, length = 20)
    private TipoOcupante tipoOcupante;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
