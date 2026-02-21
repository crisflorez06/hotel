package com.hotel.models;

import com.hotel.models.enums.EstadoEstancia;
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
@Table(name = "estancias",
        indexes = {
                @Index(name = "idx_estancias_reserva", columnList = "id_reserva")
        }
)
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

    @OneToOne(fetch = FetchType.LAZY, optional = true, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "id_reserva", unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Reserva reserva;

    @ManyToMany
    @JoinTable(
            name = "estancia_ocupantes",
            joinColumns = @JoinColumn(name = "id_estancia"),
            inverseJoinColumns = @JoinColumn(name = "id_ocupante")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Ocupante> ocupantes;


    @Column(name = "entrada_real")
    private LocalDateTime entradaReal;

    @Column(name = "salida_estimado")
    private LocalDateTime salidaEstimada;

    @Column(name = "salida_real")
    private LocalDateTime salidaReal;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_ocupacion", nullable = false, length = 20)
    private ModoOcupacion modoOcupacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEstancia estado;


    @Column(name = "precio_total",  precision = 12, scale = 2)
    private BigDecimal precioTotal;

    @Column(columnDefinition = "text")
    private String notas;

    @ManyToMany
    @JoinTable(
            name = "estancia_habitaciones",
            joinColumns = @JoinColumn(name = "id_estancia"),
            inverseJoinColumns = @JoinColumn(name = "id_habitacion")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Habitacion> habitaciones;

    @OneToMany(mappedBy = "estancia", fetch = FetchType.LAZY)
    @OrderBy("fecha DESC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Pago> pagos;

}
