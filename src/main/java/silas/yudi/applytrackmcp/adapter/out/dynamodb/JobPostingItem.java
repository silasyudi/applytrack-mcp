package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import silas.yudi.applytrackmcp.domain.model.JobPosting;
import silas.yudi.applytrackmcp.domain.model.Requirement;
import silas.yudi.applytrackmcp.domain.model.WorkModel;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

/** A vaga, gravada como mapa dentro do item da candidatura. */
@DynamoDbBean
public class JobPostingItem {

    private String company;
    private String finalClient;
    private String jobTitle;
    private String seniority;
    private String contractType;
    private String salaryRange;
    private WorkModel workModel;
    private String location;
    private String link;
    private String description;
    private List<RequirementItem> requirements;

    static JobPostingItem from(JobPosting posting) {
        var item = new JobPostingItem();
        item.setCompany(posting.company());
        item.setFinalClient(posting.finalClient());
        item.setJobTitle(posting.jobTitle());
        item.setSeniority(posting.seniority());
        item.setContractType(posting.contractType());
        item.setSalaryRange(posting.salaryRange());
        item.setWorkModel(posting.workModel());
        item.setLocation(posting.location());
        item.setLink(posting.link());
        item.setDescription(posting.description());
        item.setRequirements(toItems(posting.requirements()));
        return item;
    }

    JobPosting toDomain() {
        return new JobPosting(company, finalClient, jobTitle, seniority, contractType, salaryRange, workModel, location, link, description, toRequirements(requirements));
    }

    private static List<RequirementItem> toItems(List<Requirement> requirements) {
        return requirements.stream().map(RequirementItem::from).toList();
    }

    // Item gravado sem a lista: lista vazia
    private static List<Requirement> toRequirements(List<RequirementItem> items) {
        return items == null ? List.of() : items.stream().map(RequirementItem::toDomain).toList();
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getFinalClient() {
        return finalClient;
    }

    public void setFinalClient(String finalClient) {
        this.finalClient = finalClient;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
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

    public WorkModel getWorkModel() {
        return workModel;
    }

    public void setWorkModel(WorkModel workModel) {
        this.workModel = workModel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RequirementItem> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<RequirementItem> requirements) {
        this.requirements = requirements;
    }
}
