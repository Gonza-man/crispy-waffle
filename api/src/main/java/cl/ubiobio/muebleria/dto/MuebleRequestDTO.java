package cl.ubiobio.muebleria.dto;

import cl.ubiobio.muebleria.enums.TamanoMueble;
import lombok.Data;

@Data
public class MuebleRequestDTO {
  private String nombre;
  private String tipo;
  private Integer precioBase;
  private Integer stock;
  private TamanoMueble tamano;
  private String material;
}
