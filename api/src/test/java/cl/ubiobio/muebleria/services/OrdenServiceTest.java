package cl.ubiobio.muebleria.services;

import cl.ubiobio.muebleria.dto.CrearOrdenRequestDTO;
import cl.ubiobio.muebleria.dto.DetalleRequestDTO;
import cl.ubiobio.muebleria.dto.OrdenDTO;
import cl.ubiobio.muebleria.enums.EstadoOrden;
import cl.ubiobio.muebleria.enums.TipoAplicacion;
import cl.ubiobio.muebleria.models.*;
import cl.ubiobio.muebleria.repositories.MuebleRepository;
import cl.ubiobio.muebleria.repositories.OrdenRepository;
import cl.ubiobio.muebleria.repositories.VarianteAdicionalRepository;
import cl.ubiobio.muebleria.strategy.PrecioStrategyFactory;
import cl.ubiobio.muebleria.strategy.PrecioFijoStrategy;
import cl.ubiobio.muebleria.strategy.PrecioPorcentajeStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Comprehensive tests for OrdenService
 * Tests all four design patterns:
 * - State Pattern (order states)
 * - Decorator Pattern (variants on furniture)
 * - Strategy Pattern (price calculation)
 * - Snapshot Pattern (price freezing)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrdenService - All Design Patterns Tests")
class OrdenServiceTest {

    @Mock
    private OrdenRepository ordenRepository;

    @Mock
    private MuebleRepository muebleRepository;

    @Mock
    private VarianteAdicionalRepository varianteRepository;

    @Mock
    private PrecioStrategyFactory precioStrategyFactory;

    @InjectMocks
    private OrdenService ordenService;

    private Mueble mueble;
    private VarianteAdicional varianteFijo;
    private VarianteAdicional variantePorcentaje;
    private Orden ordenCotizacion;
    private Orden ordenVenta;

    @BeforeEach
    void setUp() {
        // Setup real strategies for testing
        PrecioFijoStrategy fijoStrategy = new PrecioFijoStrategy();
        PrecioPorcentajeStrategy porcentajeStrategy = new PrecioPorcentajeStrategy();

        lenient().when(precioStrategyFactory.getStrategy(TipoAplicacion.FIJO)).thenReturn(fijoStrategy);
        lenient().when(precioStrategyFactory.getStrategy(TipoAplicacion.PORCENTAJE)).thenReturn(porcentajeStrategy);

        // Mueble base
        mueble = new Mueble();
        mueble.setIdMueble(1);
        mueble.setNombre("Silla");
        mueble.setPrecioBase(15000);

        // Variante FIJO
        varianteFijo = new VarianteAdicional();
        varianteFijo.setIdVariante(1);
        varianteFijo.setNombre("Lacado");
        varianteFijo.setCostoExtra(5000);
        varianteFijo.setTipoAplicacion(TipoAplicacion.FIJO);

        // Variante PORCENTAJE
        variantePorcentaje = new VarianteAdicional();
        variantePorcentaje.setIdVariante(2);
        variantePorcentaje.setNombre("Tapizado");
        variantePorcentaje.setCostoExtra(10); // 10%
        variantePorcentaje.setTipoAplicacion(TipoAplicacion.PORCENTAJE);

        // Orden en COTIZACION
        ordenCotizacion = new Orden();
        ordenCotizacion.setIdOrden(1);
        ordenCotizacion.setEstadoOrden(EstadoOrden.COTIZACION);
        ordenCotizacion.setFechaCreacion(LocalDateTime.now());

        // Orden en VENTA
        ordenVenta = new Orden();
        ordenVenta.setIdOrden(2);
        ordenVenta.setEstadoOrden(EstadoOrden.VENTA);
        ordenVenta.setFechaCreacion(LocalDateTime.now());
        ordenVenta.setFechaConfirmacion(LocalDateTime.now());
    }

    // ==================== STATE PATTERN TESTS ====================

    @Test
    @DisplayName("State Pattern: Should create orden in COTIZACION state")
    void testCrearOrdenEstadoCotizacion() {
        // Given
        when(muebleRepository.findById(1)).thenReturn(Optional.of(mueble));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> {
            Orden orden = invocation.getArgument(0);
            orden.setIdOrden(1);
            return orden;
        });

