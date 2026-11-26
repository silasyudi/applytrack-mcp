package silas.yudi.applytrackmcp.domain.model;

import java.util.List;
import java.util.Objects;

import static silas.yudi.applytrackmcp.domain.model.Fields.changed;
import static silas.yudi.applytrackmcp.domain.model.Fields.optional;
import static silas.yudi.applytrackmcp.domain.model.Fields.required;

/**
 * A vaga como está no anúncio. Cliente final, senioridade, contrato e faixa salarial são texto livre e opcionais.
 * FinalClient é onde se trabalha de fato, quando a vaga é de consultoria ou alocação.
 */
public record JobPosting(String company,
                         String finalClient,
                         String jobTitle,
                         String seniority,
                         String contractType,
                         String salaryRange,
                         String link,
                         String description,
                         List<Requirement> requirements) {

    public JobPosting {
        company = required("company", company);
        finalClient = optional(finalClient);
        jobTitle = required("jobTitle", jobTitle);
        seniority = optional(seniority);
        contractType = optional(contractType);
        salaryRange = optional(salaryRange);

        if (salaryRange != null && contractType == null) {
            throw new IllegalArgumentException("Para informar a faixa salarial, informe também o tipo de contrato");
        }

        link = optional(link);
        description = required("description", description);
        requirements = requirements == null ? List.of() : copyOf(requirements);
    }

    /** Campos não enviados ficam como estão; o conteúdo (descrição e requisitos) chega agrupado. */
    JobPosting update(JobApplicationChanges changes) {
        var content = changes.content();
        return new JobPosting(
                changed(changes.company(), company),
                changed(changes.finalClient(), finalClient),
                changed(changes.jobTitle(), jobTitle),
                changed(changes.seniority(), seniority),
                changed(changes.contractType(), contractType),
                changed(changes.salaryRange(), salaryRange),
                changed(changes.link(), link),
                content == null ? description : changed(content.description(), description),
                content == null ? requirements : content.requirements()
        );
    }

    private static List<Requirement> copyOf(List<Requirement> requirements) {
        if (requirements.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Campo obrigatório: requirement");
        }
        return List.copyOf(requirements);
    }
}
