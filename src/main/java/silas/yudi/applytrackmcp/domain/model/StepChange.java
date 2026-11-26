package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

/** Campos de uma etapa a alterar. Nulo mantém o valor atual; observação em branco remove a observação. */
public record StepChange(StepState state,
                         LocalDate scheduledFor,
                         LocalDate completedOn,
                         LocalDate feedbackExpectedBy,
                         String notes) {

    boolean isEmpty() {
        return Stream.of(state, scheduledFor, completedOn, feedbackExpectedBy, notes).allMatch(Objects::isNull);
    }
}
