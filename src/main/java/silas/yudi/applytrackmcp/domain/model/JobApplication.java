package silas.yudi.applytrackmcp.domain.model;

/** Platform é texto livre: Gupy, LinkedIn, e-mail, WhatsApp etc. */
public record JobApplication(String company, String jobTitle, String description, String platform, String link) {

    public JobApplication {
        company = required("company", company);
        jobTitle = required("jobTitle", jobTitle);
        description = required("description", description);
        platform = required("platform", platform);
        link = link == null || link.isBlank() ? null : link.strip();
    }

    private static String required(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: " + field);
        }
        return value.strip();
    }
}
