package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ApplicationProcessTest {

    private static final LocalDate APPLIED = LocalDate.of(2026, 11, 20);
    private static final ProcessStep APPLICATION = new ProcessStep("Candidatura", APPLIED, true, null);
    private static final ProcessStep INTERVIEW = new ProcessStep("Entrevista técnica", LocalDate.of(2026, 11, 24), true,
            "Perguntaram sobre Kafka");

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresPlatform(String platform) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, null, "Description", List.of()),
                ApplicationProcess.start(platform, LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: platform");
    }

    // Sem data não dá para calcular prazos
    @Test
    void requiresAppliedOn() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, null, "Description", List.of()),
                ApplicationProcess.start("LinkedIn", null)))
                .withMessage("Campo obrigatório: appliedOn");
    }

    @Test
    void startUsesDefaultsForMissingValues() {
        assertThat(ApplicationProcess.start("Gupy", APPLIED, null, null, null, null)).isEqualTo(
                new ApplicationProcess("Gupy", APPLIED, "Candidatura enviada", APPLIED, Situation.ACTIVE, List.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void startUsesDefaultStatusWhenBlank(String status) {
        assertThat(ApplicationProcess.start("Gupy", APPLIED, status, null, null, null).status())
                .isEqualTo("Candidatura enviada");
    }

    @Test
    void startKeepsGivenValuesWithoutSurroundingSpaces() {
        var process = ApplicationProcess.start("Gupy", APPLIED, " Entrevista técnica realizada, aguardando feedback ",
                LocalDate.of(2026, 11, 24), Situation.ACTIVE, List.of(APPLICATION, INTERVIEW));

        assertThat(process).isEqualTo(new ApplicationProcess("Gupy", APPLIED,
                "Entrevista técnica realizada, aguardando feedback", LocalDate.of(2026, 11, 24), Situation.ACTIVE,
                List.of(APPLICATION, INTERVIEW)));
    }

    @Test
    void startRejectsStatusDateWithoutStatus() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ApplicationProcess.start("Gupy", APPLIED, null, LocalDate.of(2026, 11, 22), null, null))
                .withMessage("Para informar a data do status, envie também o status");
    }

    @Test
    void startRejectsSituationWithoutStatus() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ApplicationProcess.start("Gupy", APPLIED, null, null, Situation.WITHDRAWN, null))
                .withMessage("Para informar a situação, envie também o status");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresStatus(String status) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ApplicationProcess("Gupy", APPLIED, status, APPLIED, Situation.ACTIVE, List.of()))
                .withMessage("Campo obrigatório: status");
    }

    @Test
    void requiresStatusDate() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ApplicationProcess("Gupy", APPLIED, "Enviada", null, Situation.ACTIVE, List.of()))
                .withMessage("Campo obrigatório: statusUpdatedOn");
    }

    @Test
    void requiresSituation() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ApplicationProcess("Gupy", APPLIED, "Enviada", APPLIED, null, List.of()))
                .withMessage("Campo obrigatório: situation");
    }

    @Test
    void rejectsStatusDateBeforeApplication() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ApplicationProcess("Gupy", APPLIED, "Enviada", LocalDate.of(2026, 11, 19),
                        Situation.ACTIVE, List.of()))
                .withMessage("A data do status não pode ser anterior à candidatura: 2026-11-19");
    }

    @Test
    void stepsAreOptional() {
        assertThat(new ApplicationProcess("Gupy", APPLIED, "Enviada", APPLIED, Situation.ACTIVE, null).steps())
                .isEmpty();
    }

    @Test
    void keepsStepsInOrderAndProtectedFromOutsideChanges() {
        var steps = new ArrayList<>(List.of(APPLICATION, INTERVIEW));
        var process = new ApplicationProcess("Gupy", APPLIED, "Enviada", APPLIED, Situation.ACTIVE, steps);

        steps.clear();

        assertThat(process.steps()).containsExactly(APPLICATION, INTERVIEW);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> process.steps().add(APPLICATION));
    }

    @Test
    void rejectsNullStep() {
        var steps = new ArrayList<ProcessStep>();
        steps.add(null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ApplicationProcess("Gupy", APPLIED, "Enviada", APPLIED, Situation.ACTIVE, steps))
                .withMessage("Campo obrigatório: step");
    }

    @Test
    void acceptsStepOnTheApplicationDayAndStepWithoutDate() {
        var steps = List.of(new ProcessStep("Candidatura", APPLIED, true, null),
                new ProcessStep("Entrevista técnica", null, false, null));

        assertThat(new ApplicationProcess("Gupy", APPLIED, "Enviada", APPLIED, Situation.ACTIVE, steps).steps())
                .isEqualTo(steps);
    }

    @Test
    void rejectsStepBeforeApplication() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ApplicationProcess("Gupy", APPLIED, "Enviada", APPLIED, Situation.ACTIVE,
                        List.of(new ProcessStep("Entrevista técnica", LocalDate.of(2026, 11, 19), true, null))))
                .withMessage("A etapa não pode ser anterior à candidatura: Entrevista técnica (2026-11-19)");
    }
}
