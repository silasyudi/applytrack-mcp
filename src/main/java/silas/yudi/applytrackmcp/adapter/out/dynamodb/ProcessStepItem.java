package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.StepState;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.LocalDate;

/** Etapa gravada dentro do processo. */
@DynamoDbBean
public class ProcessStepItem {

    private String name;
    private StepState state;
    private LocalDate scheduledFor;
    private LocalDate completedOn;
    private LocalDate feedbackExpectedBy;
    private String notes;

    static ProcessStepItem from(ProcessStep step) {
        var item = new ProcessStepItem();
        item.setName(step.name());
        item.setState(step.state());
        item.setScheduledFor(step.scheduledFor());
        item.setCompletedOn(step.completedOn());
        item.setFeedbackExpectedBy(step.feedbackExpectedBy());
        item.setNotes(step.notes());
        return item;
    }

    ProcessStep toDomain() {
        return new ProcessStep(name, state, scheduledFor, completedOn, feedbackExpectedBy, notes);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StepState getState() {
        return state;
    }

    public void setState(StepState state) {
        this.state = state;
    }

    public LocalDate getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(LocalDate scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public LocalDate getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(LocalDate completedOn) {
        this.completedOn = completedOn;
    }

    public LocalDate getFeedbackExpectedBy() {
        return feedbackExpectedBy;
    }

    public void setFeedbackExpectedBy(LocalDate feedbackExpectedBy) {
        this.feedbackExpectedBy = feedbackExpectedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
