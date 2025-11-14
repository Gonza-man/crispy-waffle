package cl.ubiobio.muebleria.controllers;

import cl.ubiobio.muebleria.dto.CrearOrdenRequestDTO;
import cl.ubiobio.muebleria.dto.DetalleRequestDTO;
import cl.ubiobio.muebleria.dto.OrdenDTO;
import cl.ubiobio.muebleria.enums.EstadoOrden;
import cl.ubiobio.muebleria.services.OrdenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para órdenes
 * Expone endpoints que implementan los patrones de diseño:
 * - State Pattern (transiciones de estado)
 * - Decorator Pattern (aplicación de variantes)
 * - Strategy Pattern (cálculo de precios)
 * - Snapshot Pattern (congelación de precios en confirmación)
 */
@RestController
@RequestMapping("/api/ordenes")
@CrossOrigin(origins = "*")
public class OrdenController {

  private final OrdenService ordenService;

  public OrdenController(OrdenService ordenService) {
    this.ordenService = ordenService;
  }

  @GetMapping
  public ResponseEntity<List<OrdenDTO>> listar() {
    List<OrdenDTO> ordenes = ordenService.listarOrdenes();
    return ResponseEntity.ok(ordenes);
  }

  @GetMapping("/estado/{estado}")
  public ResponseEntity<List<OrdenDTO>> listarPorEstado(@PathVariable EstadoOrden estado) {
    List<OrdenDTO> ordenes = ordenService.listarPorEstado(estado);
    return ResponseEntity.ok(ordenes);
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrdenDTO> obtenerPorId(@PathVariable Integer id) {
    try {
      OrdenDTO orden = ordenService.obtenerPorId(id);
      return ResponseEntity.ok(orden);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Crea una nueva orden en estado COTIZACION
   */
  @PostMapping
  public ResponseEntity<OrdenDTO> crear(@RequestBody CrearOrdenRequestDTO request) {
    try {
      OrdenDTO creada = ordenService.crearOrden(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(null);
    }
  }

  /**
   * Agrega un detalle a una orden existente
   * STATE PATTERN: Solo permitido en estado COTIZACION
   */
  @PostMapping("/{id}/detalles")
  public ResponseEntity<?> agregarDetalle(@PathVariable Integer id,
                                          @RequestBody DetalleRequestDTO request) {
    try {
      OrdenDTO actualizada = ordenService.agregarDetalle(id, request);
      return ResponseEntity.ok(actualizada);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  /**
   * Elimina un detalle de una orden
   * STATE PATTERN: Solo permitido en estado COTIZACION
   */
  @DeleteMapping("/{idOrden}/detalles/{idDetalle}")
  public ResponseEntity<?> eliminarDetalle(@PathVariable Integer idOrden,
                                           @PathVariable Integer idDetalle) {
    try {
      OrdenDTO actualizada = ordenService.eliminarDetalle(idOrden, idDetalle);
      return ResponseEntity.ok(actualizada);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  /**
   * Confirma una orden (COTIZACION -> VENTA)
   * STATE PATTERN: Transición de estado
   * SNAPSHOT PATTERN: Congela precios
   */
  @PostMapping("/{id}/confirmar")
  public ResponseEntity<?> confirmar(@PathVariable Integer id) {
    try {
      OrdenDTO confirmada = ordenService.confirmarOrden(id);
      return ResponseEntity.ok(confirmada);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  /**
   * Cancela una orden
   * STATE PATTERN: Transición a estado CANCELADA
   */
  @PostMapping("/{id}/cancelar")
  public ResponseEntity<?> cancelar(@PathVariable Integer id) {
    try {
      OrdenDTO cancelada = ordenService.cancelarOrden(id);
      return ResponseEntity.ok(cancelada);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
