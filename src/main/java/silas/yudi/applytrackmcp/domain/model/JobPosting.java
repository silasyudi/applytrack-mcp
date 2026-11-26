package silas.yudi.applytrackmcp.domain.model;

import java.util.List;
import java.util.Objects;

import static silas.yudi.applytrackmcp.domain.model.Fields.optional;
import static silas.yudi.applytrackmcp.domain.model.Fields.required;

/**
 * A vaga como está no anúncio. Cliente final, senioridade, contrato e faixa salarial são texto livre e opcionais.
 * FinalClient é onde se trabalha de fato, quando a vaga é de consultoria ou alocação.
 */
public record JobPosting(String company, String finalClient, String jobTitle, String seniority, String contractType,
                         String salaryRange, String description, String link, List<Requirement> requirements) {

    public JobPosting {
        company = required("company", company);
        jobTitle = required("jobTitle", jobTitle);
        description = required("description", description);
        finalClient = optional(finalClient);
        seniority = optional(seniority);
        contractType = optional(contractType);
        salaryRange = optional(salaryRange);
        if (salaryRange != null && contractType == null) {
            throw new IllegalArgumentException("Para informar a faixa salarial, informe também o tipo de contrato");
        }
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
