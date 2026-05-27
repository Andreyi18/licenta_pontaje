package ro.upt.pontaje;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * UPT Pontaje - Sistem integrat de management al orarelor, pontajelor si anexelor salariale
 * Aplicatie web pentru Universitatea Politehnica Timisoara care automatizeaza
 * fluxul de completare pontaje si anexe salariale pentru cadrele didactice.
 */
@SpringBootApplication
@EnableScheduling
public class PontajeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PontajeApplication.class, args);
    }
}
