package com.hotel.models;

import com.hotel.models.enums.TipoUnidad;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tarifas_base")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TarifaBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_unidad", nullable = false, length = 25)
    private TipoUnidad tipoUnidad;

    @Column(name = "precio_dia_tem_baja", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioDiaTemBaja;

    @Column(name = "precio_dia_tem_alta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioDiaTemAlta;

    @Column(name = "precio_estadia_corta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioEstadiaCorta;

    @Column(name = "precio_persona_adicional_tem_baja", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioPersonaAdicionalTemBaja;

    @Column(name = "precio_persona_adicional_tem_alta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioPersonaAdicionalTemAlta;

    @Column(name = "precio_estadia_persona_adicional_corta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioEstadiaPersonaAdicionalCorta;


    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
