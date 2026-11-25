package silas.yudi.applytrackmcp.application.service;

import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.domain.exception.JobApplicationNotFoundException;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.JobApplicationChanges;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class JobApplicationService {

    private static final Comparator<JobApplication> MOST_RECENT_FIRST =
            Comparator.comparing(JobApplication::appliedOn).reversed();

    private final JobApplicationRepository repository;
    private final Clock clock;

    public JobApplicationService(JobApplicationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void register(JobApplication application) {
        rejectFuture(application.appliedOn());
        repository.save(application);
    }

    public JobApplication update(String id, JobApplicationChanges changes) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: id");
        }
        // Só a data nova: um registro com data errada ainda pode ter os outros campos corrigidos
        if (changes.appliedOn() != null) {
            rejectFuture(changes.appliedOn());
        }
        var updated = repository.findById(id)
                .orElseThrow(() -> new JobApplicationNotFoundException(id))
                .update(changes);
        repository.save(updated);
        return updated;
    }

    private void rejectFuture(LocalDate appliedOn) {
        if (appliedOn.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("A data da candidatura não pode ser futura: " + appliedOn);
        }
    }

    /** Busca por trecho da empresa ou do cargo; sem termo, lista tudo. Mais recentes primeiro. */
    public List<JobApplication> search(String term) {
        var normalizedTerm = term == null ? "" : normalize(term);
        return repository.findAll().stream()
                .filter(application -> normalize(application.company()).contains(normalizedTerm)
                        || normalize(application.jobTitle()).contains(normalizedTerm))
                .sorted(MOST_RECENT_FIRST)
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
