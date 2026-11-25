package silas.yudi.applytrackmcp.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import silas.yudi.applytrackmcp.domain.model.JobApplication;

import static org.assertj.core.api.Assertions.assertThat;

class JobApplicationServiceTest {

    private final JobApplication acme = new JobApplication(
            "Acme", "Java Developer", "Full description", "Gupy", "https://acme.com/jobs/1");
    private final JobApplication solucoes = new JobApplication(
            "Soluções Beta", "Pessoa Desenvolvedora Sênior", "Full description", "WhatsApp", null);

    private final InMemoryJobApplicationRepository repository = new InMemoryJobApplicationRepository();
    private final JobApplicationService service = new JobApplicationService(repository);

    @Test
    void registersApplication() {
        service.register(acme);

        assertThat(repository.findAll()).containsExactly(acme);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Soluções Beta", "solucoes", "SOLUÇÕES", "beta", " beta ", "senior", "desenvolvedora"})
    void searchesByPartOfCompanyOrJobTitleIgnoringCaseAndAccents(String term) {
        service.register(acme);
        service.register(solucoes);

        assertThat(service.search(term)).containsExactly(solucoes);
    }

    @Test
    void returnsNothingWhenNoApplicationMatches() {
        service.register(acme);

        assertThat(service.search("unknown")).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void listsEverythingWhenTermIsMissing(String term) {
        service.register(acme);
        service.register(solucoes);

        assertThat(service.search(term)).containsExactly(acme, solucoes);
    }
}
