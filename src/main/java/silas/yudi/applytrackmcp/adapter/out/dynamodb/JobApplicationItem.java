package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import silas.yudi.applytrackmcp.domain.model.JobApplication;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;
import java.time.LocalDate;

/** Formato gravado no DynamoDB. Não sai deste adaptador. O SDK exige classe com getters e setters. */
@DynamoDbBean
public class JobApplicationItem {

    private String id;
    private String company;
    private String jobTitle;
    private String description;
    private String platform;
    private String link;
    private LocalDate appliedOn;
    private List<RequirementItem> requirements;

    static JobApplicationItem from(JobApplication application) {
        var item = new JobApplicationItem();
        item.setId(application.id());
        item.setCompany(application.company());
        item.setJobTitle(application.jobTitle());
        item.setDescription(application.description());
        item.setPlatform(application.platform());
        item.setLink(application.link());
        item.setAppliedOn(application.appliedOn());
        item.setRequirements(application.requirements().stream().map(RequirementItem::from).toList());
        return item;
    }

    JobApplication toDomain() {
        return new JobApplication(id, company, jobTitle, description, platform, link, appliedOn,
                requirements == null ? List.of() : requirements.stream().map(RequirementItem::toDomain).toList());
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public LocalDate getAppliedOn() {
        return appliedOn;
    }

    public void setAppliedOn(LocalDate appliedOn) {
        this.appliedOn = appliedOn;
    }

    public List<RequirementItem> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<RequirementItem> requirements) {
        this.requirements = requirements;
    }
}
