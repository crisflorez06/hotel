package com.hotel.models;

import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "reservas",
        indexes = {
                @Index(name = "idx_reservas_ocupante", columnList = "id_ocupante")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ocupante", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Ocupante ocupante;

    @Column(name = "numero_personas", nullable = false)
    private Integer numeroPersonas;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "entrada_estimada", nullable = false)
    private LocalDateTime entradaEstimada;

    @Column(name = "salida_estimada", nullable = false)
    private LocalDateTime salidaEstimada;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_ocupacion", nullable = false, length = 20)
    private ModoOcupacion modoOcupacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_reserva", nullable = false, length = 30)
    private CanalReserva canalReserva;

    @Column(columnDefinition = "text")
    private String notas;

    @ManyToMany
    @JoinTable(
            name = "reserva_habitaciones",
            joinColumns = @JoinColumn(name = "id_reserva"),
            inverseJoinColumns = @JoinColumn(name = "id_habitacion")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Habitacion> habitaciones;

    @OneToOne(mappedBy = "reserva")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Estancia estancia;

    @OneToOne(mappedBy = "reserva")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pago pago;
}
