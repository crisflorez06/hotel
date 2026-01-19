package com.hotel.models;

import com.hotel.models.enums.ModoOcupacion;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "estancias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Estancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_folio", nullable = false, unique = true, length = 30)
    private String codigoFolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Cliente cliente;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "entrada_real", nullable = false)
    private LocalDateTime entradaReal;

    @Column(name = "salida_estimado", nullable = false)
    private LocalDateTime salidaEstimada;

    @Column(name = "salida_real")
    private LocalDateTime salidaReal;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_ocupacion", nullable = false, length = 20)
    private ModoOcupacion modoOcupacion;


    @Column(name = "precio_total",  precision = 12, scale = 2)
    private BigDecimal precioTotal;

    @Column(columnDefinition = "text")
    private String notas;

    @OneToMany(mappedBy = "estancia", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<EstanciaHabitacion> estanciaHabitaciones;

    @OneToOne(mappedBy = "estancia")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pago pago;

    @OneToMany(mappedBy = "estancia", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<EstanciaAcompanante> estanciaAcompanantes;

    private boolean activo;
}
