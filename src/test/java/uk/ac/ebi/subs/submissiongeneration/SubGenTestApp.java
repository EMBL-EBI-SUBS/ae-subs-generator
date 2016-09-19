package uk.ac.ebi.subs.submissiongeneration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
public class SubGenTestApp {

    public static void main(String[] args) {
        SpringApplication.run(SubGenTestApp.class, args);
    }
}
