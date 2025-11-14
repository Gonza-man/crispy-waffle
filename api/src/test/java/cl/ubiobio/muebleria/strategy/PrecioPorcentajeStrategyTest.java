package cl.ubiobio.muebleria.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PrecioPorcentajeStrategy (Strategy Pattern)
 * Tests that percentage costs are calculated correctly based on base price
 */
@DisplayName("Strategy Pattern - Precio Porcentaje Tests")
class PrecioPorcentajeStrategyTest {

    private PrecioPorcentajeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PrecioPorcentajeStrategy();
    }

    @Test
    @DisplayName("Should calculate 10% of 15000 = 1500")
    void testCalcularCostoPorcentaje10() {
        // Given
        Integer costoExtra = 10; // 10%
        Integer precioBase = 15000;

        // When
        Integer resultado = strategy.calcularCosto(costoExtra, precioBase);

        // Then
        assertEquals(1500, resultado, "10% of 15000 should be 1500");
    }

    @Test
    @DisplayName("Should calculate 20% of 50000 = 10000")
    void testCalcularCostoPorcentaje20() {
        // Given
        Integer costoExtra = 20; // 20%
        Integer precioBase = 50000;

        // When
        Integer resultado = strategy.calcularCosto(costoExtra, precioBase);

        // Then
        assertEquals(10000, resultado, "20% of 50000 should be 10000");
    }

    @Test
    @DisplayName("Should handle 0% correctly")
    void testPorcentajeCero() {
        // Given
        Integer costoExtra = 0; // 0%
        Integer precioBase = 15000;

        // When
        Integer resultado = strategy.calcularCosto(costoExtra, precioBase);

        // Then
        assertEquals(0, resultado, "0% should return 0");
    }

    @Test
    @DisplayName("Should handle 100% correctly")
    void testPorcentaje100() {
        // Given
        Integer costoExtra = 100; // 100%
        Integer precioBase = 15000;

        // When
        Integer resultado = strategy.calcularCosto(costoExtra, precioBase);

        // Then
        assertEquals(15000, resultado, "100% should return same as base price");
    }

    @Test
    @DisplayName("Should depend on base price")
    void testDepeneDePrecioBase() {
        // Given
        Integer costoExtra = 10; // 10%

        // When - Different base prices
        Integer resultado1 = strategy.calcularCosto(costoExtra, 10000);
        Integer resultado2 = strategy.calcularCosto(costoExtra, 20000);
        Integer resultado3 = strategy.calcularCosto(costoExtra, 30000);

        // Then - Results should be proportional
        assertEquals(1000, resultado1);
        assertEquals(2000, resultado2);
        assertEquals(3000, resultado3);
    }

    @Test
    @DisplayName("Should handle decimal results (truncated)")
    void testResultadoDecimal() {
        // Given
        Integer costoExtra = 15; // 15%
        Integer precioBase = 100;

        // When
        Integer resultado = strategy.calcularCosto(costoExtra, precioBase);

        // Then - 15% of 100 = 15 (not 15.0)
        assertEquals(15, resultado);
    }
}
