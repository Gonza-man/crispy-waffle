package cl.ubiobio.muebleria.services;

import cl.ubiobio.muebleria.dto.MuebleDTO;
import cl.ubiobio.muebleria.dto.MuebleRequestDTO;
import cl.ubiobio.muebleria.enums.TamanoMueble;
import cl.ubiobio.muebleria.models.Mueble;
import cl.ubiobio.muebleria.repositories.MuebleRepository;
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
@DisplayName("MuebleService Tests")
class MuebleServiceTest {

    @Mock
    private MuebleRepository muebleRepository;

    @InjectMocks
    private MuebleService muebleService;

    private Mueble mueble;
    private MuebleRequestDTO muebleRequest;

    @BeforeEach
    void setUp() {
        mueble = new Mueble();
        mueble.setIdMueble(1);
        mueble.setNombre("Silla de Roble");
        mueble.setTipo("Silla");
        mueble.setPrecioBase(15000);
        mueble.setStock(50);
        mueble.setTamano(TamanoMueble.MEDIANO);
        mueble.setMaterial("Roble");
        mueble.setEstadoLogico(true);

        muebleRequest = new MuebleRequestDTO();
        muebleRequest.setNombre("Silla de Roble");
        muebleRequest.setTipo("Silla");
        muebleRequest.setPrecioBase(15000);
        muebleRequest.setStock(50);
        muebleRequest.setTamano(TamanoMueble.MEDIANO);
        muebleRequest.setMaterial("Roble");
    }

    @Test
    @DisplayName("Should list all active muebles")
    void testListarMueblesActivos() {
        // Given
        Mueble mueble2 = new Mueble();
        mueble2.setIdMueble(2);
        mueble2.setNombre("Mesa");
        mueble2.setEstadoLogico(true);

        when(muebleRepository.findByEstadoLogicoTrue())
            .thenReturn(Arrays.asList(mueble, mueble2));

        // When
        List<MuebleDTO> resultado = muebleService.listarMueblesActivos();

        // Then
        assertEquals(2, resultado.size());
        verify(muebleRepository, times(1)).findByEstadoLogicoTrue();
    }

    @Test
    @DisplayName("Should get mueble by ID")
    void testObtenerPorId() {
        // Given
        when(muebleRepository.findById(1)).thenReturn(Optional.of(mueble));

        // When
        MuebleDTO resultado = muebleService.obtenerPorId(1);

        // Then
        assertNotNull(resultado);
        assertEquals("Silla de Roble", resultado.getNombre());
        assertEquals(15000, resultado.getPrecioBase());
        verify(muebleRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Should throw exception when mueble not found")
    void testObtenerPorIdNoEncontrado() {
        // Given
        when(muebleRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            muebleService.obtenerPorId(999);
        });
    }

    @Test
    @DisplayName("Should create new mueble")
    void testCrear() {
        // Given
        when(muebleRepository.save(any(Mueble.class))).thenReturn(mueble);

        // When
        MuebleDTO resultado = muebleService.crear(muebleRequest);

        // Then
        assertNotNull(resultado);
        assertEquals("Silla de Roble", resultado.getNombre());
        assertTrue(resultado.getEstadoLogico());
        verify(muebleRepository, times(1)).save(any(Mueble.class));
    }

    @Test
    @DisplayName("Should update existing mueble")
    void testActualizar() {
        // Given
        when(muebleRepository.findById(1)).thenReturn(Optional.of(mueble));
        when(muebleRepository.save(any(Mueble.class))).thenReturn(mueble);

        muebleRequest.setNombre("Silla Actualizada");
        muebleRequest.setPrecioBase(20000);

        // When
        MuebleDTO resultado = muebleService.actualizar(1, muebleRequest);

        // Then
        assertNotNull(resultado);
        verify(muebleRepository, times(1)).findById(1);
        verify(muebleRepository, times(1)).save(any(Mueble.class));
    }

    @Test
    @DisplayName("Should soft delete mueble (set estadoLogico to false)")
    void testEliminar() {
        // Given
        when(muebleRepository.findById(1)).thenReturn(Optional.of(mueble));
        when(muebleRepository.save(any(Mueble.class))).thenAnswer(invocation -> {
            Mueble m = invocation.getArgument(0);
            assertFalse(m.getEstadoLogico(), "estadoLogico should be false after delete");
            return m;
        });

        // When
        muebleService.eliminar(1);

        // Then
        verify(muebleRepository, times(1)).findById(1);
        verify(muebleRepository, times(1)).save(any(Mueble.class));
    }

    @Test
    @DisplayName("Should search muebles by name")
    void testBuscarPorNombre() {
        // Given
        when(muebleRepository.findByNombreContainingIgnoreCaseAndEstadoLogicoTrue("silla"))
            .thenReturn(Arrays.asList(mueble));

        // When
        List<MuebleDTO> resultado = muebleService.buscarPorNombre("silla");

        // Then
        assertEquals(1, resultado.size());
        assertEquals("Silla de Roble", resultado.get(0).getNombre());
        verify(muebleRepository, times(1))
            .findByNombreContainingIgnoreCaseAndEstadoLogicoTrue("silla");
    }
}
