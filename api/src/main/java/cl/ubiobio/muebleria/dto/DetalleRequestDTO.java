package cl.ubiobio.muebleria.dto;

import lombok.Data;

import java.util.List;

@Data
public class DetalleRequestDTO {
  private Integer idMueble;
  private Integer cantidad;
  private List<Integer> idsVariantes; // IDs de variantes a aplicar
}
