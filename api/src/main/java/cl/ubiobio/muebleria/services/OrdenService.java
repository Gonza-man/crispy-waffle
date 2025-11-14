package cl.ubiobio.muebleria.services;

import cl.ubiobio.muebleria.dto.*;
import cl.ubiobio.muebleria.enums.EstadoOrden;
import cl.ubiobio.muebleria.models.*;
import cl.ubiobio.muebleria.repositories.MuebleRepository;
import cl.ubiobio.muebleria.repositories.OrdenRepository;
import cl.ubiobio.muebleria.repositories.VarianteAdicionalRepository;
import cl.ubiobio.muebleria.strategy.PrecioStrategy;
import cl.ubiobio.muebleria.strategy.PrecioStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio que implementa los patrones de diseño:
 * - State Pattern: Gestión de estados de orden (COTIZACION -> VENTA)
 * - Decorator Pattern: Aplicación de variantes a muebles
 * - Strategy Pattern: Cálculo de precios según tipo de aplicación
 * - Snapshot Pattern: Congelación de precios al confirmar venta
 */
@Service
public class OrdenService {

  private final OrdenRepository ordenRepository;
  private final MuebleRepository muebleRepository;
  private final VarianteAdicionalRepository varianteRepository;
  private final PrecioStrategyFactory precioStrategyFactory;

  public OrdenService(OrdenRepository ordenRepository,
                      MuebleRepository muebleRepository,
                      VarianteAdicionalRepository varianteRepository,
                      PrecioStrategyFactory precioStrategyFactory) {
    this.ordenRepository = ordenRepository;
    this.muebleRepository = muebleRepository;
    this.varianteRepository = varianteRepository;
    this.precioStrategyFactory = precioStrategyFactory;
  }

  @Transactional(readOnly = true)
  public List<OrdenDTO> listarOrdenes() {
    return ordenRepository.findAll().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<OrdenDTO> listarPorEstado(EstadoOrden estado) {
    return ordenRepository.findByEstadoOrdenOrderByFechaCreacionDesc(estado).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public OrdenDTO obtenerPorId(Integer id) {
    Orden orden = ordenRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + id));
    return toDTO(orden);
  }

  /**
   * Crea una nueva orden en estado COTIZACION
   * Calcula precios en tiempo real pero NO los congela (aún son editables)
   */
  @Transactional
  public OrdenDTO crearOrden(CrearOrdenRequestDTO request) {
    Orden orden = new Orden();
    orden.setEstadoOrden(EstadoOrden.COTIZACION);
    orden.setFechaCreacion(LocalDateTime.now());

    // Procesar detalles
    for (DetalleRequestDTO detalleReq : request.getDetalles()) {
      DetalleOrden detalle = crearDetalle(orden, detalleReq);
      orden.addDetalle(detalle);
    }

    // Calcular total (sin congelar precios aún)
    Long total = calcularTotalOrden(orden);
    orden.setTotalCalculado(total);

    Orden guardada = ordenRepository.save(orden);
    return toDTO(guardada);
  }

