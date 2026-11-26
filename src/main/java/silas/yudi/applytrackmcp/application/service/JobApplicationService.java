package silas.yudi.applytrackmcp.application.service;

import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.domain.exception.JobApplicationNotFoundException;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.JobApplicationChanges;
import silas.yudi.applytrackmcp.domain.model.JobPosting;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.StepChange;

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
        rejectFutureCompletion(application.process().steps());
        repository.save(application);
    }

    /**
     * Busca por trecho da empresa, do cliente final, do cargo, da descrição ou do nome de um requisito;
     * sem termo, lista tudo. Mais recentes primeiro.
     */
    public List<JobApplication> search(String term) {
        var normalizedTerm = normalize(term);
        return repository.findAll()
                .stream()
                .filter(application -> matches(application.posting(), normalizedTerm))
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
        if (changes.steps() != null) {
            rejectFutureCompletion(changes.steps());
        }
        var updated = find(id).update(changes);
        repository.save(updated);
        return updated;
    }

    public JobApplication addStep(String id, ProcessStep step) {
        requireId(id);
        rejectFutureCompletion(step.name(), step.completedOn());
        var updated = find(id).addStep(step);
        repository.save(updated);
        return updated;
    }

    public JobApplication updateStep(String id, String stepName, StepChange change) {
        requireId(id);
        rejectFutureCompletion(stepName, change.completedOn());
        var updated = find(id).updateStep(stepName, change);
        repository.save(updated);
        return updated;
    }

    /**
     * Devolve a candidatura removida.
     */
    public JobApplication delete(String id) {
        requireId(id);
        var application = find(id);
        repository.deleteById(id);
        return application;
    }

    private static boolean matches(JobPosting posting, String normalizedTerm) {
        return normalize(posting.company()).contains(normalizedTerm)
                || normalize(posting.finalClient()).contains(normalizedTerm)
                || normalize(posting.jobTitle()).contains(normalizedTerm)
                || normalize(posting.description()).contains(normalizedTerm)
                || posting.requirements()
                .stream()
                .anyMatch(requirement -> normalize(requirement.name()).contains(normalizedTerm));
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

    private void rejectFutureCompletion(List<ProcessStep> steps) {
        steps.forEach(step -> rejectFutureCompletion(step.name(), step.completedOn()));
    }

    // Marcada pode ser futura; concluída não
    private void rejectFutureCompletion(String stepName, LocalDate completedOn) {
        if (completedOn != null && completedOn.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException(
                    "A etapa não pode ter conclusão futura: %s (%s)".formatted(stepName.strip(), completedOn));
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
