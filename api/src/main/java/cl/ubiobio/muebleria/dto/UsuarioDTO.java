package cl.ubiobio.muebleria.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private Long id;
    private String username;
    private String email;
    private String rol;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
