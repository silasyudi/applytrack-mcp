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
public record ApplicationProcess(String platform,
                                 LocalDate appliedOn,
                                 String status,
                                 LocalDate statusUpdatedOn,
                                 Situation situation,
                                 List<ProcessStep> steps) {

    public static final String DEFAULT_STATUS = "Candidatura enviada";

    public ApplicationProcess {
        platform = required("platform", platform);
        appliedOn = required("appliedOn", appliedOn);
        status = required("status", status);
        statusUpdatedOn = required("statusUpdatedOn", statusUpdatedOn);
        situation = required("situation", situation);

        if (statusUpdatedOn.isBefore(appliedOn)) {
            throw new IllegalArgumentException("A data do status não pode ser anterior à candidatura: " + statusUpdatedOn);
        }

        steps = steps == null ? List.of() : copyOf(steps);

        for (var step : steps) {
            if (step.date() != null && step.date().isBefore(appliedOn)) {
                throw new IllegalArgumentException(
                        "A etapa não pode ser anterior à candidatura: %s (%s)".formatted(step.name(), step.date()));
            }
        }
    }

    /**
     * Processo novo. Nulos recebem os padrões: status enviado, data da candidatura e ACTIVE.
     */
    public static ApplicationProcess start(String platform,
                                           LocalDate appliedOn,
                                           String status,
                                           LocalDate statusUpdatedOn,
                                           Situation situation,
                                           List<ProcessStep> steps) {
        if (optional(status) == null) {
            StatusChange.requireStatusFor(statusUpdatedOn, situation);
        }

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
        var change = changes.statusChange();
        var newStatus = change == null || change.status() == null ? status : required("status", change.status());
        var newSituation = change == null ? situation : changed(change.situation(), situation);
        var newAppliedOn = changed(changes.appliedOn(), appliedOn);
        var statusChanged = !newStatus.equals(status) || newSituation != situation;
        LocalDate newStatusDate = getNewStatusDate(change, statusChanged, today, newAppliedOn);

        return new ApplicationProcess(
                changed(changes.platform(), platform),
                newAppliedOn,
                newStatus,
                newStatusDate,
                newSituation,
                changed(changes.steps(), steps));
    }

    private LocalDate getNewStatusDate(StatusChange change, boolean statusChanged, LocalDate today, LocalDate newAppliedOn) {
        if (change != null && change.on() != null) {
            return change.on();
        }

        if (statusChanged) {
            return today;
        }

        if (statusUpdatedOn.equals(appliedOn)) {
            return newAppliedOn;
        }

        return statusUpdatedOn;
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
