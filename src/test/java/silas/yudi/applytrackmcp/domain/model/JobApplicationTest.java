package silas.yudi.applytrackmcp.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

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
}
