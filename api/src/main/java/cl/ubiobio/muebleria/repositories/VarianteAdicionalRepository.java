package cl.ubiobio.muebleria.repositories;

import cl.ubiobio.muebleria.models.VarianteAdicional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VarianteAdicionalRepository extends JpaRepository<VarianteAdicional, Integer> {

  // Soft delete: solo obtener variantes activas
  List<VarianteAdicional> findByActivoTrue();
}
