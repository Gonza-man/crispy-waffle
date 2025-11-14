package cl.ubiobio.muebleria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class MuebleriaApplication {

  public static void main(String[] args) {
    SpringApplication.run(MuebleriaApplication.class, args);
  }

  @RequestMapping("/")
  public String healthCheck() {
    return "Healthy :D";
  }

}
