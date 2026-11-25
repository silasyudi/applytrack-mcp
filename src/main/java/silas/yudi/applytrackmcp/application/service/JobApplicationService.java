package silas.yudi.applytrackmcp.application.service;

import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.domain.model.JobApplication;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class JobApplicationService {

    private final JobApplicationRepository repository;

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    public void register(JobApplication application) {
        repository.save(application);
    }

    /** Busca por trecho da empresa ou do cargo; sem termo, lista tudo. */
    public List<JobApplication> search(String term) {
        var normalizedTerm = term == null ? "" : normalize(term);
        return repository.findAll().stream()
                .filter(application -> normalize(application.company()).contains(normalizedTerm)
                        || normalize(application.jobTitle()).contains(normalizedTerm))
                .toList();
    }

    // Ignora acento, caixa e espaços nas pontas
    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
