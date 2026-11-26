package silas.yudi.applytrackmcp.domain.model;

import java.util.UUID;

import static silas.yudi.applytrackmcp.domain.model.Fields.required;

/** Candidatura: a vaga (posting) e o que acontece com ela (process). */
public record JobApplication(String id, JobPosting posting, ApplicationProcess process) {

    public JobApplication {
        id = required("id", id);

        if (posting == null) {
            throw new IllegalArgumentException("Campo obrigatório: posting");
        }

        if (process == null) {
            throw new IllegalArgumentException("Campo obrigatório: process");
        }
    }

    /** Candidatura nova: recebe um id aleatório. */
    public JobApplication(JobPosting posting, ApplicationProcess process) {
        this(UUID.randomUUID().toString(), posting, process);
    }

    public JobApplication update(JobApplicationChanges changes) {
        if (changes.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um campo para alterar");
        }

        return new JobApplication(id, posting.update(changes), process.update(changes));
    }

    public JobApplication addStep(ProcessStep step) {
        return new JobApplication(id, posting, process.addStep(step));
    }

    /** Localiza a etapa pelo nome, sem diferenciar acentos e maiúsculas. */
    public JobApplication updateStep(String name, StepChange change) {
        return new JobApplication(id, posting, process.updateStep(name, change));
    }
}
