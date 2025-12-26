package watermap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class WatermapApplication {

    public static void main(String[] args) {
        SpringApplication.run(WatermapApplication.class, args);
    }

    @Bean
    CommandLineRunner runProcessor(WatermapProcessor processor) {
        return args -> processor.run();
    }
}
