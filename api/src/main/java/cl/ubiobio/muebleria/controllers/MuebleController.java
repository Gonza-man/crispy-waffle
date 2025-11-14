package cl.ubiobio.muebleria.controllers;

import cl.ubiobio.muebleria.dto.MuebleDTO;
import cl.ubiobio.muebleria.dto.MuebleRequestDTO;
import cl.ubiobio.muebleria.services.MuebleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/muebles")
@CrossOrigin(origins = "*")
public class MuebleController {

  private final MuebleService muebleService;

  public MuebleController(MuebleService muebleService) {
    this.muebleService = muebleService;
  }

  @GetMapping
  public ResponseEntity<List<MuebleDTO>> listar() {
    List<MuebleDTO> muebles = muebleService.listarMueblesActivos();
    return ResponseEntity.ok(muebles);
  }

  @GetMapping("/{id}")
  public ResponseEntity<MuebleDTO> obtenerPorId(@PathVariable Integer id) {
    try {
      MuebleDTO mueble = muebleService.obtenerPorId(id);
      return ResponseEntity.ok(mueble);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/buscar")
  public ResponseEntity<List<MuebleDTO>> buscarPorNombre(@RequestParam String nombre) {
    List<MuebleDTO> muebles = muebleService.buscarPorNombre(nombre);
    return ResponseEntity.ok(muebles);
  }

  @PostMapping
  public ResponseEntity<MuebleDTO> crear(@RequestBody MuebleRequestDTO request) {
    MuebleDTO creado = muebleService.crear(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(creado);
  }

  @PutMapping("/{id}")
  public ResponseEntity<MuebleDTO> actualizar(@PathVariable Integer id,
                                               @RequestBody MuebleRequestDTO request) {
    try {
      MuebleDTO actualizado = muebleService.actualizar(id, request);
      return ResponseEntity.ok(actualizado);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
    try {
      muebleService.eliminar(id);
      return ResponseEntity.noContent().build();
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
