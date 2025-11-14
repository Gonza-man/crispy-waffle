package cl.ubiobio.muebleria.repositories;

import cl.ubiobio.muebleria.models.Mueble;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MuebleRepository extends JpaRepository<Mueble, Integer> {

  // Soft delete: solo obtener muebles activos
  List<Mueble> findByEstadoLogicoTrue();

  // Buscar por nombre
  List<Mueble> findByNombreContainingIgnoreCaseAndEstadoLogicoTrue(String nombre);
}
