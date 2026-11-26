package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProcessStepTest {

    private static final LocalDate APPLIED = LocalDate.of(2026, 11, 20);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void stepRequiresName(String name) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ProcessStep(name, APPLIED, false, null))
                .withMessage("Campo obrigatório: step.name");
    }

    @Test
    void stepKeepsDataWithoutSurroundingSpaces() {
        assertThat(new ProcessStep(" Fit cultural ", APPLIED, true, " Conversa leve "))
                .isEqualTo(new ProcessStep("Fit cultural", APPLIED, true, "Conversa leve"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void stepDateAndNotesAreOptional(String notes) {
        var step = new ProcessStep("Entrevista com líder", null, false, notes);

        assertThat(step.date()).isNull();
        assertThat(step.notes()).isNull();
    }
}