  /**
   * Agrega un detalle a una orden existente
   * STATE PATTERN: Solo permitido si la orden está en COTIZACION
   */
  @Transactional
  public OrdenDTO agregarDetalle(Integer idOrden, DetalleRequestDTO detalleRequest) {
    Orden orden = ordenRepository.findById(idOrden)
        .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + idOrden));

    // STATE PATTERN: Validar que la orden sea editable
    if (orden.getEstadoOrden() != EstadoOrden.COTIZACION) {
      throw new RuntimeException("No se puede modificar una orden en estado: " + orden.getEstadoOrden());
    }

    DetalleOrden detalle = crearDetalle(orden, detalleRequest);
    orden.addDetalle(detalle);

    // Recalcular total
    Long total = calcularTotalOrden(orden);
    orden.setTotalCalculado(total);

    Orden actualizada = ordenRepository.save(orden);
    return toDTO(actualizada);
  }

  /**
   * Confirma una orden (transición COTIZACION -> VENTA)
   * STATE PATTERN: Transición de estado
   * SNAPSHOT PATTERN: Congela precios al momento de la confirmación
   */
  @Transactional
  public OrdenDTO confirmarOrden(Integer idOrden) {
    Orden orden = ordenRepository.findById(idOrden)
        .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + idOrden));

    // STATE PATTERN: Validar transición válida
    if (orden.getEstadoOrden() != EstadoOrden.COTIZACION) {
      throw new RuntimeException("Solo se puede confirmar una orden en estado COTIZACION");
    }

    // SNAPSHOT PATTERN: Congelar precios
    congelarPrecios(orden);

    // STATE PATTERN: Cambiar estado
    orden.setEstadoOrden(EstadoOrden.VENTA);
    orden.setFechaConfirmacion(LocalDateTime.now());

    // Recalcular total final
    Long totalFinal = calcularTotalOrdenSnapshot(orden);
    orden.setTotalCalculado(totalFinal);

    Orden confirmada = ordenRepository.save(orden);
    return toDTO(confirmada);
  }

  /**
   * Cancela una orden
   * STATE PATTERN: Permite cancelar desde cualquier estado
   */
  @Transactional
  public OrdenDTO cancelarOrden(Integer idOrden) {
    Orden orden = ordenRepository.findById(idOrden)
        .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + idOrden));

    orden.setEstadoOrden(EstadoOrden.CANCELADA);

    Orden cancelada = ordenRepository.save(orden);
    return toDTO(cancelada);
  }

  /**
   * Elimina un detalle de una orden
   * STATE PATTERN: Solo permitido en COTIZACION
   */
  @Transactional
  public OrdenDTO eliminarDetalle(Integer idOrden, Integer idDetalle) {
    Orden orden = ordenRepository.findById(idOrden)
        .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + idOrden));

    // STATE PATTERN: Validar que la orden sea editable
    if (orden.getEstadoOrden() != EstadoOrden.COTIZACION) {
      throw new RuntimeException("No se puede modificar una orden en estado: " + orden.getEstadoOrden());
    }

    orden.getDetalles().removeIf(d -> d.getIdDetalle().equals(idDetalle));

    // Recalcular total
    Long total = calcularTotalOrden(orden);
    orden.setTotalCalculado(total);

    Orden actualizada = ordenRepository.save(orden);
    return toDTO(actualizada);
  }

  // ==================== MÉTODOS PRIVADOS ====================

  /**
   * Crea un detalle de orden con sus variantes
   * DECORATOR PATTERN: Las variantes "decoran" el mueble base
   */
  private DetalleOrden crearDetalle(Orden orden, DetalleRequestDTO request) {
    Mueble mueble = muebleRepository.findById(request.getIdMueble())
        .orElseThrow(() -> new RuntimeException("Mueble no encontrado con ID: " + request.getIdMueble()));

    DetalleOrden detalle = new DetalleOrden();
    detalle.setOrden(orden);
    detalle.setMueble(mueble);
    detalle.setCantidad(request.getCantidad());

    // DECORATOR PATTERN: Aplicar variantes si existen
    if (request.getIdsVariantes() != null && !request.getIdsVariantes().isEmpty()) {
      for (Integer idVariante : request.getIdsVariantes()) {
        VarianteAdicional variante = varianteRepository.findById(idVariante)
            .orElseThrow(() -> new RuntimeException("Variante no encontrada con ID: " + idVariante));

        DetalleOrdenVariante detalleVariante = new DetalleOrdenVariante();
        detalleVariante.setDetalle(detalle);
        detalleVariante.setVariante(variante);

        detalle.getVariantesAplicadas().add(detalleVariante);
      }
    }

    return detalle;
  }

  /**
   * SNAPSHOT PATTERN: Congela los precios de todos los detalles
   * Calcula y persiste el precio final al momento de la confirmación
   */
  private void congelarPrecios(Orden orden) {
    for (DetalleOrden detalle : orden.getDetalles()) {
      Integer precioBase = detalle.getMueble().getPrecioBase();
      Integer precioConVariantes = precioBase;

      // DECORATOR + STRATEGY PATTERN: Calcular costo de cada variante
      for (DetalleOrdenVariante detalleVariante : detalle.getVariantesAplicadas()) {
        VarianteAdicional variante = detalleVariante.getVariante();

        // STRATEGY PATTERN: Usar estrategia según tipo de aplicación
        PrecioStrategy strategy = precioStrategyFactory.getStrategy(variante.getTipoAplicacion());
        Integer costoVariante = strategy.calcularCosto(variante.getCostoExtra(), precioBase);

        // SNAPSHOT: Guardar el costo calculado en este momento
        detalleVariante.setPrecioAplicadoSnapshot(costoVariante);

        // DECORATOR: Agregar el costo al precio total
        precioConVariantes += costoVariante;
      }

      // SNAPSHOT: Congelar el precio unitario final
      detalle.setPrecioUnitarioFinal(precioConVariantes);
    }
  }

  /**
   * Calcula el total de una orden en estado COTIZACION (sin snapshots)
   * Usa precios en tiempo real del catálogo
   */
  private Long calcularTotalOrden(Orden orden) {
    long total = 0L;

    for (DetalleOrden detalle : orden.getDetalles()) {
      Integer precioBase = detalle.getMueble().getPrecioBase();
      Integer precioConVariantes = precioBase;

      // DECORATOR + STRATEGY PATTERN: Calcular variantes
      for (DetalleOrdenVariante detalleVariante : detalle.getVariantesAplicadas()) {
        VarianteAdicional variante = detalleVariante.getVariante();
        PrecioStrategy strategy = precioStrategyFactory.getStrategy(variante.getTipoAplicacion());
        Integer costoVariante = strategy.calcularCosto(variante.getCostoExtra(), precioBase);
        precioConVariantes += costoVariante;
      }

      total += (long) precioConVariantes * detalle.getCantidad();
    }

    return total;
  }

  /**
   * Calcula el total de una orden confirmada usando snapshots
   * Usa precios congelados
   */
  private Long calcularTotalOrdenSnapshot(Orden orden) {
    long total = 0L;

    for (DetalleOrden detalle : orden.getDetalles()) {
      total += (long) detalle.getPrecioUnitarioFinal() * detalle.getCantidad();
    }

    return total;
  }

  // ==================== MAPPERS ====================

  private OrdenDTO toDTO(Orden orden) {
    OrdenDTO dto = new OrdenDTO();
    dto.setIdOrden(orden.getIdOrden());
    dto.setFechaCreacion(orden.getFechaCreacion());
    dto.setFechaConfirmacion(orden.getFechaConfirmacion());
    dto.setEstadoOrden(orden.getEstadoOrden());
    dto.setTotalCalculado(orden.getTotalCalculado());

    List<DetalleOrdenDTO> detallesDTO = orden.getDetalles().stream()
        .map(this::detalleToDTO)
        .collect(Collectors.toList());
    dto.setDetalles(detallesDTO);

    return dto;
  }

  private DetalleOrdenDTO detalleToDTO(DetalleOrden detalle) {
    DetalleOrdenDTO dto = new DetalleOrdenDTO();
    dto.setIdDetalle(detalle.getIdDetalle());
    dto.setIdMueble(detalle.getMueble().getIdMueble());
    dto.setNombreMueble(detalle.getMueble().getNombre());
    dto.setCantidad(detalle.getCantidad());
    dto.setPrecioUnitarioFinal(detalle.getPrecioUnitarioFinal());

    List<VarianteAplicadaDTO> variantesDTO = detalle.getVariantesAplicadas().stream()
        .map(this::varianteAplicadaToDTO)
        .collect(Collectors.toList());
    dto.setVariantes(variantesDTO);

    return dto;
  }

  private VarianteAplicadaDTO varianteAplicadaToDTO(DetalleOrdenVariante detalleVariante) {
    VarianteAplicadaDTO dto = new VarianteAplicadaDTO();
    dto.setIdVariante(detalleVariante.getVariante().getIdVariante());
    dto.setNombre(detalleVariante.getVariante().getNombre());
    dto.setPrecioAplicadoSnapshot(detalleVariante.getPrecioAplicadoSnapshot());
    return dto;
  }
}
