package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JobApplicationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    @Test
    void keepsDataWithoutSurroundingSpaces() {
        var application = new JobApplication(
                new JobPosting(" Acme ", null, " Java Developer ", null, null, null, " Full description ", " https://acme.com/jobs/1 ", List.of()),
                ApplicationProcess.start(" LinkedIn ", LocalDate.of(2026, 11, 20)));

        assertThat(application.posting().company()).isEqualTo("Acme");
        assertThat(application.posting().jobTitle()).isEqualTo("Java Developer");
        assertThat(application.posting().description()).isEqualTo("Full description");
        assertThat(application.process().platform()).isEqualTo("LinkedIn");
        assertThat(application.posting().link()).isEqualTo("https://acme.com/jobs/1");
        assertThat(application.process().appliedOn()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresCompany(String company) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                new JobPosting(company, null, "Java Developer", null, null, null, "Description", null, List.of()),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: company");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresJobTitle(String jobTitle) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                new JobPosting("Acme", null, jobTitle, null, null, null, "Description", null, List.of()),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: jobTitle");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresDescription(String description) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, description, null, List.of()),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: description");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresPlatform(String platform) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", null, List.of()),
                ApplicationProcess.start(platform, LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: platform");
    }

    // Há processos que só chegam por e-mail ou WhatsApp
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void linkIsOptional(String link) {
        assertThat(new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", link, List.of()),
                ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 20))).posting().link()).isNull();
    }

    // Sem data não dá para calcular prazos
    @Test
    void requiresAppliedOn() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", null, List.of()),
                ApplicationProcess.start("LinkedIn", null)))
                .withMessage("Campo obrigatório: appliedOn");
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
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", null, List.of()),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: id");
    }

    @Test
    void updatesAllFieldsAndKeepsId() {
        var original = acme();

        var updated = original.update(new JobApplicationChanges("Beta", null, "Analyst", null, null, null, "Gupy", "https://beta.com", LocalDate.of(2026, 11, 21), new ContentChange("New description", List.of()), null, null), TODAY);

        assertThat(updated).isEqualTo(new JobApplication(original.id(),
                new JobPosting("Beta", null, "Analyst", null, null, null, "New description", "https://beta.com", List.of()),
                ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 21))));
    }

    @Test
    void keepsFieldsThatAreNotInTheChanges() {
        var original = acme();

        assertThat(original.update(new JobApplicationChanges(null, null, "Analyst", null, null, null, null, null, null, null, null, null), TODAY))
                .isEqualTo(new JobApplication(original.id(),
                new JobPosting("Acme", null, "Analyst", null, null, null, "Description", "https://acme.com", List.of()),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankLinkRemovesTheLink(String link) {
        var original = acme();

        assertThat(original.update(new JobApplicationChanges(null, null, null, null, null, null, null, link, null, null, null, null), TODAY))
                .isEqualTo(new JobApplication(original.id(),
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", null, List.of()),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))));
    }

    @Test
    void updateCannotEraseRequiredField() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> acme().update(new JobApplicationChanges(" ", null, null, null, null, null, null, null, null, null, null, null), TODAY))
                .withMessage("Campo obrigatório: company");
    }

    @Test
    void updateRequiresAtLeastOneChange() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> acme().update(new JobApplicationChanges(null, null, null, null, null, null, null, null, null, null, null, null), TODAY))
                .withMessage("Informe ao menos um campo para alterar");
    }

    // ---------- requisitos

    @Test
    void requirementKeepsDataWithoutSurroundingSpaces() {
        assertThat(new Requirement(" Spring Boot ", " Avançado ", Importance.REQUIRED))
                .isEqualTo(new Requirement("Spring Boot", "Avançado", Importance.REQUIRED));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requirementRequiresName(String name) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Requirement(name, "Avançado", Importance.REQUIRED))
                .withMessage("Campo obrigatório: requirement.name");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requirementLevelIsOptional(String level) {
        assertThat(new Requirement("Java", level, Importance.REQUIRED).level()).isNull();
    }

    @Test
    void requirementWithoutImportanceIsUnspecified() {
        assertThat(new Requirement("Java", null, null).importance()).isEqualTo(Importance.UNSPECIFIED);
    }

    @Test
    void requirementsAreOptional() {
        var application = new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", null, null),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20)));

        assertThat(application.posting().requirements()).isEmpty();
    }

    @Test
    void keepsRequirementsInOrderAndProtectedFromOutsideChanges() {
        var requirements = new ArrayList<>(List.of(JAVA, ENGLISH));
        var application = new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", null, requirements),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20)));

        requirements.add(NODE);

        assertThat(application.posting().requirements()).containsExactly(JAVA, ENGLISH);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> application.posting().requirements().add(NODE));
    }

    @Test
    void rejectsNullRequirement() {
        var requirements = new ArrayList<Requirement>();
        requirements.add(JAVA);
        requirements.add(null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", null, requirements),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20))))
                .withMessage("Campo obrigatório: requirement");
    }

    @Test
    void updateReplacesTheWholeRequirementList() {
        var original = withRequirements(JAVA, ENGLISH);

        var updated = original.update(requirementsChange(List.of(NODE)), TODAY);

        assertThat(updated.posting().requirements()).containsExactly(NODE);
        assertThat(updated.posting().jobTitle()).isEqualTo("Java Developer");
    }

    @Test
    void updateWithEmptyListRemovesAllRequirements() {
        assertThat(withRequirements(JAVA, ENGLISH).update(requirementsChange(List.of()), TODAY).posting().requirements()).isEmpty();
    }

    @Test
    void updateWithoutRequirementsKeepsThem() {
        var updated = withRequirements(JAVA, ENGLISH)
                .update(new JobApplicationChanges(null, null, "Analyst", null, null, null, null, null, null, null, null, null), TODAY);

        assertThat(updated.posting().requirements()).containsExactly(JAVA, ENGLISH);
    }

    private static final Requirement JAVA = new Requirement("Java", "Avançado", Importance.REQUIRED);
    private static final Requirement ENGLISH = new Requirement("Inglês", "C1", Importance.DESIRABLE);
    private static final Requirement NODE = new Requirement("Node.js", null, Importance.UNSPECIFIED);

    private static JobApplication withRequirements(Requirement... requirements) {
        return new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", null, List.of(requirements)),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20)));
    }

    private static JobApplicationChanges requirementsChange(List<Requirement> requirements) {
        return new JobApplicationChanges(null, null, null, null, null, null, null, null, null, new ContentChange(null, requirements), null, null);
    }

    // ---------- processo (acme: candidatura em 2026-11-20, status padrão; hoje é 2026-11-25)

    @Test
    void changingStatusMovesStatusDateToToday() {
        var updated = acme().update(processChange(" Entrevista marcada ", null, null, null), TODAY);

        assertThat(updated.process().status()).isEqualTo("Entrevista marcada");
        assertThat(updated.process().statusUpdatedOn()).isEqualTo(TODAY);
    }

    @Test
    void changingSituationAloneIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> processChange(null, null, Situation.REJECTED, null))
                .withMessage("Para informar a situação, envie também o status");
    }

    @Test
    void sendingTheSameStatusAndSituationKeepsStatusDate() {
        var updated = acme().update(processChange("Candidatura enviada", null, Situation.ACTIVE, null), TODAY);

        assertThat(updated.process().statusUpdatedOn()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void givenStatusDateWinsOverToday() {
        var updated = acme().update(processChange("Entrevista marcada", LocalDate.of(2026, 11, 22), null, null), TODAY);

        assertThat(updated.process().statusUpdatedOn()).isEqualTo(LocalDate.of(2026, 11, 22));
    }

    @Test
    void otherChangesKeepStatusAndStatusDate() {
        var updated = acme().update(new JobApplicationChanges(null, null, "Analyst", null, null, null, null, null, null, null, null, null), TODAY);

        assertThat(updated.process()).isEqualTo(acme().process());
    }

    // Corrigir a data da candidatura enquanto o status nunca mudou
    @Test
    void changingAppliedOnMovesStatusDateWhenStatusNeverChanged() {
        var updated = acme().update(new JobApplicationChanges(null, null, null, null, null, null, null, null, LocalDate.of(2026, 11, 18), null, null, null), TODAY);

        assertThat(updated.process().statusUpdatedOn()).isEqualTo(LocalDate.of(2026, 11, 18));
    }

    @Test
    void changingAppliedOnKeepsStatusDateOfAChangedStatus() {
        var interviewed = acme().update(processChange("Entrevista feita", LocalDate.of(2026, 11, 24), null, null), TODAY);

        var updated = interviewed.update(new JobApplicationChanges(null, null, null, null, null, null, null, null, LocalDate.of(2026, 11, 18), null, null, null), TODAY);

        assertThat(updated.process().statusUpdatedOn()).isEqualTo(LocalDate.of(2026, 11, 24));
    }

    @Test
    void updateReplacesTheWholeStepList() {
        var withSteps = acme().update(processChange(null, null, null, List.of(STEP_A, STEP_B)), TODAY);

        var updated = withSteps.update(processChange(null, null, null, List.of(STEP_B)), TODAY);

        assertThat(updated.process().steps()).containsExactly(STEP_B);
        assertThat(updated.process().status()).isEqualTo("Candidatura enviada");
    }

    @Test
    void updateWithEmptyStepsRemovesThem() {
        var withSteps = acme().update(processChange(null, null, null, List.of(STEP_A)), TODAY);

        assertThat(withSteps.update(processChange(null, null, null, List.of()), TODAY).process().steps()).isEmpty();
    }

    @Test
    void updateWithoutStepsKeepsThem() {
        var withSteps = acme().update(processChange(null, null, null, List.of(STEP_A, STEP_B)), TODAY);

        var updated = withSteps.update(processChange("Aguardando", null, null, null), TODAY);

        assertThat(updated.process().steps()).containsExactly(STEP_A, STEP_B);
    }

    // ---------- pares no update (acme: candidatura e status em 2026-11-20)

    @Test
    void resendingTheSameStatusWithADateCorrectsTheStatusDate() {
        var updated = acme().update(statusChange("Candidatura enviada", LocalDate.of(2026, 11, 22), null), TODAY);

        assertThat(updated.process().status()).isEqualTo("Candidatura enviada");
        assertThat(updated.process().statusUpdatedOn()).isEqualTo(LocalDate.of(2026, 11, 22));
    }

    @Test
    void changingSituationTogetherWithStatusChangesBothAndMovesDateToToday() {
        var updated = acme().update(statusChange("Reprovado na técnica", null, Situation.REJECTED), TODAY);

        assertThat(updated.process().status()).isEqualTo("Reprovado na técnica");
        assertThat(updated.process().situation()).isEqualTo(Situation.REJECTED);
        assertThat(updated.process().statusUpdatedOn()).isEqualTo(TODAY);
    }

    @Test
    void changingRequirementsAloneKeepsTheDescription() {
        var updated = acme().update(contentChange(null, List.of(NODE)), TODAY);

        assertThat(updated.posting().requirements()).containsExactly(NODE);
        assertThat(updated.posting().description()).isEqualTo("Description");
    }

    @Test
    void changingDescriptionTogetherWithRequirementsChangesBoth() {
        var updated = acme().update(contentChange("Kafka e Java", List.of(JAVA)), TODAY);

        assertThat(updated.posting().description()).isEqualTo("Kafka e Java");
        assertThat(updated.posting().requirements()).containsExactly(JAVA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankDescriptionIsRejectedOnUpdate(String description) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> acme().update(contentChange(description, List.of()), TODAY))
                .withMessage("Campo obrigatório: description");
    }

    // ---------- invariantes de datas na edição

    @Test
    void statusDateBeforeApplicationIsRejectedOnUpdate() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> acme().update(statusChange("Entrevista", LocalDate.of(2026, 11, 19), null), TODAY))
                .withMessage("A data do status não pode ser anterior à candidatura: 2026-11-19");
    }

    @Test
    void movingApplicationAfterAChangedStatusDateIsRejected() {
        var interviewed = acme().update(statusChange("Entrevista feita", LocalDate.of(2026, 11, 22), null), TODAY);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> interviewed.update(new JobApplicationChanges(null, null, null, null, null, null, null, null, LocalDate.of(2026, 11, 23), null, null, null), TODAY))
                .withMessage("A data do status não pode ser anterior à candidatura: 2026-11-22");
    }

    @Test
    void movingApplicationAfterAStepIsRejected() {
        var withStep = acme().update(processChange(null, null, null,
                List.of(new ProcessStep("Fit cultural", LocalDate.of(2026, 11, 21), true, null))), TODAY);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> withStep.update(new JobApplicationChanges(null, null, null, null, null, null, null, null, LocalDate.of(2026, 11, 22), null, null, null), TODAY))
                .withMessage("A etapa não pode ser anterior à candidatura: Fit cultural (2026-11-21)");
    }

    private static JobApplicationChanges statusChange(String status, LocalDate on, Situation situation) {
        return new JobApplicationChanges(null, null, null, null, null, null, null, null, null, null, new StatusChange(status, on, situation), null);
    }

    private static JobApplicationChanges contentChange(String description, List<Requirement> requirements) {
        return new JobApplicationChanges(null, null, null, null, null, null, null, null, null, new ContentChange(description, requirements), null, null);
    }

    // ---------- campos da vaga (contracted: CLT e faixa preenchidos)

    @Test
    void updatesPostingFieldsAndKeepsTheOthers() {
        var updated = acme().update(postingChange("Banco Alfa", "Sênior", "PJ", "R$ 15.000"), TODAY);

        assertThat(updated.posting()).isEqualTo(new JobPosting("Acme", "Banco Alfa", "Java Developer", "Sênior", "PJ",
                "R$ 15.000", "Description", "https://acme.com", List.of()));
        assertThat(updated.process()).isEqualTo(acme().process());
    }

    @Test
    void updatingOnlyOnePostingFieldIsAValidChange() {
        assertThat(acme().update(postingChange(null, "Pleno", null, null), TODAY).posting().seniority())
                .isEqualTo("Pleno");
    }

    @Test
    void omittedPostingFieldsKeepTheirValues() {
        var updated = contracted().update(new JobApplicationChanges(null, null, "Analyst", null, null, null,
                null, null, null, null, null, null), TODAY);

        assertThat(updated.posting().contractType()).isEqualTo("CLT");
        assertThat(updated.posting().salaryRange()).isEqualTo("R$ 8.000");
        assertThat(updated.posting().finalClient()).isEqualTo("Banco Alfa");
        assertThat(updated.posting().seniority()).isEqualTo("Pleno");
    }

    @Test
    void blankTextRemovesOptionalPostingFields() {
        var updated = contracted().update(postingChange("", " ", "", ""), TODAY);

        assertThat(updated.posting().finalClient()).isNull();
        assertThat(updated.posting().seniority()).isNull();
        assertThat(updated.posting().contractType()).isNull();
        assertThat(updated.posting().salaryRange()).isNull();
    }

    @Test
    void removingContractTypeWhileSalaryRangeStaysIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> contracted().update(postingChange(null, null, "", null), TODAY))
                .withMessage("Para informar a faixa salarial, informe também o tipo de contrato");
    }

    @Test
    void addingSalaryRangeUsesTheExistingContractType() {
        var withContract = acme().update(postingChange(null, null, "CLT", null), TODAY);

        assertThat(withContract.update(postingChange(null, null, null, "R$ 9.000"), TODAY).posting().salaryRange())
                .isEqualTo("R$ 9.000");
    }

    @Test
    void addingSalaryRangeWithoutAnyContractTypeIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> acme().update(postingChange(null, null, null, "R$ 9.000"), TODAY))
                .withMessage("Para informar a faixa salarial, informe também o tipo de contrato");
    }

    private static JobApplication contracted() {
        return acme().update(postingChange("Banco Alfa", "Pleno", "CLT", "R$ 8.000"), TODAY);
    }

    private static JobApplicationChanges postingChange(String finalClient, String seniority, String contractType,
                                                       String salaryRange) {
        return new JobApplicationChanges(null, finalClient, null, seniority, contractType, salaryRange,
                null, null, null, null, null, null);
    }

    private static final ProcessStep STEP_A = new ProcessStep("Candidatura", LocalDate.of(2026, 11, 20), true, null);
    private static final ProcessStep STEP_B = new ProcessStep("Entrevista técnica", null, false, "Aguardando data");

    private static JobApplicationChanges processChange(String status, LocalDate statusUpdatedOn, Situation situation,
                                                       List<ProcessStep> steps) {
        return new JobApplicationChanges(null, null, null, null, null, null, null, null, null, null, StatusChange.of(status, statusUpdatedOn, situation), steps);
    }

    private static JobApplication acme() {
        return new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", "https://acme.com", List.of()),
                ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 20)));
    }
}
