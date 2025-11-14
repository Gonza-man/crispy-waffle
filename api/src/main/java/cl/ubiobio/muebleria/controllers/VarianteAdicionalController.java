package cl.ubiobio.muebleria.controllers;

import cl.ubiobio.muebleria.dto.VarianteAdicionalDTO;
import cl.ubiobio.muebleria.dto.VarianteAdicionalRequestDTO;
import cl.ubiobio.muebleria.services.VarianteAdicionalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/variantes")
@CrossOrigin(origins = "*")
public class VarianteAdicionalController {

  private final VarianteAdicionalService varianteService;

  public VarianteAdicionalController(VarianteAdicionalService varianteService) {
    this.varianteService = varianteService;
  }

  @GetMapping
  public ResponseEntity<List<VarianteAdicionalDTO>> listar() {
    List<VarianteAdicionalDTO> variantes = varianteService.listarVariantesActivas();
    return ResponseEntity.ok(variantes);
  }

  @GetMapping("/{id}")
  public ResponseEntity<VarianteAdicionalDTO> obtenerPorId(@PathVariable Integer id) {
    try {
      VarianteAdicionalDTO variante = varianteService.obtenerPorId(id);
      return ResponseEntity.ok(variante);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<VarianteAdicionalDTO> crear(@RequestBody VarianteAdicionalRequestDTO request) {
    VarianteAdicionalDTO creada = varianteService.crear(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(creada);
  }

  @PutMapping("/{id}")
  public ResponseEntity<VarianteAdicionalDTO> actualizar(@PathVariable Integer id,
                                                          @RequestBody VarianteAdicionalRequestDTO request) {
    try {
      VarianteAdicionalDTO actualizada = varianteService.actualizar(id, request);
      return ResponseEntity.ok(actualizada);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
    try {
      varianteService.eliminar(id);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
