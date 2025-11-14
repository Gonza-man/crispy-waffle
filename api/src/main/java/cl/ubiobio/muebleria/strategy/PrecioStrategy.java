package cl.ubiobio.muebleria.strategy;

/**
 * Strategy Pattern: Interface para calcular el costo de una variante
 */
public interface PrecioStrategy {
  /**
   * Calcula el costo adicional de una variante
   * @param costoExtra El valor del costo extra (puede ser fijo o porcentaje)
   * @param precioBase El precio base del mueble
   * @return El costo calculado en CLP (Integer)
   */
  Integer calcularCosto(Integer costoExtra, Integer precioBase);
}
