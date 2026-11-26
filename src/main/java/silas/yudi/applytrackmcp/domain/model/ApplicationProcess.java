package silas.yudi.applytrackmcp.domain.model;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static silas.yudi.applytrackmcp.domain.model.Fields.changed;
import static silas.yudi.applytrackmcp.domain.model.Fields.optional;
import static silas.yudi.applytrackmcp.domain.model.Fields.required;

/**
 * O que acontece com a candidatura. A situação é manual; status, quem precisa agir e a última atividade
 * são calculados a partir das etapas.
 */
public record ApplicationProcess(String platform,
                                 LocalDate appliedOn,
                                 Situation situation,
                                 String notes,
                                 List<ProcessStep> steps) {

    public ApplicationProcess {
        platform = required("platform", platform);
        appliedOn = required("appliedOn", appliedOn);
        situation = required("situation", situation);
        notes = optional(notes);
        steps = steps == null ? List.of() : copyOf(steps);

        rejectRepeatedNames(steps);
        rejectStepsBefore(appliedOn, steps);
    }

    /** Processo novo. Situação nula vira ACTIVE. */
    public static ApplicationProcess start(String platform,
                                           LocalDate appliedOn,
                                           Situation situation,
                                           String notes,
                                           List<ProcessStep> steps) {
        return new ApplicationProcess(platform, appliedOn, changed(situation, Situation.ACTIVE), notes, steps);
    }

    public static ApplicationProcess start(String platform, LocalDate appliedOn) {
        return start(platform, appliedOn, Situation.ACTIVE, null, null);
    }

    public String status() {
        return switch (situation) {
            case OFFER -> "Proposta recebida";
            case HIRED -> "Contratado";
            case REJECTED -> "Reprovado";
            case WITHDRAWN -> "Desistência";
            case CLOSED -> "Vaga fechada";
            case ACTIVE -> activeStatus();
        };
    }

    public WaitingFor waitingFor() {
        return switch (situation) {
            case OFFER -> WaitingFor.ME;
            case ACTIVE -> currentStep().filter(step -> step.state() == StepState.SCHEDULED).isPresent()
                    ? WaitingFor.ME
                    : WaitingFor.COMPANY;
            default -> WaitingFor.NOTHING;
        };
    }

    /** A candidatura ou a conclusão de etapa mais recente. Datas marcadas e retornos prometidos não contam. */
    public LocalDate lastActivityOn() {
        return Stream.concat(Stream.of(appliedOn), steps.stream().map(ProcessStep::completedOn))
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElseThrow();
    }

    /** A primeira etapa marcada; sem ela, a última aguardando retorno. Algo a fazer pesa mais que esperar. */
    public Optional<ProcessStep> currentStep() {
        return steps.stream()
                .filter(step -> step.state() == StepState.SCHEDULED)
                .findFirst()
                .or(() -> steps.reversed().stream()
                        .filter(step -> step.state() == StepState.AWAITING_FEEDBACK)
                        .findFirst());
    }

    /** Campos não enviados ficam como estão; observação em branco remove; etapas substituem a lista toda. */
    ApplicationProcess update(JobApplicationChanges changes) {
        return new ApplicationProcess(
                changed(changes.platform(), platform),
                changed(changes.appliedOn(), appliedOn),
                changed(changes.situation(), situation),
                changed(changes.notes(), notes),
                changed(changes.steps(), steps)
        );
    }

    ApplicationProcess addStep(ProcessStep step) {
        var updated = new ArrayList<>(steps);
        updated.add(step);
        return withSteps(updated);
    }

    ApplicationProcess updateStep(String name, StepChange change) {
        var given = required("step.name", name);
        var index = indexOf(given);
        var updated = new ArrayList<>(steps);
        updated.set(index, steps.get(index).update(change));
        return withSteps(updated);
    }

    private String activeStatus() {
        return currentStep()
                .map(ApplicationProcess::currentStepStatus)
                .orElseGet(() -> steps.isEmpty()
                        ? "Aguardando contato após a candidatura"
                        : closedStepStatus(steps.getLast()));
    }

    private static String currentStepStatus(ProcessStep step) {
        if (step.state() == StepState.SCHEDULED) {
            return step.scheduledFor() == null
                    ? "Próxima etapa: " + step.name()
                    : "Próxima etapa: %s em %s".formatted(step.name(), step.scheduledFor());
        }
        return step.feedbackExpectedBy() == null
                ? "Aguardando feedback de " + step.name()
                : "Aguardando feedback de %s (prometido até %s)".formatted(step.name(), step.feedbackExpectedBy());
    }

    private static String closedStepStatus(ProcessStep step) {
        return switch (step.state()) {
            case PASSED -> "Aguardando contato após aprovação em " + step.name();
            case CANCELLED -> "Aguardando contato após cancelamento de " + step.name();
            case FAILED -> "Reprovação em " + step.name();
            case SCHEDULED, AWAITING_FEEDBACK -> throw new IllegalStateException("Etapa em aberto: " + step.name());
        };
    }

    private int indexOf(String name) {
        var key = normalize(name);
        for (int i = 0; i < steps.size(); i++) {
            if (normalize(steps.get(i).name()).equals(key)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Etapa não encontrada: " + name);
    }

    private ApplicationProcess withSteps(List<ProcessStep> updated) {
        return new ApplicationProcess(platform, appliedOn, situation, notes, updated);
    }

    private static List<ProcessStep> copyOf(List<ProcessStep> steps) {
        if (steps.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Campo obrigatório: step");
        }
        return List.copyOf(steps);
    }

    private static void rejectRepeatedNames(List<ProcessStep> steps) {
        var seen = new HashSet<String>();
        for (var step : steps) {
            if (!seen.add(normalize(step.name()))) {
                throw new IllegalArgumentException("Etapa repetida: " + step.name());
            }
        }
    }

    private static void rejectStepsBefore(LocalDate appliedOn, List<ProcessStep> steps) {
        for (var step : steps) {
            Stream.of(step.scheduledFor(), step.completedOn(), step.feedbackExpectedBy())
                    .filter(date -> date != null && date.isBefore(appliedOn))
                    .findFirst()
                    .ifPresent(date -> {
                        throw new IllegalArgumentException(
                                "A etapa não pode ser anterior à candidatura: %s (%s)".formatted(step.name(), date));
                    });
        }
    }

    /** Nome comparável: sem acentos, minúsculo e sem espaços nas pontas. */
    private static String normalize(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
