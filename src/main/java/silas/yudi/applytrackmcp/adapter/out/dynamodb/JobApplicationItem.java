package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import silas.yudi.applytrackmcp.domain.model.JobApplication;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** Formato gravado no DynamoDB. Não sai deste adaptador. O SDK exige classe com getters e setters. */
@DynamoDbBean
public class JobApplicationItem {

    private String id;
    private JobPostingItem posting;
    private ApplicationProcessItem process;

    static JobApplicationItem from(JobApplication application) {
        var item = new JobApplicationItem();
        item.setId(application.id());
        item.setPosting(JobPostingItem.from(application.posting()));
        item.setProcess(ApplicationProcessItem.from(application.process()));
        return item;
    }

    JobApplication toDomain() {
        return new JobApplication(id, posting.toDomain(), process.toDomain());
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JobPostingItem getPosting() {
        return posting;
    }

    public void setPosting(JobPostingItem posting) {
        this.posting = posting;
    }

    public ApplicationProcessItem getProcess() {
        return process;
    }

    public void setProcess(ApplicationProcessItem process) {
        this.process = process;
    }
}
