package cl.ubiobio.muebleria.repositories;

import cl.ubiobio.muebleria.enums.EstadoOrden;
import cl.ubiobio.muebleria.models.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Integer> {

  // Buscar por estado
  List<Orden> findByEstadoOrden(EstadoOrden estado);

  // Obtener cotizaciones (órdenes en estado COTIZACION)
  List<Orden> findByEstadoOrdenOrderByFechaCreacionDesc(EstadoOrden estado);
}
