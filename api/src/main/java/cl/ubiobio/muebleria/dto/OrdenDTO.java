package cl.ubiobio.muebleria.dto;

import cl.ubiobio.muebleria.enums.EstadoOrden;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrdenDTO {
  private Integer idOrden;
  private LocalDateTime fechaCreacion;
  private LocalDateTime fechaConfirmacion;
  private EstadoOrden estadoOrden;
  private Long totalCalculado;
  private List<DetalleOrdenDTO> detalles;
}
