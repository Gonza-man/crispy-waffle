package cl.ubiobio.muebleria.strategy;

import cl.ubiobio.muebleria.enums.TipoAplicacion;
import org.springframework.stereotype.Component;

/**
 * Factory para obtener la estrategia correcta según el TipoAplicacion
 */
@Component
public class PrecioStrategyFactory {

  private final PrecioFijoStrategy precioFijoStrategy;
  private final PrecioPorcentajeStrategy precioPorcentajeStrategy;

  public PrecioStrategyFactory(PrecioFijoStrategy precioFijoStrategy,
                                PrecioPorcentajeStrategy precioPorcentajeStrategy) {
    this.precioFijoStrategy = precioFijoStrategy;
    this.precioPorcentajeStrategy = precioPorcentajeStrategy;
  }

  /**
   * Retorna la estrategia correcta según el tipo de aplicación
   */
  public PrecioStrategy getStrategy(TipoAplicacion tipoAplicacion) {
    return switch (tipoAplicacion) {
      case FIJO -> precioFijoStrategy;
      case PORCENTAJE -> precioPorcentajeStrategy;
    };
  }
}
