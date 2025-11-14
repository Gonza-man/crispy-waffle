package cl.ubiobio.muebleria.services;

import cl.ubiobio.muebleria.dto.VarianteAdicionalDTO;
import cl.ubiobio.muebleria.dto.VarianteAdicionalRequestDTO;
import cl.ubiobio.muebleria.models.VarianteAdicional;
import cl.ubiobio.muebleria.repositories.VarianteAdicionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VarianteAdicionalService {

  private final VarianteAdicionalRepository varianteRepository;

  public VarianteAdicionalService(VarianteAdicionalRepository varianteRepository) {
    this.varianteRepository = varianteRepository;
  }

  @Transactional(readOnly = true)
  public List<VarianteAdicionalDTO> listarVariantesActivas() {
    return varianteRepository.findByActivoTrue().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public VarianteAdicionalDTO obtenerPorId(Integer id) {
    VarianteAdicional variante = varianteRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Variante no encontrada con ID: " + id));
    return toDTO(variante);
  }

  @Transactional
  public VarianteAdicionalDTO crear(VarianteAdicionalRequestDTO request) {
    VarianteAdicional variante = new VarianteAdicional();
    variante.setNombre(request.getNombre());
    variante.setCostoExtra(request.getCostoExtra());
    variante.setTipoAplicacion(request.getTipoAplicacion());
    variante.setActivo(true);

    VarianteAdicional guardada = varianteRepository.save(variante);
    return toDTO(guardada);
  }

  @Transactional
  public VarianteAdicionalDTO actualizar(Integer id, VarianteAdicionalRequestDTO request) {
    VarianteAdicional variante = varianteRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Variante no encontrada con ID: " + id));

    variante.setNombre(request.getNombre());
    variante.setCostoExtra(request.getCostoExtra());
    variante.setTipoAplicacion(request.getTipoAplicacion());

    VarianteAdicional actualizada = varianteRepository.save(variante);
    return toDTO(actualizada);
  }

  @Transactional
  public void eliminar(Integer id) {
    VarianteAdicional variante = varianteRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Variante no encontrada con ID: " + id));

    // Soft delete
    variante.setActivo(false);
    varianteRepository.save(variante);
  }

  // Mapper
  private VarianteAdicionalDTO toDTO(VarianteAdicional variante) {
    VarianteAdicionalDTO dto = new VarianteAdicionalDTO();
    dto.setIdVariante(variante.getIdVariante());
    dto.setNombre(variante.getNombre());
    dto.setCostoExtra(variante.getCostoExtra());
    dto.setTipoAplicacion(variante.getTipoAplicacion());
    dto.setActivo(variante.getActivo());
    return dto;
  }
}
