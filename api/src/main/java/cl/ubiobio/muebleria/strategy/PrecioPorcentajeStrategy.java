package cl.ubiobio.muebleria.strategy;

import org.springframework.stereotype.Component;

/**
 * Strategy Pattern: Implementación para costo PORCENTAJE
 * El costoExtra representa un porcentaje (ej: 10 = 10%)
 */
@Component
public class PrecioPorcentajeStrategy implements PrecioStrategy {

  @Override
  public Integer calcularCosto(Integer costoExtra, Integer precioBase) {
    // Para PORCENTAJE, calculamos el porcentaje del precio base
    // costoExtra = 10 significa 10%
    // Resultado: (precioBase * porcentaje) / 100
    return (precioBase * costoExtra) / 100;
  }
}
