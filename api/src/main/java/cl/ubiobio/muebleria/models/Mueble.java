package cl.ubiobio.muebleria.models;

import cl.ubiobio.muebleria.enums.TamanoMueble;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "muebles")
public class Mueble {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer idMueble;

  @Column(nullable = false, length = 100)
  private String nombre;

  private String tipo;

  // CLP: Usamos Integer. Ej: 15000
  @Column(name = "precio_base", nullable = false)
  private Integer precioBase;

  @Column(nullable = false)
  private Integer stock;

  @Column(name = "estado_logico")
  private Boolean estadoLogico = true; // Soft Delete

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TamanoMueble tamano;

  private String material;
}
