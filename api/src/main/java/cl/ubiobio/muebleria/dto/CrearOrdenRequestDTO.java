package cl.ubiobio.muebleria.dto;

import lombok.Data;

import java.util.List;

@Data
public class CrearOrdenRequestDTO {
  private List<DetalleRequestDTO> detalles;
}
