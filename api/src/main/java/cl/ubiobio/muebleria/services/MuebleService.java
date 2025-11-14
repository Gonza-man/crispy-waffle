package cl.ubiobio.muebleria.services;

import cl.ubiobio.muebleria.dto.MuebleDTO;
import cl.ubiobio.muebleria.dto.MuebleRequestDTO;
import cl.ubiobio.muebleria.models.Mueble;
import cl.ubiobio.muebleria.repositories.MuebleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MuebleService {

  private final MuebleRepository muebleRepository;

  public MuebleService(MuebleRepository muebleRepository) {
    this.muebleRepository = muebleRepository;
  }

  @Transactional(readOnly = true)
  public List<MuebleDTO> listarMueblesActivos() {
    return muebleRepository.findByEstadoLogicoTrue().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public MuebleDTO obtenerPorId(Integer id) {
    Mueble mueble = muebleRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Mueble no encontrado con ID: " + id));
    return toDTO(mueble);
  }

  @Transactional
  public MuebleDTO crear(MuebleRequestDTO request) {
    Mueble mueble = new Mueble();
    mueble.setNombre(request.getNombre());
    mueble.setTipo(request.getTipo());
    mueble.setPrecioBase(request.getPrecioBase());
    mueble.setStock(request.getStock());
    mueble.setTamano(request.getTamano());
    mueble.setMaterial(request.getMaterial());
    mueble.setEstadoLogico(true);

    Mueble guardado = muebleRepository.save(mueble);
    return toDTO(guardado);
  }

  @Transactional
  public MuebleDTO actualizar(Integer id, MuebleRequestDTO request) {
    Mueble mueble = muebleRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Mueble no encontrado con ID: " + id));

    mueble.setNombre(request.getNombre());
    mueble.setTipo(request.getTipo());
    mueble.setPrecioBase(request.getPrecioBase());
    mueble.setStock(request.getStock());
    mueble.setTamano(request.getTamano());
    mueble.setMaterial(request.getMaterial());

    Mueble actualizado = muebleRepository.save(mueble);
    return toDTO(actualizado);
  }

  @Transactional
  public void eliminar(Integer id) {
    Mueble mueble = muebleRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Mueble no encontrado con ID: " + id));

    // Soft delete
    mueble.setEstadoLogico(false);
    muebleRepository.save(mueble);
  }

  @Transactional(readOnly = true)
  public List<MuebleDTO> buscarPorNombre(String nombre) {
    return muebleRepository.findByNombreContainingIgnoreCaseAndEstadoLogicoTrue(nombre).stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  // Mapper
  private MuebleDTO toDTO(Mueble mueble) {
    MuebleDTO dto = new MuebleDTO();
    dto.setIdMueble(mueble.getIdMueble());
    dto.setNombre(mueble.getNombre());
    dto.setTipo(mueble.getTipo());
    dto.setPrecioBase(mueble.getPrecioBase());
    dto.setStock(mueble.getStock());
    dto.setTamano(mueble.getTamano());
    dto.setMaterial(mueble.getMaterial());
    dto.setEstadoLogico(mueble.getEstadoLogico());
    return dto;
  }
}