        DetalleRequestDTO detalleRequest = new DetalleRequestDTO();
        detalleRequest.setIdMueble(1);
        detalleRequest.setCantidad(1);
        detalleRequest.setIdsVariantes(Arrays.asList());

        CrearOrdenRequestDTO request = new CrearOrdenRequestDTO();
        request.setDetalles(Arrays.asList(detalleRequest));

        // When
        OrdenDTO resultado = ordenService.crearOrden(request);

        // Then
        assertNotNull(resultado);
        assertEquals(EstadoOrden.COTIZACION, resultado.getEstadoOrden());
        assertNull(resultado.getFechaConfirmacion(), "Cotizacion should not have fecha confirmacion");
        verify(ordenRepository, times(1)).save(any(Orden.class));
    }

    @Test
    @DisplayName("State Pattern: Should allow edit in COTIZACION state")
    void testAgregarDetalleEnCotizacion() {
        // Given
        when(ordenRepository.findById(1)).thenReturn(Optional.of(ordenCotizacion));
        when(muebleRepository.findById(1)).thenReturn(Optional.of(mueble));
        when(ordenRepository.save(any(Orden.class))).thenReturn(ordenCotizacion);

        DetalleRequestDTO detalleRequest = new DetalleRequestDTO();
        detalleRequest.setIdMueble(1);
        detalleRequest.setCantidad(1);
        detalleRequest.setIdsVariantes(Arrays.asList());

        // When
        OrdenDTO resultado = ordenService.agregarDetalle(1, detalleRequest);

        // Then
        assertNotNull(resultado);
        verify(ordenRepository, times(1)).save(any(Orden.class));
    }

    @Test
    @DisplayName("State Pattern: Should NOT allow edit in VENTA state")
    void testNoPermitirEditarEnVenta() {
        // Given
        when(ordenRepository.findById(2)).thenReturn(Optional.of(ordenVenta));

        DetalleRequestDTO detalleRequest = new DetalleRequestDTO();
        detalleRequest.setIdMueble(1);
        detalleRequest.setCantidad(1);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ordenService.agregarDetalle(2, detalleRequest);
        });

        assertTrue(exception.getMessage().contains("VENTA"));
        verify(ordenRepository, never()).save(any(Orden.class));
    }

    @Test
    @DisplayName("State Pattern: Should transition from COTIZACION to VENTA")
    void testConfirmarOrdenTransicion() {
        // Given
        DetalleOrden detalle = new DetalleOrden();
        detalle.setMueble(mueble);
        detalle.setCantidad(1);
        ordenCotizacion.addDetalle(detalle);

        when(ordenRepository.findById(1)).thenReturn(Optional.of(ordenCotizacion));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrdenDTO resultado = ordenService.confirmarOrden(1);

        // Then
        assertEquals(EstadoOrden.VENTA, resultado.getEstadoOrden());
        assertNotNull(resultado.getFechaConfirmacion());
        verify(ordenRepository, times(1)).save(any(Orden.class));
    }

    @Test
    @DisplayName("State Pattern: Should NOT allow confirm from VENTA state")
    void testNoPermitirConfirmarDesdeVenta() {
        // Given
        when(ordenRepository.findById(2)).thenReturn(Optional.of(ordenVenta));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ordenService.confirmarOrden(2);
        });

        assertTrue(exception.getMessage().contains("COTIZACION"));
    }

    @Test
    @DisplayName("State Pattern: Should allow cancel from any state")
    void testCancelarOrden() {
        // Given
        when(ordenRepository.findById(1)).thenReturn(Optional.of(ordenCotizacion));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrdenDTO resultado = ordenService.cancelarOrden(1);

        // Then
        assertEquals(EstadoOrden.CANCELADA, resultado.getEstadoOrden());
    }

    // ==================== DECORATOR PATTERN TESTS ====================

    @Test
    @DisplayName("Decorator Pattern: Should calculate base price without variants")
    void testCalculoPrecioBaseSinVariantes() {
        // Given
        when(muebleRepository.findById(1)).thenReturn(Optional.of(mueble));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> {
            Orden orden = invocation.getArgument(0);
            orden.setIdOrden(1);
            return orden;
        });

        DetalleRequestDTO detalleRequest = new DetalleRequestDTO();
        detalleRequest.setIdMueble(1);
        detalleRequest.setCantidad(2);
        detalleRequest.setIdsVariantes(Arrays.asList()); // Sin variantes

        CrearOrdenRequestDTO request = new CrearOrdenRequestDTO();
        request.setDetalles(Arrays.asList(detalleRequest));

        // When
        OrdenDTO resultado = ordenService.crearOrden(request);

        // Then
        // Base: 15000 × 2 = 30000
        assertEquals(30000L, resultado.getTotalCalculado());
    }

    @Test
    @DisplayName("Decorator Pattern: Should add FIJO variant cost to base")
    void testDecoradorConVarianteFijo() {
        // Given
        when(muebleRepository.findById(1)).thenReturn(Optional.of(mueble));
        when(varianteRepository.findById(1)).thenReturn(Optional.of(varianteFijo));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> {
            Orden orden = invocation.getArgument(0);
            orden.setIdOrden(1);
            return orden;
        });

        DetalleRequestDTO detalleRequest = new DetalleRequestDTO();
        detalleRequest.setIdMueble(1);
        detalleRequest.setCantidad(1);
        detalleRequest.setIdsVariantes(Arrays.asList(1)); // Lacado +5000

        CrearOrdenRequestDTO request = new CrearOrdenRequestDTO();
        request.setDetalles(Arrays.asList(detalleRequest));

        // When
        OrdenDTO resultado = ordenService.crearOrden(request);

        // Then
        // Base: 15000 + Lacado: 5000 = 20000
        assertEquals(20000L, resultado.getTotalCalculado());
    }

    @Test
    @DisplayName("Decorator Pattern: Should add PORCENTAJE variant cost to base")
    void testDecoradorConVariantePorcentaje() {
        // Given
        when(muebleRepository.findById(1)).thenReturn(Optional.of(mueble));
        when(varianteRepository.findById(2)).thenReturn(Optional.of(variantePorcentaje));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> {
            Orden orden = invocation.getArgument(0);
            orden.setIdOrden(1);
            return orden;
        });

        DetalleRequestDTO detalleRequest = new DetalleRequestDTO();
        detalleRequest.setIdMueble(1);
        detalleRequest.setCantidad(1);
        detalleRequest.setIdsVariantes(Arrays.asList(2)); // Tapizado 10%

        CrearOrdenRequestDTO request = new CrearOrdenRequestDTO();
        request.setDetalles(Arrays.asList(detalleRequest));

        // When
        OrdenDTO resultado = ordenService.crearOrden(request);

        // Then
        // Base: 15000 + Tapizado 10%: 1500 = 16500
        assertEquals(16500L, resultado.getTotalCalculado());
    }

    @Test
    @DisplayName("Decorator Pattern: Should apply multiple variants (chaining)")
    void testDecoradorMultiplesVariantes() {
        // Given
        when(muebleRepository.findById(1)).thenReturn(Optional.of(mueble));
        when(varianteRepository.findById(1)).thenReturn(Optional.of(varianteFijo));
        when(varianteRepository.findById(2)).thenReturn(Optional.of(variantePorcentaje));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> {
            Orden orden = invocation.getArgument(0);
            orden.setIdOrden(1);
            return orden;
        });

        DetalleRequestDTO detalleRequest = new DetalleRequestDTO();
        detalleRequest.setIdMueble(1);
        detalleRequest.setCantidad(2);
        detalleRequest.setIdsVariantes(Arrays.asList(1, 2)); // Ambas variantes

        CrearOrdenRequestDTO request = new CrearOrdenRequestDTO();
        request.setDetalles(Arrays.asList(detalleRequest));

        // When
        OrdenDTO resultado = ordenService.crearOrden(request);

        // Then
        // Base: 15000 + Lacado: 5000 + Tapizado 10%: 1500 = 21500 por unidad
        // Total: 21500 × 2 = 43000
        assertEquals(43000L, resultado.getTotalCalculado());
    }

    // ==================== STRATEGY PATTERN TESTS ====================

    @Test
    @DisplayName("Strategy Pattern: FIJO should add fixed amount")
    void testStrategyFijo() {
        // Given
        when(muebleRepository.findById(1)).thenReturn(Optional.of(mueble));
        when(varianteRepository.findById(1)).thenReturn(Optional.of(varianteFijo));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> {
            Orden orden = invocation.getArgument(0);
            orden.setIdOrden(1);
            return orden;
        });

        DetalleRequestDTO detalleRequest = new DetalleRequestDTO();
        detalleRequest.setIdMueble(1);
        detalleRequest.setCantidad(1);
        detalleRequest.setIdsVariantes(Arrays.asList(1));

        CrearOrdenRequestDTO request = new CrearOrdenRequestDTO();
        request.setDetalles(Arrays.asList(detalleRequest));

        // When
        OrdenDTO resultado = ordenService.crearOrden(request);

        // Then - Strategy FIJO adds exactly 5000
        assertEquals(20000L, resultado.getTotalCalculado()); // 15000 + 5000
    }

    @Test
    @DisplayName("Strategy Pattern: PORCENTAJE should calculate percentage")
    void testStrategyPorcentaje() {
        // Given
        when(muebleRepository.findById(1)).thenReturn(Optional.of(mueble));
        when(varianteRepository.findById(2)).thenReturn(Optional.of(variantePorcentaje));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> {
            Orden orden = invocation.getArgument(0);
            orden.setIdOrden(1);
            return orden;
        });

        DetalleRequestDTO detalleRequest = new DetalleRequestDTO();
        detalleRequest.setIdMueble(1);
        detalleRequest.setCantidad(1);
        detalleRequest.setIdsVariantes(Arrays.asList(2));

        CrearOrdenRequestDTO request = new CrearOrdenRequestDTO();
        request.setDetalles(Arrays.asList(detalleRequest));

        // When
        OrdenDTO resultado = ordenService.crearOrden(request);

        // Then - Strategy PORCENTAJE calculates 10% of 15000 = 1500
        assertEquals(16500L, resultado.getTotalCalculado()); // 15000 + 1500
    }

    // ==================== SNAPSHOT PATTERN TESTS ====================

    @Test
    @DisplayName("Snapshot Pattern: Should freeze prices when confirming")
    void testSnapshotCongelarPrecios() {
        // Given
        DetalleOrden detalle = new DetalleOrden();
        detalle.setMueble(mueble);
        detalle.setCantidad(1);

        DetalleOrdenVariante detalleVariante = new DetalleOrdenVariante();
        detalleVariante.setVariante(varianteFijo);
        detalle.getVariantesAplicadas().add(detalleVariante);

        ordenCotizacion.addDetalle(detalle);

        when(ordenRepository.findById(1)).thenReturn(Optional.of(ordenCotizacion));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrdenDTO resultado = ordenService.confirmarOrden(1);

        // Then - Prices should be frozen in snapshot fields
        assertNotNull(resultado.getDetalles().get(0).getPrecioUnitarioFinal());
        assertEquals(20000, resultado.getDetalles().get(0).getPrecioUnitarioFinal()); // 15000 + 5000
        assertNotNull(resultado.getDetalles().get(0).getVariantes().get(0).getPrecioAplicadoSnapshot());
        assertEquals(5000, resultado.getDetalles().get(0).getVariantes().get(0).getPrecioAplicadoSnapshot());
    }

    @Test
    @DisplayName("Snapshot Pattern: Should preserve total calculado")
    void testSnapshotPreservarTotal() {
        // Given
        DetalleOrden detalle = new DetalleOrden();
        detalle.setMueble(mueble);
        detalle.setCantidad(2);

        ordenCotizacion.addDetalle(detalle);

        when(ordenRepository.findById(1)).thenReturn(Optional.of(ordenCotizacion));
        when(ordenRepository.save(any(Orden.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrdenDTO resultado = ordenService.confirmarOrden(1);

        // Then
        assertNotNull(resultado.getTotalCalculado());
        assertEquals(30000L, resultado.getTotalCalculado()); // 15000 × 2
    }
}
