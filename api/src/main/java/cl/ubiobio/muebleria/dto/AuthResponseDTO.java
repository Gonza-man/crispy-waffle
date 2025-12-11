package cl.ubiobio.muebleria.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String type = "Bearer";
    private String username;
    private String rol;

    public AuthResponseDTO(String token, String username, String rol) {
        this.token = token;
        this.username = username;
        this.rol = rol;
    }
}
