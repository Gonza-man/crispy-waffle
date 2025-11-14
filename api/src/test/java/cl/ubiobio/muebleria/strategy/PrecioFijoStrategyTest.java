package cl.ubiobio.muebleria.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PrecioFijoStrategy (Strategy Pattern)
 * Tests that fixed costs are added correctly regardless of base price
 */
@DisplayName("Strategy Pattern - Precio Fijo Tests")
class PrecioFijoStrategyTest {

    private PrecioFijoStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PrecioFijoStrategy();
    }

    @Test
    @DisplayName("Should add fixed cost of 5000 CLP")
    void testCalcularCostoFijo() {
        // Given
        Integer costoExtra = 5000;
        Integer precioBase = 15000;

        // When
        Integer resultado = strategy.calcularCosto(costoExtra, precioBase);

        // Then
        assertEquals(5000, resultado, "FIJO strategy should return exact costo extra");
    }

    @Test
    @DisplayName("Should not depend on base price")
    void testNoDepeneDePrecioBase() {
        // Given
        Integer costoExtra = 5000;

        // When - Try with different base prices
        Integer resultado1 = strategy.calcularCosto(costoExtra, 10000);
        Integer resultado2 = strategy.calcularCosto(costoExtra, 50000);
        Integer resultado3 = strategy.calcularCosto(costoExtra, 100000);

        // Then - All should return same fixed cost
        assertEquals(5000, resultado1);
        assertEquals(5000, resultado2);
        assertEquals(5000, resultado3);
    }

    @Test
    @DisplayName("Should handle zero cost")
    void testCostoCero() {
        // Given
        Integer costoExtra = 0;
        Integer precioBase = 15000;

        // When
        Integer resultado = strategy.calcularCosto(costoExtra, precioBase);

        // Then
        assertEquals(0, resultado);
    }

    @Test
    @DisplayName("Should handle large fixed costs")
    void testCostoGrande() {
        // Given
        Integer costoExtra = 1000000; // 1 million CLP
        Integer precioBase = 15000;

        // When
        Integer resultado = strategy.calcularCosto(costoExtra, precioBase);

        // Then
        assertEquals(1000000, resultado);
    }
}
