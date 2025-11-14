package cl.ubiobio.muebleria.strategy;

import org.springframework.stereotype.Component;

/**
 * Strategy Pattern: Implementación para costo FIJO
 * El costoExtra representa un monto fijo en CLP
 */
@Component
public class PrecioFijoStrategy implements PrecioStrategy {

  @Override
  public Integer calcularCosto(Integer costoExtra, Integer precioBase) {
    // Para FIJO, simplemente retornamos el costo extra (no depende del precio base)
    return costoExtra;
  }
}
