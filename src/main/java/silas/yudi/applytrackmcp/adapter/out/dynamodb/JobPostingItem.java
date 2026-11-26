package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import silas.yudi.applytrackmcp.domain.model.JobPosting;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

/** A vaga, gravada como mapa dentro do item da candidatura. */
@DynamoDbBean
public class JobPostingItem {

    private String company;
    private String finalClient;
    private String seniority;
    private String contractType;
    private String salaryRange;
    private String jobTitle;
    private String description;
    private String link;
    private List<RequirementItem> requirements;

    static JobPostingItem from(JobPosting posting) {
        var item = new JobPostingItem();
        item.setCompany(posting.company());
        item.setFinalClient(posting.finalClient());
        item.setSeniority(posting.seniority());
        item.setContractType(posting.contractType());
        item.setSalaryRange(posting.salaryRange());
        item.setJobTitle(posting.jobTitle());
        item.setDescription(posting.description());
        item.setLink(posting.link());
        item.setRequirements(posting.requirements().stream().map(RequirementItem::from).toList());
        return item;
    }

    JobPosting toDomain() {
        return new JobPosting(company, finalClient, jobTitle, seniority, contractType, salaryRange, description, link,
                requirements == null ? List.of() : requirements.stream().map(RequirementItem::toDomain).toList());
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<RequirementItem> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<RequirementItem> requirements) {
        this.requirements = requirements;
    }

    public String getFinalClient() {
        return finalClient;
    }

    public void setFinalClient(String finalClient) {
        this.finalClient = finalClient;
    }

    public String getSeniority() {
        return seniority;
    }

    public void setSeniority(String seniority) {
        this.seniority = seniority;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }
}
