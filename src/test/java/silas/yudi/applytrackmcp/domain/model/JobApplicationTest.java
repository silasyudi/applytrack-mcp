package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static silas.yudi.applytrackmcp.support.JobPostingBuilder.aPosting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JobApplicationTest {

    private static final Requirement JAVA = new Requirement("Java", "Avançado", Importance.REQUIRED);
    private static final Requirement ENGLISH = new Requirement("Inglês", "C1", Importance.DESIRABLE);
    private static final Requirement NODE = new Requirement("Node.js", null, Importance.UNSPECIFIED);

    @Nested
    class Creation {

        @Test
        void keepsDataWithoutSurroundingSpaces() {
            var application = new JobApplication(
                    aPosting()
                            .company(" Acme ")
                            .jobTitle(" Java Developer ")
                            .link(" https://acme.com/jobs/1 ")
                            .description(" Full description ")
                            .build(),
                    ApplicationProcess.start(" LinkedIn ", LocalDate.of(2026, 11, 20)));

            assertThat(application.posting().company()).isEqualTo("Acme");
            assertThat(application.posting().jobTitle()).isEqualTo("Java Developer");
            assertThat(application.posting().description()).isEqualTo("Full description");
            assertThat(application.process().platform()).isEqualTo("LinkedIn");
            assertThat(application.posting().link()).isEqualTo("https://acme.com/jobs/1");
            assertThat(application.process().appliedOn()).isEqualTo(LocalDate.of(2026, 11, 20));
        }

        @Test
        void newApplicationsReceiveDifferentIds() {
            var first = acme();
            var second = acme();

            assertThat(UUID.fromString(first.id())).isNotEqualTo(UUID.fromString(second.id()));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void requiresId(String id) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new JobApplication(id,
                            aPosting().build(),
                            ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                    .withMessage("Campo obrigatório: id");
        }
    }

    @Nested
    class Update {

        @Test
        void updatesAllFieldsAndKeepsId() {
            var original = acme();

            var updated = original.update(JobApplicationChanges.builder()
                    .company("Beta")
                    .jobTitle("Analyst")
                    .link("https://beta.com")
                    .content(new ContentChange("New description", List.of()))
                    .platform("Gupy")
                    .appliedOn(LocalDate.of(2026, 11, 21))
                    .build());

            assertThat(updated).isEqualTo(new JobApplication(original.id(),
                    aPosting()
                            .company("Beta")
                            .jobTitle("Analyst")
                            .link("https://beta.com")
                            .description("New description")
                            .build(),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 21))));
        }

        @Test
        void keepsFieldsThatAreNotInTheChanges() {
            var original = acme();

            assertThat(original.update(JobApplicationChanges.builder().jobTitle("Analyst").build()))
                    .isEqualTo(new JobApplication(original.id(),
                            aPosting().jobTitle("Analyst").link("https://acme.com").build(),
                            ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void blankLinkRemovesTheLink(String link) {
            var original = acme();

            assertThat(original.update(JobApplicationChanges.builder().link(link).build()))
                    .isEqualTo(new JobApplication(original.id(),
                            aPosting().build(),
                            ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))));
        }

        @Test
        void addsALinkToAnApplicationWithoutLink() {
            var withoutLink = acme().update(JobApplicationChanges.builder().link("").build());

            var updated = withoutLink.update(JobApplicationChanges.builder().link("https://acme.com/2").build());

            assertThat(updated.posting().link()).isEqualTo("https://acme.com/2");
        }

        @Test
        void updateCannotEraseRequiredField() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> acme().update(JobApplicationChanges.builder().company(" ").build()))
                    .withMessage("Campo obrigatório: company");
        }

        @Test
        void updateRequiresAtLeastOneChange() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> acme().update(JobApplicationChanges.builder().build()))
                    .withMessage("Informe ao menos um campo para alterar");
        }
    }

    // contracted(): cliente final, senioridade, CLT e faixa salarial preenchidos
    @Nested
    class UpdatePostingFields {

        @Test
        void updatesPostingFieldsAndKeepsTheOthers() {
            var updated = acme().update(postingChange("Banco Alfa", "Sênior", "PJ", "R$ 15.000"));

            assertThat(updated.posting()).isEqualTo(aPosting()
                    .finalClient("Banco Alfa")
                    .seniority("Sênior")
                    .contractType("PJ")
                    .salaryRange("R$ 15.000")
                    .link("https://acme.com")
                    .build());
            assertThat(updated.process()).isEqualTo(acme().process());
        }

        @Test
        void updatingOnlyOnePostingFieldIsAValidChange() {
            assertThat(acme().update(postingChange(null, "Pleno", null, null)).posting().seniority())
                    .isEqualTo("Pleno");
        }

        @Test
        void omittedPostingFieldsKeepTheirValues() {
            var updated = contracted().update(JobApplicationChanges.builder().jobTitle("Analyst").build());

            assertThat(updated.posting().contractType()).isEqualTo("CLT");
            assertThat(updated.posting().salaryRange()).isEqualTo("R$ 8.000");
            assertThat(updated.posting().finalClient()).isEqualTo("Banco Alfa");
            assertThat(updated.posting().seniority()).isEqualTo("Pleno");
        }

        @Test
        void blankTextRemovesOptionalPostingFields() {
            var updated = contracted().update(postingChange("", " ", "", ""));

            assertThat(updated.posting().finalClient()).isNull();
            assertThat(updated.posting().seniority()).isNull();
            assertThat(updated.posting().contractType()).isNull();
            assertThat(updated.posting().salaryRange()).isNull();
        }

        @Test
        void addingSalaryRangeUsesTheExistingContractType() {
            var withContract = acme().update(postingChange(null, null, "CLT", null));

            assertThat(withContract.update(postingChange(null, null, null, "R$ 9.000")).posting().salaryRange())
                    .isEqualTo("R$ 9.000");
        }

        @Test
        void addingSalaryRangeWithoutAnyContractTypeIsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> acme().update(postingChange(null, null, null, "R$ 9.000")))
                    .withMessage("Para informar a faixa salarial, informe também o tipo de contrato");
        }

        @Test
        void removingContractTypeWhileSalaryRangeStaysIsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> contracted().update(postingChange(null, null, "", null)))
                    .withMessage("Para informar a faixa salarial, informe também o tipo de contrato");
        }
    }

    @Nested
    class UpdateContent {

        @Test
        void changingDescriptionTogetherWithRequirementsChangesBoth() {
            var updated = acme().update(contentChange("Kafka e Java", List.of(JAVA)));

            assertThat(updated.posting().description()).isEqualTo("Kafka e Java");
            assertThat(updated.posting().requirements()).containsExactly(JAVA);
        }

        @Test
        void changingRequirementsAloneKeepsTheDescription() {
            var updated = acme().update(contentChange(null, List.of(NODE)));

            assertThat(updated.posting().requirements()).containsExactly(NODE);
            assertThat(updated.posting().description()).isEqualTo("Description");
        }

        @Test
        void updateReplacesTheWholeRequirementList() {
            var original = withRequirements(JAVA, ENGLISH);

            var updated = original.update(requirementsChange(List.of(NODE)));

            assertThat(updated.posting().requirements()).containsExactly(NODE);
            assertThat(updated.posting().jobTitle()).isEqualTo("Java Developer");
        }

        @Test
        void updateWithEmptyListRemovesAllRequirements() {
            assertThat(withRequirements(JAVA, ENGLISH).update(requirementsChange(List.of())).posting().requirements()).isEmpty();
        }

        @Test
        void updateWithoutRequirementsKeepsThem() {
            var updated = withRequirements(JAVA, ENGLISH)
                    .update(JobApplicationChanges.builder().jobTitle("Analyst").build());

            assertThat(updated.posting().requirements()).containsExactly(JAVA, ENGLISH);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void blankDescriptionIsRejectedOnUpdate(String description) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> acme().update(contentChange(description, List.of())))
                    .withMessage("Campo obrigatório: description");
        }
    }

    // O status é calculado pelo processo; aqui só se garante que a candidatura entrega as mudanças a ele
    @Nested
    class UpdateProcess {

        @Test
        void updatesSituationNotesAndStepsKeepingIdAndPosting() {
            var original = acme();
            var step = new ProcessStep("Triagem", StepState.PASSED, null, LocalDate.of(2026, 11, 21), null, null);

            var updated = original.update(JobApplicationChanges.builder()
                    .situation(Situation.WITHDRAWN)
                    .notes("Salário abaixo do esperado")
                    .steps(List.of(step))
                    .build());

            assertThat(updated).isEqualTo(new JobApplication(original.id(), original.posting(),
                    new ApplicationProcess("LinkedIn", LocalDate.of(2026, 11, 20), Situation.WITHDRAWN,
                            "Salário abaixo do esperado", List.of(step))));
        }

        @Test
        void postingChangesKeepTheProcess() {
            var updated = acme().update(JobApplicationChanges.builder().jobTitle("Analyst").build());

            assertThat(updated.process()).isEqualTo(acme().process());
        }
    }

    @Nested
    class Steps {

        private final ProcessStep interview = new ProcessStep("Entrevista técnica", StepState.SCHEDULED,
                LocalDate.of(2026, 12, 3), null, null, null);

        @Test
        void addsAStepKeepingIdAndPosting() {
            var original = acme();

            var updated = original.addStep(interview);

            assertThat(updated.id()).isEqualTo(original.id());
            assertThat(updated.posting()).isEqualTo(original.posting());
            assertThat(updated.process().steps()).containsExactly(interview);
        }

        @Test
        void updatesAStepKeepingIdAndPosting() {
            var original = acme().addStep(interview);

            var updated = original.updateStep("entrevista tecnica",
                    new StepChange(StepState.AWAITING_FEEDBACK, null, LocalDate.of(2026, 11, 25), null, null));

            assertThat(updated.id()).isEqualTo(original.id());
            assertThat(updated.posting()).isEqualTo(original.posting());
            assertThat(updated.process().steps()).containsExactly(new ProcessStep("Entrevista técnica",
                    StepState.AWAITING_FEEDBACK, LocalDate.of(2026, 12, 3), LocalDate.of(2026, 11, 25), null, null));
        }
    }

    private static JobApplication withRequirements(Requirement... requirements) {
        return new JobApplication(
                aPosting().requirements(List.of(requirements)).build(),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20)));
    }

    private static JobApplicationChanges requirementsChange(List<Requirement> requirements) {
        return JobApplicationChanges.builder().content(new ContentChange(null, requirements)).build();
    }

    private static JobApplicationChanges contentChange(String description, List<Requirement> requirements) {
        return JobApplicationChanges.builder().content(new ContentChange(description, requirements)).build();
    }

    private static JobApplication contracted() {
        return acme().update(postingChange("Banco Alfa", "Pleno", "CLT", "R$ 8.000"));
    }

    private static JobApplicationChanges postingChange(String finalClient, String seniority, String contractType,
                                                       String salaryRange) {
        return JobApplicationChanges.builder()
                .finalClient(finalClient)
                .seniority(seniority)
                .contractType(contractType)
                .salaryRange(salaryRange)
                .build();
    }

    private static JobApplication acme() {
        return new JobApplication(
                aPosting().link("https://acme.com").build(),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20)));
    }
}
