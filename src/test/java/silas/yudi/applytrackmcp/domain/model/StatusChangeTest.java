package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class StatusChangeTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 22);

    @Test
    void nothingAboutStatusMeansNoStatusChange() {
        assertThat(StatusChange.of(null, null, null)).isNull();
    }

    @Test
    void statusAloneIsAValidChange() {
        assertThat(StatusChange.of("Entrevista marcada", null, null))
                .isEqualTo(new StatusChange("Entrevista marcada", null, null));
    }

    @Test
    void statusWithDateAndSituationIsAValidChange() {
        assertThat(StatusChange.of(" Reprovado ", DAY, Situation.REJECTED))
                .isEqualTo(new StatusChange("Reprovado", DAY, Situation.REJECTED));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankStatusIsRejectedEvenAlone(String status) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> StatusChange.of(status, null, null))
                .withMessage("Campo obrigatório: status");
    }

    @Test
    void statusDateWithoutStatusIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> StatusChange.of(null, DAY, null))
                .withMessage("Para informar a data do status, envie também o status");
    }

    @Test
    void situationWithoutStatusIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> StatusChange.of(null, null, Situation.REJECTED))
                .withMessage("Para informar a situação, envie também o status");
    }

    @Test
    void dateAndSituationWithoutStatusAreRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> StatusChange.of(null, DAY, Situation.REJECTED))
                .withMessage("Para informar a data do status, envie também o status");
    }
}
