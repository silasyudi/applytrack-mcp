package silas.yudi.applytrackmcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.application.service.JobApplicationService;

/** Cria os beans do núcleo, para que domain e application não dependam do Spring. */
@Configuration
public class BeanConfig {

    @Bean
    JobApplicationService jobApplicationService(JobApplicationRepository repository) {
        return new JobApplicationService(repository);
    }
}
