package com.hotel.models;

import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.MedioPago;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "pagos",
        indexes = {
                @Index(name = "idx_pagos_reserva", columnList = "id_reserva"),
                @Index(name = "idx_pagos_estancia", columnList = "id_estancia")
        }
)
@Check(constraints = "(id_estancia IS NULL) <> (id_reserva IS NULL)")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Puede ser null si el pago es de una RESERVA
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "id_estancia", nullable = true, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Estancia estancia;

    // Puede ser null si el pago es de una ESTANCIA
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "id_reserva", nullable = true, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Reserva reserva;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false, length = 20)
    private MedioPago medioPago;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;
}
