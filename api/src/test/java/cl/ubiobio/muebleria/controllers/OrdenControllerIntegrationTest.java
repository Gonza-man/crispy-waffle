package cl.ubiobio.muebleria.controllers;

import cl.ubiobio.muebleria.enums.EstadoOrden;
import cl.ubiobio.muebleria.enums.TamanoMueble;
import cl.ubiobio.muebleria.enums.TipoAplicacion;
import cl.ubiobio.muebleria.models.Mueble;
import cl.ubiobio.muebleria.models.VarianteAdicional;
import cl.ubiobio.muebleria.repositories.MuebleRepository;
import cl.ubiobio.muebleria.repositories.OrdenRepository;
import cl.ubiobio.muebleria.repositories.VarianteAdicionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrdenController
 * Tests the full stack including all design patterns
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("OrdenController Integration Tests - All Patterns")
class OrdenControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MuebleRepository muebleRepository;

    @Autowired
    private VarianteAdicionalRepository varianteRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    private Integer muebleId;
    private Integer varianteFijoId;
    private Integer variantePorcentajeId;

    @BeforeEach
    void setUp() {
        // Clean data
        ordenRepository.deleteAll();
        varianteRepository.deleteAll();
        muebleRepository.deleteAll();

        // Create test data
        Mueble mueble = new Mueble();
        mueble.setNombre("Silla Test");
        mueble.setTipo("Silla");
        mueble.setPrecioBase(15000);
        mueble.setStock(100);
        mueble.setTamano(TamanoMueble.MEDIANO);
        mueble.setMaterial("Roble");
        mueble.setEstadoLogico(true);
        mueble = muebleRepository.save(mueble);
        muebleId = mueble.getIdMueble();

        VarianteAdicional varianteFijo = new VarianteAdicional();
        varianteFijo.setNombre("Lacado");
        varianteFijo.setCostoExtra(5000);
        varianteFijo.setTipoAplicacion(TipoAplicacion.FIJO);
        varianteFijo.setActivo(true);
        varianteFijo = varianteRepository.save(varianteFijo);
        varianteFijoId = varianteFijo.getIdVariante();

        VarianteAdicional variantePorcentaje = new VarianteAdicional();
        variantePorcentaje.setNombre("Tapizado");
        variantePorcentaje.setCostoExtra(10);
        variantePorcentaje.setTipoAplicacion(TipoAplicacion.PORCENTAJE);
        variantePorcentaje.setActivo(true);
        variantePorcentaje = varianteRepository.save(variantePorcentaje);
        variantePorcentajeId = variantePorcentaje.getIdVariante();
    }

    @Test
    @DisplayName("Integration: Should create orden in COTIZACION state")
    void testCrearOrdenIntegration() throws Exception {
        String requestBody = String.format("""
            {
                "detalles": [
                    {
                        "idMueble": %d,
                        "cantidad": 1,
                        "idsVariantes": []
                    }
                ]
            }
            """, muebleId);

        mockMvc.perform(post("/api/ordenes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoOrden").value("COTIZACION"))
                .andExpect(jsonPath("$.totalCalculado").value(15000))
                .andExpect(jsonPath("$.fechaConfirmacion").doesNotExist());
    }

    @Test
    @DisplayName("Integration: Should apply FIJO variant (Decorator + Strategy)")
    void testCrearOrdenConVarianteFijo() throws Exception {
        String requestBody = String.format("""
            {
                "detalles": [
                    {
                        "idMueble": %d,
                        "cantidad": 1,
                        "idsVariantes": [%d]
                    }
                ]
            }
            """, muebleId, varianteFijoId);

        mockMvc.perform(post("/api/ordenes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoOrden").value("COTIZACION"))
                .andExpect(jsonPath("$.totalCalculado").value(20000)); // 15000 + 5000
    }

    @Test
    @DisplayName("Integration: Should apply PORCENTAJE variant (Strategy)")
    void testCrearOrdenConVariantePorcentaje() throws Exception {
        String requestBody = String.format("""
            {
                "detalles": [
                    {
                        "idMueble": %d,
                        "cantidad": 1,
                        "idsVariantes": [%d]
                    }
                ]
            }
            """, muebleId, variantePorcentajeId);

        mockMvc.perform(post("/api/ordenes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoOrden").value("COTIZACION"))
                .andExpect(jsonPath("$.totalCalculado").value(16500)); // 15000 + 1500 (10%)
    }

    @Test
    @DisplayName("Integration: Should apply multiple variants (Decorator chain)")
    void testCrearOrdenConMultiplesVariantes() throws Exception {
        String requestBody = String.format("""
            {
                "detalles": [
                    {
                        "idMueble": %d,
                        "cantidad": 2,
                        "idsVariantes": [%d, %d]
                    }
                ]
            }
            """, muebleId, varianteFijoId, variantePorcentajeId);

        mockMvc.perform(post("/api/ordenes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoOrden").value("COTIZACION"))
                .andExpect(jsonPath("$.totalCalculado").value(43000)); // (15000 + 5000 + 1500) × 2
    }

    @Test
    @DisplayName("Integration: Should list orders by state")
    void testListarOrdenesPorEstado() throws Exception {
        // Create an order first
        String requestBody = String.format("""
            {
                "detalles": [
                    {
                        "idMueble": %d,
                        "cantidad": 1,
                        "idsVariantes": []
                    }
                ]
            }
            """, muebleId);

        mockMvc.perform(post("/api/ordenes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated());

        // List by COTIZACION state
        mockMvc.perform(get("/api/ordenes/estado/COTIZACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].estadoOrden").value("COTIZACION"));
    }
}
