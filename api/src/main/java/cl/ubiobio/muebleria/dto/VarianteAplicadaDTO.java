package cl.ubiobio.muebleria.dto;

import lombok.Data;

@Data
public class VarianteAplicadaDTO {
  private Integer idVariante;
  private String nombre;
  private Integer precioAplicadoSnapshot;
}
