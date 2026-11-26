package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;
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

    /** Today é a data da mudança, quando status ou situação mudam sem data informada. */
    public JobApplication update(JobApplicationChanges changes, LocalDate today) {
        if (changes.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um campo para alterar");
        }
        var content = changes.content();
        var updatedPosting = new JobPosting(
                changed(changes.company(), posting.company()),
                changed(changes.finalClient(), posting.finalClient()),
                changed(changes.jobTitle(), posting.jobTitle()),
                changed(changes.seniority(), posting.seniority()),
                changed(changes.contractType(), posting.contractType()),
                changed(changes.salaryRange(), posting.salaryRange()),
                content == null ? posting.description() : changed(content.description(), posting.description()),
                changed(changes.link(), posting.link()),
                content == null ? posting.requirements() : changed(content.requirements(), posting.requirements()));
        return new JobApplication(id, updatedPosting, process.update(changes, today));
    }

    private static <T> T changed(T newValue, T currentValue) {
        return newValue == null ? currentValue : newValue;
    }
}
