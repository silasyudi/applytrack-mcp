package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProcessStepTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);

    @Nested
    class Creation {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void requiresName(String name) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ProcessStep(name, StepState.SCHEDULED, null, null, null, null))
                    .withMessage("Campo obrigatório: step.name");
        }

        @Test
        void requiresState() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ProcessStep("Entrevista técnica", null, null, null, null, null))
                    .withMessage("Campo obrigatório: step.state");
        }

        @Test
        void keepsDataWithoutSurroundingSpaces() {
            assertThat(new ProcessStep(" Fit cultural ", StepState.PASSED, null, DAY, null, " Conversa leve "))
                    .isEqualTo(new ProcessStep("Fit cultural", StepState.PASSED, null, DAY, null, "Conversa leve"));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void datesAndNotesAreOptional(String notes) {
            var step = new ProcessStep("Entrevista com líder", StepState.SCHEDULED, null, null, null, notes);

            assertThat(step.scheduledFor()).isNull();
            assertThat(step.completedOn()).isNull();
            assertThat(step.feedbackExpectedBy()).isNull();
            assertThat(step.notes()).isNull();
        }

        // Marcada e ainda por acontecer: uma data de conclusão contradiz o estado
        @Test
        void scheduledStepCannotHaveACompletionDate() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ProcessStep("Entrevista técnica", StepState.SCHEDULED, DAY, DAY, null, null))
                    .withMessage("Etapa marcada ainda não aconteceu: Entrevista técnica. Remova completedOn ou mude "
                            + "o estado");
        }
    }

    @Nested
    class Update {

        private final ProcessStep scheduled = new ProcessStep("Entrevista técnica", StepState.SCHEDULED, DAY, null,
                null, "Com o tech lead");

        @Test
        void changesOnlyTheGivenFields() {
            var updated = scheduled.update(new StepChange(StepState.AWAITING_FEEDBACK, null, LocalDate.of(2026, 11, 25),
                    LocalDate.of(2026, 12, 1), null));

            assertThat(updated).isEqualTo(new ProcessStep("Entrevista técnica", StepState.AWAITING_FEEDBACK, DAY,
                    LocalDate.of(2026, 11, 25), LocalDate.of(2026, 12, 1), "Com o tech lead"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void blankNotesRemoveTheNotes(String notes) {
            assertThat(scheduled.update(new StepChange(null, null, null, null, notes)).notes()).isNull();
        }

        @Test
        void requiresAtLeastOneChange() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> scheduled.update(new StepChange(null, null, null, null, null)))
                    .withMessage("Informe ao menos um campo da etapa para alterar");
        }

        @Test
        void cannotMoveBackToScheduledKeepingTheCompletionDate() {
            var done = scheduled.update(new StepChange(StepState.AWAITING_FEEDBACK, null, DAY, null, null));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> done.update(new StepChange(StepState.SCHEDULED, null, null, null, null)))
                    .withMessage("Etapa marcada ainda não aconteceu: Entrevista técnica. Remova completedOn ou mude "
                            + "o estado");
        }
    }
}
