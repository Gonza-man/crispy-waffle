package cl.ubiobio.muebleria.models;

import cl.ubiobio.muebleria.enums.EstadoOrden;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "ordenes")
public class Orden {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer idOrden;

  @Column(name = "fecha_creacion")
  private LocalDateTime fechaCreacion = LocalDateTime.now();

  @Column(name = "fecha_confirmacion")
  private LocalDateTime fechaConfirmacion; // Nullable

  // Hook para State Pattern
  @Enumerated(EnumType.STRING)
  @Column(name = "estado_orden", nullable = false, length = 20)
  private EstadoOrden estadoOrden = EstadoOrden.COTIZACION;

  // BigInt para evitar overflow en totales acumulados
  @Column(name = "total_calculado")
  private Long totalCalculado;

  // Relación con Usuario (dueño de la orden)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_usuario", nullable = false)
  private Usuario usuario;

  // Relación con los detalles (Cascade para guardar todo junto)
  @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DetalleOrden> detalles = new ArrayList<>();

  // Método helper para mantener consistencia bidireccional
  public void addDetalle(DetalleOrden detalle) {
    detalles.add(detalle);
    detalle.setOrden(this);
  }
}
