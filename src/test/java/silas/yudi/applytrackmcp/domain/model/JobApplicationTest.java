package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JobApplicationTest {

    @Test
    void keepsDataWithoutSurroundingSpaces() {
        var application = new JobApplication(
                " Acme ", " Java Developer ", " Full description ", " LinkedIn ", " https://acme.com/jobs/1 ", LocalDate.of(2026, 11, 20));

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
                .isThrownBy(() -> new JobApplication(company, "Java Developer", "Description", "LinkedIn", null, LocalDate.of(2026, 11, 20)))
                .withMessage("Campo obrigatório: company");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresJobTitle(String jobTitle) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication("Acme", jobTitle, "Description", "LinkedIn", null, LocalDate.of(2026, 11, 20)))
                .withMessage("Campo obrigatório: jobTitle");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresDescription(String description) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication("Acme", "Java Developer", description, "LinkedIn", null, LocalDate.of(2026, 11, 20)))
                .withMessage("Campo obrigatório: description");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresPlatform(String platform) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication("Acme", "Java Developer", "Description", platform, null, LocalDate.of(2026, 11, 20)))
                .withMessage("Campo obrigatório: platform");
    }

    // Há processos que só chegam por e-mail ou WhatsApp
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void linkIsOptional(String link) {
        assertThat(new JobApplication("Acme", "Java Developer", "Description", "WhatsApp", link, LocalDate.of(2026, 11, 20)).link()).isNull();
    }

    // Sem data não dá para calcular prazos
    @Test
    void requiresAppliedOn() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JobApplication("Acme", "Java Developer", "Description", "LinkedIn", null, null))
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
                        id, "Acme", "Java Developer", "Description", "LinkedIn", null, LocalDate.of(2026, 11, 20)))
                .withMessage("Campo obrigatório: id");
    }

    @Test
    void updatesAllFieldsAndKeepsId() {
        var original = acme();

        var updated = original.update(new JobApplicationChanges(
                "Beta", "Analyst", "New description", "Gupy", "https://beta.com", LocalDate.of(2026, 11, 21)));

        assertThat(updated).isEqualTo(new JobApplication(
                original.id(), "Beta", "Analyst", "New description", "Gupy", "https://beta.com", LocalDate.of(2026, 11, 21)));
    }

    @Test
    void keepsFieldsThatAreNotInTheChanges() {
        var original = acme();

        assertThat(original.update(new JobApplicationChanges(null, "Analyst", null, null, null, null)))
                .isEqualTo(new JobApplication(original.id(), "Acme", "Analyst", "Description", "LinkedIn",
                        "https://acme.com", LocalDate.of(2026, 11, 20)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankLinkRemovesTheLink(String link) {
        var original = acme();

        assertThat(original.update(new JobApplicationChanges(null, null, null, null, link, null)))
                .isEqualTo(new JobApplication(original.id(), "Acme", "Java Developer", "Description", "LinkedIn",
                        null, LocalDate.of(2026, 11, 20)));
    }

    @Test
    void updateCannotEraseRequiredField() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> acme().update(new JobApplicationChanges(" ", null, null, null, null, null)))
                .withMessage("Campo obrigatório: company");
    }

    @Test
    void updateRequiresAtLeastOneChange() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> acme().update(new JobApplicationChanges(null, null, null, null, null, null)))
                .withMessage("Informe ao menos um campo para alterar");
    }

    private static JobApplication acme() {
        return new JobApplication(
                "Acme", "Java Developer", "Description", "LinkedIn", "https://acme.com", LocalDate.of(2026, 11, 20));
    }
}
