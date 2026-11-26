package silas.yudi.applytrackmcp.domain.model;

import java.util.List;
import java.util.Objects;

import static silas.yudi.applytrackmcp.domain.model.Fields.optional;
import static silas.yudi.applytrackmcp.domain.model.Fields.required;

/** A vaga como está no anúncio. */
public record JobPosting(String company, String jobTitle, String description, String link,
                         List<Requirement> requirements) {

    public JobPosting {
        company = required("company", company);
        jobTitle = required("jobTitle", jobTitle);
        description = required("description", description);
        link = optional(link);
        requirements = requirements == null ? List.of() : copyOf(requirements);
    }

    private static List<Requirement> copyOf(List<Requirement> requirements) {
        if (requirements.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Campo obrigatório: requirement");
        }
        return List.copyOf(requirements);
    }
}
