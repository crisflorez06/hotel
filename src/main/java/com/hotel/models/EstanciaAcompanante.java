package com.hotel.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "estancia_acompanante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstanciaAcompanante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estancia", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Estancia estancia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_acompanante", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Acompanante acompanante;

    @Column(name = "estado_ocupacion", nullable = false)
    private Boolean estadoOcupacion;
}
