package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/** Platform é texto livre: Gupy, LinkedIn, e-mail, WhatsApp etc. */
public record JobApplication(String id, String company, String jobTitle, String description, String platform,
                             String link, LocalDate appliedOn) {

    public JobApplication {
        id = required("id", id);
        company = required("company", company);
        jobTitle = required("jobTitle", jobTitle);
        description = required("description", description);
        platform = required("platform", platform);
        link = link == null || link.isBlank() ? null : link.strip();
        if (appliedOn == null) {
            throw new IllegalArgumentException("Campo obrigatório: appliedOn");
        }
    }

    /** Candidatura nova: recebe um id aleatório. */
    public JobApplication(String company, String jobTitle, String description, String platform, String link,
                          LocalDate appliedOn) {
        this(UUID.randomUUID().toString(), company, jobTitle, description, platform, link, appliedOn);
    }

    public JobApplication update(JobApplicationChanges changes) {
        if (changes.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um campo para alterar");
        }
        return new JobApplication(
                id,
                changed(changes.company(), company),
                changed(changes.jobTitle(), jobTitle),
                changed(changes.description(), description),
                changed(changes.platform(), platform),
                changed(changes.link(), link),
                changed(changes.appliedOn(), appliedOn));
    }

    private static <T> T changed(T newValue, T currentValue) {
        return newValue == null ? currentValue : newValue;
    }

    private static String required(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: " + field);
        }
        return value.strip();
    }
}
