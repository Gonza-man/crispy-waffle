package cl.ubiobio.muebleria.models;

import cl.ubiobio.muebleria.enums.TipoAplicacion;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "variantes_adicionales")
public class VarianteAdicional {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer idVariante;

  @Column(nullable = false, length = 100)
  private String nombre;

  // CLP: Si es FIJO, es dinero (5000). Si es PORCENTAJE, es entero (10 = 10%).
  @Column(name = "costo_extra", nullable = false)
  private Integer costoExtra;

  // Hook para Strategy Pattern
  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_aplicacion", nullable = false, length = 20)
  private TipoAplicacion tipoAplicacion;

  private Boolean activo = true;
}
