package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import silas.yudi.applytrackmcp.domain.model.ApplicationProcess;
import silas.yudi.applytrackmcp.domain.model.Situation;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.LocalDate;
import java.util.List;

/** O processo, gravado como mapa dentro do item da candidatura. */
@DynamoDbBean
public class ApplicationProcessItem {

    private String platform;
    private LocalDate appliedOn;
    private String status;
    private LocalDate statusUpdatedOn;
    private Situation situation;
    private List<ProcessStepItem> steps;

    static ApplicationProcessItem from(ApplicationProcess process) {
        var item = new ApplicationProcessItem();
        item.setPlatform(process.platform());
        item.setAppliedOn(process.appliedOn());
        item.setStatus(process.status());
        item.setStatusUpdatedOn(process.statusUpdatedOn());
        item.setSituation(process.situation());
        item.setSteps(process.steps().stream().map(ProcessStepItem::from).toList());
        return item;
    }

    ApplicationProcess toDomain() {
        return new ApplicationProcess(platform, appliedOn, status, statusUpdatedOn, situation,
                steps == null ? List.of() : steps.stream().map(ProcessStepItem::toDomain).toList());
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public LocalDate getAppliedOn() {
        return appliedOn;
    }

    public void setAppliedOn(LocalDate appliedOn) {
        this.appliedOn = appliedOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStatusUpdatedOn() {
        return statusUpdatedOn;
    }

    public void setStatusUpdatedOn(LocalDate statusUpdatedOn) {
        this.statusUpdatedOn = statusUpdatedOn;
    }

    public Situation getSituation() {
        return situation;
    }

    public void setSituation(Situation situation) {
        this.situation = situation;
    }

    public List<ProcessStepItem> getSteps() {
        return steps;
    }

    public void setSteps(List<ProcessStepItem> steps) {
        this.steps = steps;
    }
}
