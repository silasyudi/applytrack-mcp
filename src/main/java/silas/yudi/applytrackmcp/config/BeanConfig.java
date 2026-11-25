package silas.yudi.applytrackmcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.application.service.JobApplicationService;

import java.time.Clock;
import java.time.ZoneId;

/** Cria os beans do núcleo, para que domain e application não dependam do Spring. */
@Configuration
public class BeanConfig {

    @Bean
    JobApplicationService jobApplicationService(JobApplicationRepository repository, Clock clock) {
        return new JobApplicationService(repository, clock);
    }

    // Fuso fixo: a AWS roda em UTC, e à noite já seria o dia seguinte
    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
