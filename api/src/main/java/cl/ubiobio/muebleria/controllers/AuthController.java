package cl.ubiobio.muebleria.controllers;

import cl.ubiobio.muebleria.dto.AuthResponseDTO;
import cl.ubiobio.muebleria.dto.LoginRequestDTO;
import cl.ubiobio.muebleria.dto.RegisterRequestDTO;
import cl.ubiobio.muebleria.enums.Rol;
import cl.ubiobio.muebleria.models.Usuario;
import cl.ubiobio.muebleria.repositories.UsuarioRepository;
import cl.ubiobio.muebleria.security.CustomUserDetailsService;
import cl.ubiobio.muebleria.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            Usuario usuario = userDetailsService.getUserByUsername(loginRequest.getUsername());

            return ResponseEntity.ok(new AuthResponseDTO(jwt, usuario.getUsername(), usuario.getRol().name()));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Credenciales inválidas");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO registerRequest) {
        // Validate username not taken
        if (usuarioRepository.existsByUsername(registerRequest.getUsername())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "El nombre de usuario ya existe");
            return ResponseEntity.badRequest().body(error);
        }

        // Validate email not taken
        if (usuarioRepository.existsByEmail(registerRequest.getEmail())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "El email ya está registrado");
            return ResponseEntity.badRequest().body(error);
        }

        // Create new user with USER role
        Usuario usuario = new Usuario();
        usuario.setUsername(registerRequest.getUsername());
        usuario.setEmail(registerRequest.getEmail());
        usuario.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        usuario.setRol(Rol.USER);
        usuario.setActivo(true);

        usuarioRepository.save(usuario);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Usuario registrado exitosamente");
        response.put("username", usuario.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Usuario usuario = userDetailsService.getUserByUsername(authentication.getName());
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", usuario.getId());
        userInfo.put("username", usuario.getUsername());
        userInfo.put("email", usuario.getEmail());
        userInfo.put("rol", usuario.getRol().name());

        return ResponseEntity.ok(userInfo);
    }
}
