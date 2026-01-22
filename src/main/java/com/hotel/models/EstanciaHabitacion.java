package com.hotel.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "estancia_habitaciones",
        indexes = {
                @Index(name = "idx_est_hab_estancia", columnList = "id_estancia"),
                @Index(name = "idx_est_hab_habitacion", columnList = "id_habitacion")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstanciaHabitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estancia", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Estancia estancia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_habitacion", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Habitacion habitacion;

    private Boolean estadoOcupacion;

}
