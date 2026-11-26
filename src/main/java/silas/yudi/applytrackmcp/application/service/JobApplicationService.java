package silas.yudi.applytrackmcp.application.service;

import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.domain.exception.JobApplicationNotFoundException;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.JobApplicationChanges;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class JobApplicationService {

    private static final Comparator<JobApplication> MOST_RECENT_FIRST =
            Comparator.comparing((JobApplication application) -> application.process().appliedOn()).reversed();

    private final JobApplicationRepository repository;
    private final Clock clock;

    public JobApplicationService(JobApplicationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void register(JobApplication application) {
        rejectFuture("A data da candidatura", application.process().appliedOn());
        rejectFuture("A data do status", application.process().statusUpdatedOn());
        rejectDoneStepsInTheFuture(application.process().steps());
        repository.save(application);
    }

    /**
     * Busca por trecho da empresa, do cliente final, do cargo, da descrição ou do nome de um requisito;
     * sem termo, lista tudo. Mais recentes primeiro.
     */
    public List<JobApplication> search(String term) {
        var normalizedTerm = term == null ? "" : normalize(term);
        return repository.findAll().stream()
                .filter(application -> normalize(application.posting().company()).contains(normalizedTerm)
                        || normalize(application.posting().finalClient()).contains(normalizedTerm)
                        || normalize(application.posting().jobTitle()).contains(normalizedTerm)
                        || normalize(application.posting().description()).contains(normalizedTerm)
                        || application.posting().requirements().stream()
                                .anyMatch(requirement -> normalize(requirement.name()).contains(normalizedTerm)))
                .sorted(MOST_RECENT_FIRST)
                .toList();
    }

    public JobApplication get(String id) {
        requireId(id);
        return find(id);
    }

    public JobApplication update(String id, JobApplicationChanges changes) {
        requireId(id);
        // Só a data nova: um registro com data errada ainda pode ter os outros campos corrigidos
        if (changes.appliedOn() != null) {
            rejectFuture("A data da candidatura", changes.appliedOn());
        }
        if (changes.statusChange() != null && changes.statusChange().on() != null) {
            rejectFuture("A data do status", changes.statusChange().on());
        }
        if (changes.steps() != null) {
            rejectDoneStepsInTheFuture(changes.steps());
        }
        var updated = find(id).update(changes, LocalDate.now(clock));
        repository.save(updated);
        return updated;
    }

    /** Devolve a candidatura removida. */
    public JobApplication delete(String id) {
        requireId(id);
        var application = find(id);
        repository.deleteById(id);
        return application;
    }

    private static void requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: id");
        }
    }

    private JobApplication find(String id) {
        return repository.findById(id).orElseThrow(() -> new JobApplicationNotFoundException(id));
    }

    private void rejectFuture(String what, LocalDate date) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException(what + " não pode ser futura: " + date);
        }
    }

    private void rejectDoneStepsInTheFuture(List<ProcessStep> steps) {
        var today = LocalDate.now(clock);
        for (var step : steps) {
            if (step.done() && step.date() != null && step.date().isAfter(today)) {
                throw new IllegalArgumentException("Etapa marcada como feita não pode ter data futura: %s (%s)"
                        .formatted(step.name(), step.date()));
            }
        }
    }

    // Ignora acento, caixa e espaços nas pontas
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
