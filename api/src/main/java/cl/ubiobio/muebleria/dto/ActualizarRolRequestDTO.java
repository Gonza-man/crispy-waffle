package cl.ubiobio.muebleria.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarRolRequestDTO {
    private String rol; // "ADMIN" or "USER"
}
