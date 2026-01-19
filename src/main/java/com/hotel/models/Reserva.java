package com.hotel.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "reservas")
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
    @JoinColumn(name = "id_cliente", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Cliente cliente;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "entrada_estimada", nullable = false)
    private LocalDateTime entradaEstimada;

    @Column(name = "salida_estimada", nullable = false)
    private LocalDateTime salidaEstimada;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "canal_reserva", nullable = false, length = 30)
    private String canalReserva;

    @Column(columnDefinition = "text")
    private String observaciones;

    @OneToMany(mappedBy = "reserva")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ReservaHabitacion> reservaHabitaciones;

    @OneToMany(mappedBy = "reserva")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Estancia> estancias;

    @OneToMany(mappedBy = "reserva")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Pago> pagos;
}
