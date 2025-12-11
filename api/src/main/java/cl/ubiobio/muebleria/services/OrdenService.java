package cl.ubiobio.muebleria.services;

import cl.ubiobio.muebleria.dto.*;
import cl.ubiobio.muebleria.enums.EstadoOrden;
import cl.ubiobio.muebleria.enums.Rol;
import cl.ubiobio.muebleria.models.*;
import cl.ubiobio.muebleria.repositories.MuebleRepository;
import cl.ubiobio.muebleria.repositories.OrdenRepository;
import cl.ubiobio.muebleria.repositories.VarianteAdicionalRepository;
import cl.ubiobio.muebleria.strategy.PrecioStrategy;
import cl.ubiobio.muebleria.strategy.PrecioStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
  public List<OrdenDTO> listarOrdenes(Usuario usuario) {
    List<Orden> ordenes;
    if (usuario.getRol() == Rol.ADMIN) {
      ordenes = ordenRepository.findAll();
    } else {
      ordenes = ordenRepository.findByUsuarioOrderByFechaCreacionDesc(usuario);
    }
    return ordenes.stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<OrdenDTO> listarPorEstado(EstadoOrden estado, Usuario usuario) {
    List<Orden> ordenes;
    if (usuario.getRol() == Rol.ADMIN) {
      ordenes = ordenRepository.findByEstadoOrdenOrderByFechaCreacionDesc(estado);
    } else {
      ordenes = ordenRepository.findByUsuarioAndEstadoOrdenOrderByFechaCreacionDesc(usuario, estado);
    }
    return ordenes.stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public OrdenDTO obtenerPorId(Integer id, Usuario usuario) {
    Orden orden = ordenRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + id));

    // Validar que el usuario tenga acceso a esta orden
    if (usuario.getRol() != Rol.ADMIN && !orden.getUsuario().getId().equals(usuario.getId())) {
      throw new RuntimeException("No tienes permiso para ver esta orden");
    }

    return toDTO(orden);
  }

  /**
   * Crea una nueva orden en estado COTIZACION
   * Calcula precios en tiempo real pero NO los congela (aún son editables)
   */
  @Transactional
  public OrdenDTO crearOrden(CrearOrdenRequestDTO request, Usuario usuario) {
    Orden orden = new Orden();
    orden.setEstadoOrden(EstadoOrden.COTIZACION);
    orden.setFechaCreacion(LocalDateTime.now());
    orden.setUsuario(usuario);

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
  public OrdenDTO agregarDetalle(Integer idOrden, DetalleRequestDTO detalleRequest, Usuario usuario) {
    Orden orden = ordenRepository.findById(idOrden)
        .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + idOrden));

    // Validar que el usuario tenga acceso a esta orden
    if (usuario.getRol() != Rol.ADMIN && !orden.getUsuario().getId().equals(usuario.getId())) {
      throw new RuntimeException("No tienes permiso para modificar esta orden");
    }

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
   * Descuenta el stock de los muebles
   */
  @Transactional
  public OrdenDTO confirmarOrden(Integer idOrden, Usuario usuario) {
    Orden orden = ordenRepository.findById(idOrden)
        .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + idOrden));

    // Validar que el usuario tenga acceso a esta orden
    if (usuario.getRol() != Rol.ADMIN && !orden.getUsuario().getId().equals(usuario.getId())) {
      throw new RuntimeException("No tienes permiso para confirmar esta orden");
    }

    // STATE PATTERN: Validar transición válida
    if (orden.getEstadoOrden() != EstadoOrden.COTIZACION) {
      throw new RuntimeException("Solo se puede confirmar una orden en estado COTIZACION");
    }

    // Validar stock disponible antes de confirmar
    validarStockDisponible(orden);

    // Descontar stock de cada mueble
    descontarStock(orden);

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
   * Si la orden estaba en VENTA, devuelve el stock
   */
  @Transactional
  public OrdenDTO cancelarOrden(Integer idOrden, Usuario usuario) {
    Orden orden = ordenRepository.findById(idOrden)
        .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + idOrden));

    // Validar que el usuario tenga acceso a esta orden
    if (usuario.getRol() != Rol.ADMIN && !orden.getUsuario().getId().equals(usuario.getId())) {
      throw new RuntimeException("No tienes permiso para cancelar esta orden");
    }

    // Si la orden estaba confirmada (VENTA), devolver stock
    if (orden.getEstadoOrden() == EstadoOrden.VENTA) {
      devolverStock(orden);
    }

    orden.setEstadoOrden(EstadoOrden.CANCELADA);

    Orden cancelada = ordenRepository.save(orden);
    return toDTO(cancelada);
  }

  /**
   * Elimina un detalle de una orden
   * STATE PATTERN: Solo permitido en COTIZACION
   */
  @Transactional
  public OrdenDTO eliminarDetalle(Integer idOrden, Integer idDetalle, Usuario usuario) {
    Orden orden = ordenRepository.findById(idOrden)
        .orElseThrow(() -> new RuntimeException("Orden no encontrada con ID: " + idOrden));

    // Validar que el usuario tenga acceso a esta orden
    if (usuario.getRol() != Rol.ADMIN && !orden.getUsuario().getId().equals(usuario.getId())) {
      throw new RuntimeException("No tienes permiso para modificar esta orden");
    }

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
   * Valida stock disponible considerando cotizaciones pendientes
   */
  private DetalleOrden crearDetalle(Orden orden, DetalleRequestDTO request) {
    Mueble mueble = muebleRepository.findById(request.getIdMueble())
        .orElseThrow(() -> new RuntimeException("Mueble no encontrado con ID: " + request.getIdMueble()));

    // Calcular stock disponible (físico - reservado en cotizaciones activas)
    Integer stockDisponible = calcularStockDisponible(mueble);

    if (stockDisponible < request.getCantidad()) {
      throw new RuntimeException(
          String.format("Stock insuficiente para mueble '%s'. Disponible: %d, Solicitado: %d (Stock físico: %d, Reservado en cotizaciones: %d)",
              mueble.getNombre(), stockDisponible, request.getCantidad(),
              mueble.getStock(), mueble.getStock() - stockDisponible));
    }

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

  /**
   * Valida que haya stock físico suficiente para todos los items de la orden
   * Solo valida stock físico real, no considera otras cotizaciones
   * (al confirmar, la cotización actual libera su reserva y consume stock físico)
   */
  private void validarStockDisponible(Orden orden) {
    for (DetalleOrden detalle : orden.getDetalles()) {
      Mueble mueble = detalle.getMueble();
      if (mueble.getStock() < detalle.getCantidad()) {
        throw new RuntimeException(
            String.format("Stock insuficiente para confirmar orden. Mueble '%s': Stock físico=%d, Requerido=%d",
                mueble.getNombre(), mueble.getStock(), detalle.getCantidad()));
      }
    }
  }

  /**
   * Descuenta el stock de los muebles al confirmar una orden
   */
  private void descontarStock(Orden orden) {
    for (DetalleOrden detalle : orden.getDetalles()) {
      Mueble mueble = detalle.getMueble();
      Integer nuevoStock = mueble.getStock() - detalle.getCantidad();
      mueble.setStock(nuevoStock);
      muebleRepository.save(mueble);
    }
  }

  /**
   * Devuelve el stock de los muebles al cancelar una orden confirmada
   */
  private void devolverStock(Orden orden) {
    for (DetalleOrden detalle : orden.getDetalles()) {
      Mueble mueble = detalle.getMueble();
      Integer nuevoStock = mueble.getStock() + detalle.getCantidad();
      mueble.setStock(nuevoStock);
      muebleRepository.save(mueble);
    }
  }

  /**
   * Calcula el stock realmente disponible considerando reservas en cotizaciones
   * Stock Disponible = Stock Físico - Stock Reservado en COTIZACIONES activas
   */
  private Integer calcularStockDisponible(Mueble mueble) {
    // Obtener todas las órdenes en estado COTIZACION
    List<Orden> cotizacionesActivas = ordenRepository.findByEstadoOrdenOrderByFechaCreacionDesc(EstadoOrden.COTIZACION);

    // Calcular cuánto stock está reservado en cotizaciones
    int stockReservado = 0;
    for (Orden cotizacion : cotizacionesActivas) {
      for (DetalleOrden detalle : cotizacion.getDetalles()) {
        if (detalle.getMueble().getIdMueble().equals(mueble.getIdMueble())) {
          stockReservado += detalle.getCantidad();
        }
      }
    }

    return mueble.getStock() - stockReservado;
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
