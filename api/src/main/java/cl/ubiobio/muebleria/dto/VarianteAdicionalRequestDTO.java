package cl.ubiobio.muebleria.dto;

import cl.ubiobio.muebleria.enums.TipoAplicacion;
import lombok.Data;

@Data
public class VarianteAdicionalRequestDTO {
  private String nombre;
  private Integer costoExtra;
  private TipoAplicacion tipoAplicacion;
}
