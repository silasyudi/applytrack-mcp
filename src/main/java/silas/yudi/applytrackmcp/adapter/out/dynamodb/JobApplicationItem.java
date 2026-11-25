package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import silas.yudi.applytrackmcp.domain.model.JobApplication;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.UUID;

/** Formato gravado no DynamoDB. Não sai deste adaptador. O SDK exige classe com getters e setters. */
@DynamoDbBean
public class JobApplicationItem {

    private String id;
    private String company;
    private String jobTitle;
    private String description;
    private String platform;
    private String link;

    static JobApplicationItem from(JobApplication application) {
        var item = new JobApplicationItem();
        item.setId(UUID.randomUUID().toString());
        item.setCompany(application.company());
        item.setJobTitle(application.jobTitle());
        item.setDescription(application.description());
        item.setPlatform(application.platform());
        item.setLink(application.link());
        return item;
    }

    JobApplication toDomain() {
        return new JobApplication(company, jobTitle, description, platform, link);
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
}
