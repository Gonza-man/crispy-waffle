package cl.ubiobio.muebleria.controllers;

import cl.ubiobio.muebleria.dto.ActualizarRolRequestDTO;
import cl.ubiobio.muebleria.dto.UsuarioDTO;
import cl.ubiobio.muebleria.enums.Rol;
import cl.ubiobio.muebleria.models.Usuario;
import cl.ubiobio.muebleria.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Listar todos los usuarios (solo ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        List<UsuarioDTO> usuarios = usuarioRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Actualizar rol de un usuario (solo ADMIN)
     * Permite promover USER -> ADMIN o degradar ADMIN -> USER
     */
    @PutMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizarRol(@PathVariable Long id,
                                          @RequestBody ActualizarRolRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar que el rol sea válido
        try {
            Rol nuevoRol = Rol.valueOf(request.getRol().toUpperCase());
            usuario.setRol(nuevoRol);
            usuarioRepository.save(usuario);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Rol actualizado exitosamente");
            response.put("username", usuario.getUsername());
            response.put("nuevoRol", nuevoRol.name());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Rol inválido. Use 'ADMIN' o 'USER'");
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Activar/desactivar usuario (soft delete)
     */
    @PutMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setActivo(!usuario.getActivo());
        usuarioRepository.save(usuario);

        Map<String, Object> response = new HashMap<>();
        response.put("message", usuario.getActivo() ? "Usuario activado" : "Usuario desactivado");
        response.put("activo", usuario.getActivo());

        return ResponseEntity.ok(response);
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setUsername(usuario.getUsername());
        dto.setEmail(usuario.getEmail());
        dto.setRol(usuario.getRol().name());
        dto.setActivo(usuario.getActivo());
        dto.setFechaCreacion(usuario.getFechaCreacion());
        return dto;
    }
}
