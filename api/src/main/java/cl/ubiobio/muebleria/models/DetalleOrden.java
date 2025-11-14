package cl.ubiobio.muebleria.models;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "detalles_orden")
public class DetalleOrden {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer idDetalle;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_orden", nullable = false)
  private Orden orden;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_mueble", nullable = false)
  private Mueble mueble;

  private Integer cantidad = 1;

  // SNAPSHOT: Precio congelado en CLP
  @Column(name = "precio_unitario_final")
  private Integer precioUnitarioFinal;

  // Relación con variantes aplicadas
  @OneToMany(mappedBy = "detalle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DetalleOrdenVariante> variantesAplicadas = new ArrayList<>();
}
