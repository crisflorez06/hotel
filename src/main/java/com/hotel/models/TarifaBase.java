package com.hotel.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "Unidad", length = 20)
    private String unidad;

    @Column(name = "precio_dia_tem_baja", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioDiaTemBaja;

    @Column(name = "precio_dia_tem_alta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioDiaTemAlta;

    @Column(name = "precio_estadia_corta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioEstadiaCorta;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
