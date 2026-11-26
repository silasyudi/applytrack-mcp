package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** Campos que só fazem sentido juntos chegam agrupados; aqui ficam as regras de cada grupo. */
class ChangeGroupsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 22);
    private static final List<Requirement> JAVA = List.of(new Requirement("Java", null, Importance.REQUIRED));

    // ---------- status + data + situação

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

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankStatusIsRejectedEvenAlone(String status) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> StatusChange.of(status, null, null))
                .withMessage("Campo obrigatório: status");
    }

    // ---------- descrição + requisitos

    @Test
    void nothingAboutContentMeansNoContentChange() {
        assertThat(ContentChange.of(null, null)).isNull();
    }

    @Test
    void requirementsAloneAreAValidChange() {
        assertThat(ContentChange.of(null, JAVA)).isEqualTo(new ContentChange(null, JAVA));
    }

    @Test
    void descriptionWithEmptyRequirementsIsAValidChange() {
        assertThat(ContentChange.of("Nova descrição", List.of())).isEqualTo(new ContentChange("Nova descrição", List.of()));
    }

    @Test
    void descriptionWithoutRequirementsIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ContentChange.of("Nova descrição", null))
                .withMessage("Para informar a descrição, envie também os requisitos (lista vazia se a vaga não citar nenhum)");
    }
}
