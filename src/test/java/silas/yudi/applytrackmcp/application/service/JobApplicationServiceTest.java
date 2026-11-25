package silas.yudi.applytrackmcp.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.domain.model.JobApplication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository repository;

    @InjectMocks
    private JobApplicationService service;

    private final JobApplication acme = new JobApplication(
            "Acme", "Java Developer", "Full description", "Gupy", "https://acme.com/jobs/1");
    private final JobApplication solucoes = new JobApplication(
            "Soluções Beta", "Pessoa Desenvolvedora Sênior", "Full description", "WhatsApp", null);

    @Test
    void registersApplicationOnce() {
        service.register(acme);

        verify(repository).save(acme);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Soluções Beta", "solucoes", "SOLUÇÕES", "beta", " beta ", "senior", "desenvolvedora"})
    void searchesByPartOfCompanyOrJobTitleIgnoringCaseAndAccents(String term) {
        when(repository.findAll()).thenReturn(List.of(acme, solucoes));

        assertThat(service.search(term)).containsExactly(solucoes);
    }

    @Test
    void returnsNothingWhenNoApplicationMatches() {
        when(repository.findAll()).thenReturn(List.of(acme));

        assertThat(service.search("unknown")).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void listsEverythingWhenTermIsMissing(String term) {
        when(repository.findAll()).thenReturn(List.of(acme, solucoes));

        assertThat(service.search(term)).containsExactly(acme, solucoes);
    }
}
