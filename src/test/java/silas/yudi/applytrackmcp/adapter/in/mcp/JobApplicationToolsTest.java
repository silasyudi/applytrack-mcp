package silas.yudi.applytrackmcp.adapter.in.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import silas.yudi.applytrackmcp.application.service.JobApplicationService;
import silas.yudi.applytrackmcp.domain.model.JobApplication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationToolsTest {

    @Mock
    private JobApplicationService service;

    @InjectMocks
    private JobApplicationTools tools;

    private final JobApplication acme = new JobApplication(
            "Acme", "Java Developer", "Full description", "Gupy", "https://acme.com/jobs/1");

    @Test
    void registersApplicationAndConfirms() {
        var message = tools.registerJobApplication(
                acme.company(), acme.jobTitle(), acme.description(), acme.platform(), acme.link());

        verify(service).register(acme);
        assertThat(message).isEqualTo("Candidatura registrada: Acme - Java Developer");
    }

    @Test
    void doesNotRegisterInvalidApplication() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.registerJobApplication(" ", "Java Developer", "Description", "Gupy", null))
                .withMessage("Campo obrigatório: company");
        verifyNoInteractions(service);
    }

    @Test
    void returnsAllApplicationsFound() {
        var acmeLabs = new JobApplication("Acme Labs", "Analyst", "Another description", "WhatsApp", null);
        when(service.search("acme")).thenReturn(List.of(acme, acmeLabs));

        assertThat(tools.searchJobApplications("acme")).containsExactly(acme, acmeLabs);
    }
}
