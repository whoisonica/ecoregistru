package ro.ecoregistru;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EcoRegistruApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcoRegistruApplication.class, args);
    }
}
