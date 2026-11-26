package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ApplicationProcessTest {

    private static final LocalDate APPLIED = LocalDate.of(2026, 11, 20);

    private static final ProcessStep SCREENING_PASSED = step("Triagem", StepState.PASSED, null,
            LocalDate.of(2026, 11, 21), null);
    private static final ProcessStep INTERVIEW_AWAITING = step("Entrevista técnica", StepState.AWAITING_FEEDBACK,
            LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 24), null);
    private static final ProcessStep TEST_SCHEDULED = step("Teste técnico", StepState.SCHEDULED,
            LocalDate.of(2026, 12, 3), null, null);

    @Nested
    class Creation {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void requiresPlatform(String platform) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ApplicationProcess.start(platform, APPLIED))
                    .withMessage("Campo obrigatório: platform");
        }

        // Sem data não dá para calcular prazos
        @Test
        void requiresAppliedOn() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ApplicationProcess.start("Gupy", null))
                    .withMessage("Campo obrigatório: appliedOn");
        }

        @Test
        void requiresSituation() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ApplicationProcess("Gupy", APPLIED, null, null, List.of()))
                    .withMessage("Campo obrigatório: situation");
        }

        @Test
        void startUsesDefaultsForMissingValues() {
            assertThat(ApplicationProcess.start("Gupy", APPLIED, null, null, null))
                    .isEqualTo(new ApplicationProcess("Gupy", APPLIED, Situation.ACTIVE, null, List.of()));
        }

        @Test
        void keepsGivenValuesWithoutSurroundingSpaces() {
            assertThat(ApplicationProcess.start(" Gupy ", APPLIED, Situation.OFFER, " Indicação do João ",
                    List.of(SCREENING_PASSED)))
                    .isEqualTo(new ApplicationProcess("Gupy", APPLIED, Situation.OFFER, "Indicação do João",
                            List.of(SCREENING_PASSED)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void blankNotesBecomeNull(String notes) {
            assertThat(ApplicationProcess.start("Gupy", APPLIED, null, notes, null).notes()).isNull();
        }

        @Test
        void keepsStepsInOrderAndProtectedFromOutsideChanges() {
            var steps = new ArrayList<>(List.of(SCREENING_PASSED, INTERVIEW_AWAITING));
            var process = ApplicationProcess.start("Gupy", APPLIED, null, null, steps);

            steps.clear();

            assertThat(process.steps()).containsExactly(SCREENING_PASSED, INTERVIEW_AWAITING);
            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(() -> process.steps().add(TEST_SCHEDULED));
        }

        @Test
        void rejectsNullStep() {
            var steps = new ArrayList<ProcessStep>();
            steps.add(null);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ApplicationProcess.start("Gupy", APPLIED, null, null, steps))
                    .withMessage("Campo obrigatório: step");
        }

        // O nome identifica a etapa nas tools de etapa
        @Test
        void rejectsRepeatedStepNamesIgnoringCaseAndAccents() {
            var repeated = step(" ENTREVISTA TECNICA ", StepState.SCHEDULED, null, null, null);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ApplicationProcess.start("Gupy", APPLIED, null, null,
                            List.of(INTERVIEW_AWAITING, repeated)))
                    .withMessage("Etapa repetida: ENTREVISTA TECNICA");
        }

        @ParameterizedTest
        @ValueSource(strings = {"scheduledFor", "completedOn", "feedbackExpectedBy"})
        void rejectsStepDateBeforeApplication(String field) {
            var early = LocalDate.of(2026, 11, 19);
            var step = switch (field) {
                case "scheduledFor" -> step("Triagem", StepState.SCHEDULED, early, null, null);
                case "completedOn" -> step("Triagem", StepState.PASSED, null, early, null);
                default -> step("Triagem", StepState.AWAITING_FEEDBACK, null, null, early);
            };

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ApplicationProcess.start("Gupy", APPLIED, null, null, List.of(step)))
                    .withMessage("A etapa não pode ser anterior à candidatura: Triagem (2026-11-19)");
        }

        @Test
        void acceptsStepOnTheApplicationDay() {
            var sameDay = step("Triagem", StepState.PASSED, APPLIED, APPLIED, APPLIED);

            assertThat(ApplicationProcess.start("Gupy", APPLIED, null, null, List.of(sameDay)).steps())
                    .containsExactly(sameDay);
        }
    }

    // Status, quem precisa agir e última atividade são calculados, nunca informados
    @Nested
    class Status {

        @Test
        void withoutStepsWaitsForTheCompanyToGetInTouch() {
            var process = withSteps();

            assertThat(process.status()).isEqualTo("Aguardando contato após a candidatura");
            assertThat(process.waitingFor()).isEqualTo(WaitingFor.COMPANY);
            assertThat(process.currentStep()).isEmpty();
        }

        @Test
        void scheduledStepWaitsForTheUser() {
            var process = withSteps(SCREENING_PASSED, TEST_SCHEDULED);

            assertThat(process.status()).isEqualTo("Próxima etapa: Teste técnico em 2026-12-03");
            assertThat(process.waitingFor()).isEqualTo(WaitingFor.ME);
            assertThat(process.currentStep()).contains(TEST_SCHEDULED);
        }

        @Test
        void scheduledStepWithoutDate() {
            var withoutDate = step("Entrevista com líder", StepState.SCHEDULED, null, null, null);

            assertThat(withSteps(withoutDate).status()).isEqualTo("Próxima etapa: Entrevista com líder");
        }

        @Test
        void stepAwaitingFeedbackWaitsForTheCompany() {
            var process = withSteps(SCREENING_PASSED, INTERVIEW_AWAITING);

            assertThat(process.status()).isEqualTo("Aguardando feedback de Entrevista técnica");
            assertThat(process.waitingFor()).isEqualTo(WaitingFor.COMPANY);
            assertThat(process.currentStep()).contains(INTERVIEW_AWAITING);
        }

        @Test
        void showsThePromisedFeedbackDate() {
            var promised = step("Entrevista técnica", StepState.AWAITING_FEEDBACK, null, LocalDate.of(2026, 11, 24),
                    LocalDate.of(2026, 12, 1));

            assertThat(withSteps(promised).status())
                    .isEqualTo("Aguardando feedback de Entrevista técnica (prometido até 2026-12-01)");
        }

        // Algo a fazer pesa mais que uma resposta a esperar
        @Test
        void scheduledStepWinsOverStepAwaitingFeedback() {
            var process = withSteps(INTERVIEW_AWAITING, TEST_SCHEDULED);

            assertThat(process.waitingFor()).isEqualTo(WaitingFor.ME);
            assertThat(process.currentStep()).contains(TEST_SCHEDULED);
        }

        // A mais próxima de acontecer: as etapas são listadas em ordem
        @Test
        void firstScheduledStepIsTheCurrentOne() {
            var later = step("Entrevista com líder", StepState.SCHEDULED, LocalDate.of(2026, 12, 9), null, null);

            assertThat(withSteps(TEST_SCHEDULED, later).currentStep()).contains(TEST_SCHEDULED);
        }

        // O retorno mais recente a esperar
        @Test
        void lastStepAwaitingFeedbackIsTheCurrentOne() {
            var later = step("Fit cultural", StepState.AWAITING_FEEDBACK, null, LocalDate.of(2026, 11, 25), null);

            assertThat(withSteps(INTERVIEW_AWAITING, later).currentStep()).contains(later);
        }

        @Test
        void afterAPassedStepWaitsForContact() {
            var process = withSteps(SCREENING_PASSED);

            assertThat(process.status()).isEqualTo("Aguardando contato após aprovação em Triagem");
            assertThat(process.waitingFor()).isEqualTo(WaitingFor.COMPANY);
        }

        @Test
        void afterACancelledStepWaitsForContact() {
            var cancelled = step("Entrevista técnica", StepState.CANCELLED, LocalDate.of(2026, 11, 25), null, null);

            assertThat(withSteps(SCREENING_PASSED, cancelled).status())
                    .isEqualTo("Aguardando contato após cancelamento de Entrevista técnica");
        }

        // A situação é manual: a etapa reprovada não encerra o processo sozinha
        @Test
        void failedStepKeepsTheProcessActive() {
            var failed = step("Entrevista técnica", StepState.FAILED, null, LocalDate.of(2026, 11, 24), null);
            var process = withSteps(SCREENING_PASSED, failed);

            assertThat(process.situation()).isEqualTo(Situation.ACTIVE);
            assertThat(process.status()).isEqualTo("Reprovação em Entrevista técnica");
            assertThat(process.waitingFor()).isEqualTo(WaitingFor.COMPANY);
        }

        @ParameterizedTest
        @CsvSource({
                "OFFER, Proposta recebida, ME",
                "HIRED, Contratado, NOTHING",
                "REJECTED, Reprovado, NOTHING",
                "WITHDRAWN, Desistência, NOTHING",
                "CLOSED, Vaga fechada, NOTHING"
        })
        void situationOtherThanActiveDefinesTheStatus(Situation situation, String status, WaitingFor waitingFor) {
            var process = ApplicationProcess.start("Gupy", APPLIED, situation, null, List.of(TEST_SCHEDULED));

            assertThat(process.status()).isEqualTo(status);
            assertThat(process.waitingFor()).isEqualTo(waitingFor);
        }

        @Test
        void lastActivityIsTheApplicationWhenNothingHappenedYet() {
            assertThat(withSteps(TEST_SCHEDULED).lastActivityOn()).isEqualTo(APPLIED);
        }

        // Só conta o que aconteceu: datas marcadas e retornos prometidos ficam de fora
        @Test
        void lastActivityIsTheLatestCompletion() {
            var promised = step("Fit cultural", StepState.AWAITING_FEEDBACK, LocalDate.of(2026, 12, 9),
                    LocalDate.of(2026, 11, 22), LocalDate.of(2026, 12, 10));

            assertThat(withSteps(INTERVIEW_AWAITING, promised, TEST_SCHEDULED).lastActivityOn())
                    .isEqualTo(LocalDate.of(2026, 11, 24));
        }
    }

    @Nested
    class Update {

        private final ApplicationProcess original =
                ApplicationProcess.start("Gupy", APPLIED, null, "Indicação", List.of(SCREENING_PASSED));

        @Test
        void changesOnlyTheGivenFields() {
            var updated = original.update(JobApplicationChanges.builder()
                    .platform("LinkedIn")
                    .appliedOn(LocalDate.of(2026, 11, 19))
                    .situation(Situation.WITHDRAWN)
                    .build());

            assertThat(updated).isEqualTo(new ApplicationProcess("LinkedIn", LocalDate.of(2026, 11, 19),
                    Situation.WITHDRAWN, "Indicação", List.of(SCREENING_PASSED)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void blankNotesRemoveTheNotes(String notes) {
            assertThat(original.update(JobApplicationChanges.builder().notes(notes).build()).notes()).isNull();
        }

        @Test
        void replacesTheWholeStepList() {
            var updated = original.update(JobApplicationChanges.builder().steps(List.of(TEST_SCHEDULED)).build());

            assertThat(updated.steps()).containsExactly(TEST_SCHEDULED);
        }

        @Test
        void emptyStepListRemovesAllSteps() {
            assertThat(original.update(JobApplicationChanges.builder().steps(List.of()).build()).steps()).isEmpty();
        }

        @Test
        void movingTheApplicationAfterAStepIsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> original.update(JobApplicationChanges.builder()
                            .appliedOn(LocalDate.of(2026, 11, 22))
                            .build()))
                    .withMessage("A etapa não pode ser anterior à candidatura: Triagem (2026-11-21)");
        }
    }

    @Nested
    class Steps {

        private final ApplicationProcess original = withSteps(SCREENING_PASSED, INTERVIEW_AWAITING);

        @Test
        void addsAStepAtTheEnd() {
            assertThat(original.addStep(TEST_SCHEDULED).steps())
                    .containsExactly(SCREENING_PASSED, INTERVIEW_AWAITING, TEST_SCHEDULED);
        }

        @Test
        void rejectsAStepWithARepeatedName() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> original.addStep(step("triagem", StepState.SCHEDULED, null, null, null)))
                    .withMessage("Etapa repetida: triagem");
        }

        @Test
        void updatesOnlyTheStepWithTheNameIgnoringCaseAndAccents() {
            var updated = original.updateStep(" ENTREVISTA TECNICA ",
                    new StepChange(StepState.PASSED, null, null, null, "Gostaram do Kafka"));

            assertThat(updated.steps()).containsExactly(SCREENING_PASSED, new ProcessStep("Entrevista técnica",
                    StepState.PASSED, LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 24), null,
                    "Gostaram do Kafka"));
        }

        @Test
        void rejectsAnUnknownStep() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> original.updateStep("Teste técnico",
                            new StepChange(StepState.PASSED, null, null, null, null)))
                    .withMessage("Etapa não encontrada: Teste técnico");
        }

        @Test
        void rejectsAnUpdateThatPutsTheStepBeforeTheApplication() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> original.updateStep("Triagem",
                            new StepChange(null, null, LocalDate.of(2026, 11, 19), null, null)))
                    .withMessage("A etapa não pode ser anterior à candidatura: Triagem (2026-11-19)");
        }
    }

    private static ApplicationProcess withSteps(ProcessStep... steps) {
        return ApplicationProcess.start("Gupy", APPLIED, null, null, List.of(steps));
    }

    private static ProcessStep step(String name, StepState state, LocalDate scheduledFor, LocalDate completedOn,
                                    LocalDate feedbackExpectedBy) {
        return new ProcessStep(name, state, scheduledFor, completedOn, feedbackExpectedBy, null);
    }
}
