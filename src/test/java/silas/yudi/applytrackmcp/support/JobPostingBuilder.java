package silas.yudi.applytrackmcp.support;

import silas.yudi.applytrackmcp.domain.model.JobPosting;
import silas.yudi.applytrackmcp.domain.model.Requirement;

import java.util.List;

/**
 * Test Data Builder: começa com uma vaga válida (Acme, Java Developer, "Description", sem opcionais)
 * e cada teste muda só o que importa para ele.
 */
public final class JobPostingBuilder {

    private String company = "Acme";
    private String finalClient;
    private String jobTitle = "Java Developer";
    private String seniority;
    private String contractType;
    private String salaryRange;
    private String link;
    private String description = "Description";
    private List<Requirement> requirements = List.of();

    private JobPostingBuilder() {
    }

    public static JobPostingBuilder aPosting() {
        return new JobPostingBuilder();
    }

    public JobPostingBuilder company(String company) {
        this.company = company;
        return this;
    }

    public JobPostingBuilder finalClient(String finalClient) {
        this.finalClient = finalClient;
        return this;
    }

    public JobPostingBuilder jobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
        return this;
    }

    public JobPostingBuilder seniority(String seniority) {
        this.seniority = seniority;
        return this;
    }

    public JobPostingBuilder contractType(String contractType) {
        this.contractType = contractType;
        return this;
    }

    public JobPostingBuilder salaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
        return this;
    }

    public JobPostingBuilder link(String link) {
        this.link = link;
        return this;
    }

    public JobPostingBuilder description(String description) {
        this.description = description;
        return this;
    }

    public JobPostingBuilder requirements(List<Requirement> requirements) {
        this.requirements = requirements;
        return this;
    }

    public JobPosting build() {
        return new JobPosting(
                company,
                finalClient,
                jobTitle,
                seniority,
                contractType,
                salaryRange,
                link,
                description,
                requirements
        );
    }
}
