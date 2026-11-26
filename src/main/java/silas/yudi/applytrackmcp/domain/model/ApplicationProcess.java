package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static silas.yudi.applytrackmcp.domain.model.Fields.optional;
import static silas.yudi.applytrackmcp.domain.model.Fields.required;

/**
 * O que acontece com a candidatura. Platform e status são texto livre.
 * StatusUpdatedOn é a data da última mudança de status ou de situação.
 */
public record ApplicationProcess(String platform, LocalDate appliedOn, String status, LocalDate statusUpdatedOn,
                                 Situation situation, List<ProcessStep> steps) {

    public static final String DEFAULT_STATUS = "Candidatura enviada";

    public ApplicationProcess {
        platform = required("platform", platform);
        if (appliedOn == null) {
            throw new IllegalArgumentException("Campo obrigatório: appliedOn");
        }
        status = required("status", status);
        if (statusUpdatedOn == null) {
            throw new IllegalArgumentException("Campo obrigatório: statusUpdatedOn");
        }
        if (situation == null) {
            throw new IllegalArgumentException("Campo obrigatório: situation");
        }
        if (statusUpdatedOn.isBefore(appliedOn)) {
            throw new IllegalArgumentException("A data do status não pode ser anterior à candidatura: " + statusUpdatedOn);
        }
        steps = steps == null ? List.of() : copyOf(steps);
    }

    /** Processo novo. Nulos recebem os padrões: status enviado, data da candidatura e ACTIVE. */
    public static ApplicationProcess start(String platform, LocalDate appliedOn, String status,
                                           LocalDate statusUpdatedOn, Situation situation, List<ProcessStep> steps) {
        return new ApplicationProcess(
                platform,
                appliedOn,
                Objects.requireNonNullElse(optional(status), DEFAULT_STATUS),
                statusUpdatedOn == null ? appliedOn : statusUpdatedOn,
                Objects.requireNonNullElse(situation, Situation.ACTIVE),
                steps);
    }

    public static ApplicationProcess start(String platform, LocalDate appliedOn) {
        return start(platform, appliedOn, null, null, null, null);
    }

    /**
     * Mudar status ou situação leva a data do status para hoje, salvo se a data vier informada.
     * Enquanto o status nunca mudou (data do status igual à da candidatura), ela acompanha a data da candidatura.
     */
    ApplicationProcess update(JobApplicationChanges changes, LocalDate today) {
        var newStatus = changes.status() == null ? status : required("status", changes.status());
        var newSituation = changed(changes.situation(), situation);
        var newAppliedOn = changed(changes.appliedOn(), appliedOn);
        var statusChanged = !newStatus.equals(status) || newSituation != situation;

        LocalDate newStatusDate;
        if (changes.statusUpdatedOn() != null) {
            newStatusDate = changes.statusUpdatedOn();
        } else if (statusChanged) {
            newStatusDate = today;
        } else if (statusUpdatedOn.equals(appliedOn)) {
            newStatusDate = newAppliedOn;
        } else {
            newStatusDate = statusUpdatedOn;
        }
        return new ApplicationProcess(changed(changes.platform(), platform), newAppliedOn, newStatus, newStatusDate,
                newSituation, changed(changes.steps(), steps));
    }

    private static <T> T changed(T newValue, T currentValue) {
        return newValue == null ? currentValue : newValue;
    }

    private static List<ProcessStep> copyOf(List<ProcessStep> steps) {
        if (steps.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Campo obrigatório: step");
        }
        return List.copyOf(steps);
    }
}
