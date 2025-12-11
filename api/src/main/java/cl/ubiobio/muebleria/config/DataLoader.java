package cl.ubiobio.muebleria.config;

import cl.ubiobio.muebleria.enums.Rol;
import cl.ubiobio.muebleria.models.Usuario;
import cl.ubiobio.muebleria.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Value("${admin.email:admin@muebleria.cl}")
    private String adminEmail;

    @Override
    public void run(String... args) throws Exception {
        // Check if any admin exists
        boolean adminExists = usuarioRepository.findAll().stream()
                .anyMatch(u -> u.getRol() == Rol.ADMIN);

        if (!adminExists) {
            // Create default admin user
            Usuario admin = new Usuario();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setEmail(adminEmail);
            admin.setRol(Rol.ADMIN);
            admin.setActivo(true);

            usuarioRepository.save(admin);

            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║  ADMIN USER CREATED                                          ║");
            System.out.println("║  Username: " + adminUsername + "                                               ║");
            System.out.println("║  Password: " + adminPassword + "                                          ║");
            System.out.println("║  Email: " + adminEmail + "                              ║");
            System.out.println("║                                                              ║");
            System.out.println("║  ⚠️  CHANGE DEFAULT PASSWORD IN PRODUCTION!                  ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
        }
    }
}
