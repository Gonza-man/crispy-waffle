package cl.ubiobio.muebleria.services;

import cl.ubiobio.muebleria.dto.VarianteAdicionalDTO;
import cl.ubiobio.muebleria.dto.VarianteAdicionalRequestDTO;
import cl.ubiobio.muebleria.enums.TipoAplicacion;
import cl.ubiobio.muebleria.models.VarianteAdicional;
import cl.ubiobio.muebleria.repositories.VarianteAdicionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VarianteAdicionalService Tests")
class VarianteAdicionalServiceTest {

    @Mock
    private VarianteAdicionalRepository varianteRepository;

    @InjectMocks
    private VarianteAdicionalService varianteService;

    private VarianteAdicional varianteFijo;
    private VarianteAdicional variantePorcentaje;
    private VarianteAdicionalRequestDTO requestFijo;
    private VarianteAdicionalRequestDTO requestPorcentaje;

    @BeforeEach
    void setUp() {
        // Variante FIJO
        varianteFijo = new VarianteAdicional();
        varianteFijo.setIdVariante(1);
        varianteFijo.setNombre("Lacado Premium");
        varianteFijo.setCostoExtra(5000);
        varianteFijo.setTipoAplicacion(TipoAplicacion.FIJO);
        varianteFijo.setActivo(true);

        // Variante PORCENTAJE
        variantePorcentaje = new VarianteAdicional();
        variantePorcentaje.setIdVariante(2);
        variantePorcentaje.setNombre("Tapizado Especial");
        variantePorcentaje.setCostoExtra(10);
        variantePorcentaje.setTipoAplicacion(TipoAplicacion.PORCENTAJE);
        variantePorcentaje.setActivo(true);

        // Request FIJO
        requestFijo = new VarianteAdicionalRequestDTO();
        requestFijo.setNombre("Lacado Premium");
        requestFijo.setCostoExtra(5000);
        requestFijo.setTipoAplicacion(TipoAplicacion.FIJO);

        // Request PORCENTAJE
        requestPorcentaje = new VarianteAdicionalRequestDTO();
        requestPorcentaje.setNombre("Tapizado Especial");
        requestPorcentaje.setCostoExtra(10);
        requestPorcentaje.setTipoAplicacion(TipoAplicacion.PORCENTAJE);
    }

    @Test
    @DisplayName("Should list all active variantes")
    void testListarVariantesActivas() {
        // Given
        when(varianteRepository.findByActivoTrue())
            .thenReturn(Arrays.asList(varianteFijo, variantePorcentaje));

        // When
        List<VarianteAdicionalDTO> resultado = varianteService.listarVariantesActivas();

        // Then
        assertEquals(2, resultado.size());
        verify(varianteRepository, times(1)).findByActivoTrue();
    }

    @Test
    @DisplayName("Should get variante by ID")
    void testObtenerPorId() {
        // Given
        when(varianteRepository.findById(1)).thenReturn(Optional.of(varianteFijo));

        // When
        VarianteAdicionalDTO resultado = varianteService.obtenerPorId(1);

        // Then
        assertNotNull(resultado);
        assertEquals("Lacado Premium", resultado.getNombre());
        assertEquals(5000, resultado.getCostoExtra());
        assertEquals(TipoAplicacion.FIJO, resultado.getTipoAplicacion());
        verify(varianteRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should create FIJO variante")
    void testCrearVarianteFijo() {
        // Given
        when(varianteRepository.save(any(VarianteAdicional.class))).thenReturn(varianteFijo);

        // When
        VarianteAdicionalDTO resultado = varianteService.crear(requestFijo);

        // Then
        assertNotNull(resultado);
        assertEquals("Lacado Premium", resultado.getNombre());
        assertEquals(TipoAplicacion.FIJO, resultado.getTipoAplicacion());
        assertTrue(resultado.getActivo());
        verify(varianteRepository, times(1)).save(any(VarianteAdicional.class));
    }

    @Test
    @DisplayName("Should create PORCENTAJE variante")
    void testCrearVariantePorcentaje() {
        // Given
        when(varianteRepository.save(any(VarianteAdicional.class))).thenReturn(variantePorcentaje);

        // When
        VarianteAdicionalDTO resultado = varianteService.crear(requestPorcentaje);

        // Then
        assertNotNull(resultado);
        assertEquals("Tapizado Especial", resultado.getNombre());
        assertEquals(10, resultado.getCostoExtra());
        assertEquals(TipoAplicacion.PORCENTAJE, resultado.getTipoAplicacion());
        verify(varianteRepository, times(1)).save(any(VarianteAdicional.class));
    }

    @Test
    @DisplayName("Should update variante")
    void testActualizar() {
        // Given
        when(varianteRepository.findById(1)).thenReturn(Optional.of(varianteFijo));
        when(varianteRepository.save(any(VarianteAdicional.class))).thenReturn(varianteFijo);

        requestFijo.setNombre("Lacado Premium Plus");
        requestFijo.setCostoExtra(7000);

        // When
        VarianteAdicionalDTO resultado = varianteService.actualizar(1, requestFijo);

        // Then
        assertNotNull(resultado);
        verify(varianteRepository, times(1)).findById(1);
        verify(varianteRepository, times(1)).save(any(VarianteAdicional.class));
    }

    @Test
    @DisplayName("Should soft delete variante (set activo to false)")
    void testEliminar() {
        // Given
        when(varianteRepository.findById(1)).thenReturn(Optional.of(varianteFijo));
        when(varianteRepository.save(any(VarianteAdicional.class))).thenAnswer(invocation -> {
            VarianteAdicional v = invocation.getArgument(0);
            assertFalse(v.getActivo(), "activo should be false after delete");
            return v;
        });

        // When
        varianteService.eliminar(1);

        // Then
        verify(varianteRepository, times(1)).findById(1);
        verify(varianteRepository, times(1)).save(any(VarianteAdicional.class));
    }

    @Test
    @DisplayName("Should throw exception when variante not found")
    void testObtenerPorIdNoEncontrado() {
        // Given
        when(varianteRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            varianteService.obtenerPorId(999);
        });
    }
}
