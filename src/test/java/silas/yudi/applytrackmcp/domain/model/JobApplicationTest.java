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

    @Test
    void keepsDataWithoutSurroundingSpaces() {
        var application = new JobApplication(
                " Acme ", " Java Developer ", " Full description ", " LinkedIn ", " https://acme.com/jobs/1 ", LocalDate.of(2026, 11, 20), List.of());

        assertThat(application.company()).isEqualTo("Acme");
        assertThat(application.jobTitle()).isEqualTo("Java Developer");
        assertThat(application.description()).isEqualTo("Full description");
        assertThat(application.platform()).isEqualTo("LinkedIn");
        assertThat(application.link()).isEqualTo("https://acme.com/jobs/1");
        assertThat(application.appliedOn()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresCompany(String company) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication(company, "Java Developer", "Description", "LinkedIn", null, LocalDate.of(2026, 11, 20), List.of()))
                .withMessage("Campo obrigatório: company");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresJobTitle(String jobTitle) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication("Acme", jobTitle, "Description", "LinkedIn", null, LocalDate.of(2026, 11, 20), List.of()))
                .withMessage("Campo obrigatório: jobTitle");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresDescription(String description) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication("Acme", "Java Developer", description, "LinkedIn", null, LocalDate.of(2026, 11, 20), List.of()))
                .withMessage("Campo obrigatório: description");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresPlatform(String platform) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication("Acme", "Java Developer", "Description", platform, null, LocalDate.of(2026, 11, 20), List.of()))
                .withMessage("Campo obrigatório: platform");
    }

    // Há processos que só chegam por e-mail ou WhatsApp
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void linkIsOptional(String link) {
        assertThat(new JobApplication("Acme", "Java Developer", "Description", "WhatsApp", link, LocalDate.of(2026, 11, 20), List.of()).link()).isNull();
    }

    // Sem data não dá para calcular prazos
    @Test
    void requiresAppliedOn() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication("Acme", "Java Developer", "Description", "LinkedIn", null, null, List.of()))
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
                .isThrownBy(() -> new JobApplication(
                        id, "Acme", "Java Developer", "Description", "LinkedIn", null, LocalDate.of(2026, 11, 20), List.of()))
                .withMessage("Campo obrigatório: id");
    }

    @Test
    void updatesAllFieldsAndKeepsId() {
        var original = acme();

        var updated = original.update(new JobApplicationChanges(
                "Beta", "Analyst", "New description", "Gupy", "https://beta.com", LocalDate.of(2026, 11, 21), null));

        assertThat(updated).isEqualTo(new JobApplication(
                original.id(), "Beta", "Analyst", "New description", "Gupy", "https://beta.com", LocalDate.of(2026, 11, 21), List.of()));
    }

    @Test
    void keepsFieldsThatAreNotInTheChanges() {
        var original = acme();

        assertThat(original.update(new JobApplicationChanges(null, "Analyst", null, null, null, null, null)))
                .isEqualTo(new JobApplication(original.id(), "Acme", "Analyst", "Description", "LinkedIn",
                        "https://acme.com", LocalDate.of(2026, 11, 20), List.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankLinkRemovesTheLink(String link) {
        var original = acme();

        assertThat(original.update(new JobApplicationChanges(null, null, null, null, link, null, null)))
                .isEqualTo(new JobApplication(original.id(), "Acme", "Java Developer", "Description", "LinkedIn",
                        null, LocalDate.of(2026, 11, 20), List.of()));
    }

    @Test
    void updateCannotEraseRequiredField() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> acme().update(new JobApplicationChanges(" ", null, null, null, null, null, null)))
                .withMessage("Campo obrigatório: company");
    }

    @Test
    void updateRequiresAtLeastOneChange() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> acme().update(new JobApplicationChanges(null, null, null, null, null, null, null)))
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
                "Acme", "Java Developer", "Description", "LinkedIn", null, LocalDate.of(2026, 11, 20), null);

        assertThat(application.requirements()).isEmpty();
    }

    @Test
    void keepsRequirementsInOrderAndProtectedFromOutsideChanges() {
        var requirements = new ArrayList<>(List.of(JAVA, ENGLISH));
        var application = new JobApplication(
                "Acme", "Java Developer", "Description", "LinkedIn", null, LocalDate.of(2026, 11, 20), requirements);

        requirements.add(NODE);

        assertThat(application.requirements()).containsExactly(JAVA, ENGLISH);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> application.requirements().add(NODE));
    }

    @Test
    void rejectsNullRequirement() {
        var requirements = new ArrayList<Requirement>();
        requirements.add(JAVA);
        requirements.add(null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication("Acme", "Java Developer", "Description", "LinkedIn", null,
                        LocalDate.of(2026, 11, 20), requirements))
                .withMessage("Campo obrigatório: requirement");
    }

    @Test
    void updateReplacesTheWholeRequirementList() {
        var original = withRequirements(JAVA, ENGLISH);

        var updated = original.update(requirementsChange(List.of(NODE)));

        assertThat(updated.requirements()).containsExactly(NODE);
        assertThat(updated.jobTitle()).isEqualTo("Java Developer");
    }

    @Test
    void updateWithEmptyListRemovesAllRequirements() {
        assertThat(withRequirements(JAVA, ENGLISH).update(requirementsChange(List.of())).requirements()).isEmpty();
    }

    @Test
    void updateWithoutRequirementsKeepsThem() {
        var updated = withRequirements(JAVA, ENGLISH)
                .update(new JobApplicationChanges(null, "Analyst", null, null, null, null, null));

        assertThat(updated.requirements()).containsExactly(JAVA, ENGLISH);
    }

    private static final Requirement JAVA = new Requirement("Java", "Avançado", Importance.REQUIRED);
    private static final Requirement ENGLISH = new Requirement("Inglês", "C1", Importance.DESIRABLE);
    private static final Requirement NODE = new Requirement("Node.js", null, Importance.UNSPECIFIED);

    private static JobApplication withRequirements(Requirement... requirements) {
        return new JobApplication("Acme", "Java Developer", "Description", "LinkedIn", null,
                LocalDate.of(2026, 11, 20), List.of(requirements));
    }

    private static JobApplicationChanges requirementsChange(List<Requirement> requirements) {
        return new JobApplicationChanges(null, null, null, null, null, null, requirements);
    }

    private static JobApplication acme() {
        return new JobApplication(
                "Acme", "Java Developer", "Description", "LinkedIn", "https://acme.com", LocalDate.of(2026, 11, 20), List.of());
    }
}
