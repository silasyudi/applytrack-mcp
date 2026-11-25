package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;

/** Platform é texto livre: Gupy, LinkedIn, e-mail, WhatsApp etc. */
public record JobApplication(String company, String jobTitle, String description, String platform, String link,
                             LocalDate appliedOn) {

    public JobApplication {
        company = required("company", company);
        jobTitle = required("jobTitle", jobTitle);
        description = required("description", description);
        platform = required("platform", platform);
        link = link == null || link.isBlank() ? null : link.strip();
        if (appliedOn == null) {
            throw new IllegalArgumentException("Campo obrigatório: appliedOn");
        }
    }

    private static String required(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: " + field);
        }
        return value.strip();
    }
}
