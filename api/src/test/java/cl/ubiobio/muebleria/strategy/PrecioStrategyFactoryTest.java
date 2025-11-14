package cl.ubiobio.muebleria.strategy;

import cl.ubiobio.muebleria.enums.TipoAplicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PrecioStrategyFactory (Strategy Pattern)
 * Tests that correct strategy is returned based on TipoAplicacion
 */
@DisplayName("Strategy Pattern - Factory Tests")
class PrecioStrategyFactoryTest {

    private PrecioStrategyFactory factory;
    private PrecioFijoStrategy precioFijoStrategy;
    private PrecioPorcentajeStrategy precioPorcentajeStrategy;

    @BeforeEach
    void setUp() {
        precioFijoStrategy = new PrecioFijoStrategy();
        precioPorcentajeStrategy = new PrecioPorcentajeStrategy();
        factory = new PrecioStrategyFactory(precioFijoStrategy, precioPorcentajeStrategy);
    }

    @Test
    @DisplayName("Should return PrecioFijoStrategy for FIJO")
    void testGetStrategyFijo() {
        // When
        PrecioStrategy strategy = factory.getStrategy(TipoAplicacion.FIJO);

        // Then
        assertNotNull(strategy);
        assertInstanceOf(PrecioFijoStrategy.class, strategy);
    }

    @Test
    @DisplayName("Should return PrecioPorcentajeStrategy for PORCENTAJE")
    void testGetStrategyPorcentaje() {
        // When
        PrecioStrategy strategy = factory.getStrategy(TipoAplicacion.PORCENTAJE);

        // Then
        assertNotNull(strategy);
        assertInstanceOf(PrecioPorcentajeStrategy.class, strategy);
    }

    @Test
    @DisplayName("Should return same instance for multiple calls (singleton behavior)")
    void testSameInstance() {
        // When
        PrecioStrategy strategy1 = factory.getStrategy(TipoAplicacion.FIJO);
        PrecioStrategy strategy2 = factory.getStrategy(TipoAplicacion.FIJO);

        // Then
        assertSame(strategy1, strategy2, "Factory should return same instance");
    }
}
