package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import silas.yudi.applytrackmcp.domain.model.ApplicationProcess;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.Situation;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.LocalDate;
import java.util.List;

/**
 * O processo, gravado como mapa dentro do item da candidatura. Status e quem precisa agir não são gravados:
 * são calculados a partir das etapas.
 */
@DynamoDbBean
public class ApplicationProcessItem {

    private String platform;
    private LocalDate appliedOn;
    private Situation situation;
    private String notes;
    private List<ProcessStepItem> steps;

    static ApplicationProcessItem from(ApplicationProcess process) {
        var item = new ApplicationProcessItem();
        item.setPlatform(process.platform());
        item.setAppliedOn(process.appliedOn());
        item.setSituation(process.situation());
        item.setNotes(process.notes());
        item.setSteps(toItems(process.steps()));
        return item;
    }

    ApplicationProcess toDomain() {
        return new ApplicationProcess(platform, appliedOn, situation, notes, toSteps(steps));
    }

    private static List<ProcessStepItem> toItems(List<ProcessStep> steps) {
        return steps.stream().map(ProcessStepItem::from).toList();
    }

    // Item gravado sem a lista: lista vazia
    private static List<ProcessStep> toSteps(List<ProcessStepItem> items) {
        return items == null ? List.of() : items.stream().map(ProcessStepItem::toDomain).toList();
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

    public Situation getSituation() {
        return situation;
    }

    public void setSituation(Situation situation) {
        this.situation = situation;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ProcessStepItem> getSteps() {
        return steps;
    }

    public void setSteps(List<ProcessStepItem> steps) {
        this.steps = steps;
    }
}
