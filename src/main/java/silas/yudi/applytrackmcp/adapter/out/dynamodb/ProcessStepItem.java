package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.LocalDate;

/** Etapa gravada dentro do processo. Boolean, e não boolean: o SDK espera getDone(). */
@DynamoDbBean
public class ProcessStepItem {

    private String name;
    private LocalDate date;
    private Boolean done;
    private String notes;

    static ProcessStepItem from(ProcessStep step) {
        var item = new ProcessStepItem();
        item.setName(step.name());
        item.setDate(step.date());
        item.setDone(step.done());
        item.setNotes(step.notes());
        return item;
    }

    ProcessStep toDomain() {
        return new ProcessStep(name, date, Boolean.TRUE.equals(done), notes);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
