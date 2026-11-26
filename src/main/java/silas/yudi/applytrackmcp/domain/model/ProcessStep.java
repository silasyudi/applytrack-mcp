package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;

import static silas.yudi.applytrackmcp.domain.model.Fields.changed;
import static silas.yudi.applytrackmcp.domain.model.Fields.optional;
import static silas.yudi.applytrackmcp.domain.model.Fields.required;

/**
 * Etapa do processo: entrevista, teste, conversa. O estado diz onde ela está; as datas são opcionais.
 * ScheduledFor é quando foi marcada (ou o prazo de entrega do teste) e pode ser futura;
 * completedOn é quando aconteceu; feedbackExpectedBy é o retorno prometido.
 */
public record ProcessStep(String name,
                          StepState state,
                          LocalDate scheduledFor,
                          LocalDate completedOn,
                          LocalDate feedbackExpectedBy,
                          String notes) {

    public ProcessStep {
        name = required("step.name", name);
        state = required("step.state", state);
        notes = optional(notes);

        if (state == StepState.SCHEDULED && completedOn != null) {
            throw new IllegalArgumentException(
                    "Etapa marcada ainda não aconteceu: %s. Remova completedOn ou mude o estado".formatted(name));
        }
    }

    /** Campos não enviados ficam como estão; observação em branco remove a observação. */
    ProcessStep update(StepChange change) {
        if (change.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um campo da etapa para alterar");
        }

        return new ProcessStep(
                name,
                changed(change.state(), state),
                changed(change.scheduledFor(), scheduledFor),
                changed(change.completedOn(), completedOn),
                changed(change.feedbackExpectedBy(), feedbackExpectedBy),
                changed(change.notes(), notes)
        );
    }
}
