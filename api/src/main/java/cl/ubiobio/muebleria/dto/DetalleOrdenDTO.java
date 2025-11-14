package cl.ubiobio.muebleria.dto;

import lombok.Data;

import java.util.List;

@Data
public class DetalleOrdenDTO {
  private Integer idDetalle;
  private Integer idMueble;
  private String nombreMueble;
  private Integer cantidad;
  private Integer precioUnitarioFinal;
  private List<VarianteAplicadaDTO> variantes;
}
