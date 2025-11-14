package cl.ubiobio.muebleria.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "detalles_orden_variantes")
public class DetalleOrdenVariante {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer idDetalleVariante;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_detalle", nullable = false)
  private DetalleOrden detalle;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_variante", nullable = false)
  private VarianteAdicional variante;

  // SNAPSHOT: Precio específico de la variante en ese momento
  @Column(name = "precio_aplicado_snapshot")
  private Integer precioAplicadoSnapshot;
}
