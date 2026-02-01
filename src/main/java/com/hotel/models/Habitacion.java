package com.hotel.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import com.hotel.models.enums.EstadoOperativo;

@Entity
@Table(name = "habitaciones",
        indexes = {
                @Index(name = "idx_habitaciones_unidad", columnList = "id_unidad")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Unidad unidad;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo", nullable = false, length = 20)
    private EstadoOperativo estadoOperativo;

    @ManyToMany(mappedBy = "habitaciones")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Reserva> reservas;

    @ManyToMany(mappedBy = "habitaciones")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Estancia> estancias;
}
