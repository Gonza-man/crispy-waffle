package cl.ubiobio.muebleria.controllers;

import cl.ubiobio.muebleria.dto.CrearOrdenRequestDTO;
import cl.ubiobio.muebleria.dto.DetalleRequestDTO;
import cl.ubiobio.muebleria.dto.OrdenDTO;
import cl.ubiobio.muebleria.enums.EstadoOrden;
import cl.ubiobio.muebleria.models.Usuario;
import cl.ubiobio.muebleria.security.CustomUserDetailsService;
import cl.ubiobio.muebleria.services.OrdenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

  @Autowired
  private CustomUserDetailsService userDetailsService;

  public OrdenController(OrdenService ordenService) {
    this.ordenService = ordenService;
  }

  private Usuario getAuthenticatedUser(Authentication authentication) {
    return userDetailsService.getUserByUsername(authentication.getName());
  }

  @GetMapping
  public ResponseEntity<List<OrdenDTO>> listar(Authentication authentication) {
    Usuario usuario = getAuthenticatedUser(authentication);
    List<OrdenDTO> ordenes = ordenService.listarOrdenes(usuario);
    return ResponseEntity.ok(ordenes);
  }

  @GetMapping("/estado/{estado}")
  public ResponseEntity<List<OrdenDTO>> listarPorEstado(@PathVariable EstadoOrden estado, Authentication authentication) {
    Usuario usuario = getAuthenticatedUser(authentication);
    List<OrdenDTO> ordenes = ordenService.listarPorEstado(estado, usuario);
    return ResponseEntity.ok(ordenes);
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrdenDTO> obtenerPorId(@PathVariable Integer id, Authentication authentication) {
    try {
      Usuario usuario = getAuthenticatedUser(authentication);
      OrdenDTO orden = ordenService.obtenerPorId(id, usuario);
      return ResponseEntity.ok(orden);
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
    }
  }

  /**
   * Crea una nueva orden en estado COTIZACION
   */
  @PostMapping
  public ResponseEntity<?> crear(@RequestBody CrearOrdenRequestDTO request, Authentication authentication) {
    try {
      Usuario usuario = getAuthenticatedUser(authentication);
      OrdenDTO creada = ordenService.crearOrden(request, usuario);
      return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    } catch (RuntimeException e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  /**
   * Agrega un detalle a una orden existente
   * STATE PATTERN: Solo permitido en estado COTIZACION
   */
  @PostMapping("/{id}/detalles")
  public ResponseEntity<?> agregarDetalle(@PathVariable Integer id,
                                          @RequestBody DetalleRequestDTO request,
                                          Authentication authentication) {
    try {
      Usuario usuario = getAuthenticatedUser(authentication);
      OrdenDTO actualizada = ordenService.agregarDetalle(id, request, usuario);
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
                                           @PathVariable Integer idDetalle,
                                           Authentication authentication) {
    try {
      Usuario usuario = getAuthenticatedUser(authentication);
      OrdenDTO actualizada = ordenService.eliminarDetalle(idOrden, idDetalle, usuario);
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
  public ResponseEntity<?> confirmar(@PathVariable Integer id, Authentication authentication) {
    try {
      Usuario usuario = getAuthenticatedUser(authentication);
      OrdenDTO confirmada = ordenService.confirmarOrden(id, usuario);
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
  public ResponseEntity<?> cancelar(@PathVariable Integer id, Authentication authentication) {
    try {
      Usuario usuario = getAuthenticatedUser(authentication);
      OrdenDTO cancelada = ordenService.cancelarOrden(id, usuario);
      return ResponseEntity.ok(cancelada);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
