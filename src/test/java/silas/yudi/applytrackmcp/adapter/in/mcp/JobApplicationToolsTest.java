package silas.yudi.applytrackmcp.adapter.in.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import silas.yudi.applytrackmcp.application.service.JobApplicationService;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.Importance;
import silas.yudi.applytrackmcp.domain.model.JobApplicationChanges;
import silas.yudi.applytrackmcp.domain.model.Requirement;

import java.time.LocalDate;
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
            "Acme", "Java Developer", "Full description", "Gupy", "https://acme.com/jobs/1", LocalDate.of(2026, 11, 20), List.of());

    @Test
    void registersApplicationAndConfirms() {
        var message = tools.registerJobApplication(
                acme.company(), acme.jobTitle(), acme.description(), acme.platform(), acme.link(), "2026-11-20", null);

        var registered = ArgumentCaptor.forClass(JobApplication.class);
        verify(service).register(registered.capture());
        assertThat(registered.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(acme);
        assertThat(message).isEqualTo("Candidatura registrada: Acme - Java Developer");
    }

    @Test
    void doesNotRegisterInvalidApplication() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.registerJobApplication(" ", "Java Developer", "Description", "Gupy", null, "2026-11-20", null))
                .withMessage("Campo obrigatório: company");
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresDateAndRegistersNothing(String appliedOn) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.registerJobApplication(
                        "Acme", "Java Developer", "Description", "Gupy", null, appliedOn, null))
                .withMessage("Campo obrigatório: appliedOn");
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"20/11/2026", "2026-02-30", "ontem"})
    void rejectsInvalidDateAndRegistersNothing(String appliedOn) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.registerJobApplication(
                        "Acme", "Java Developer", "Description", "Gupy", null, appliedOn, null))
                .withMessage("Data inválida: " + appliedOn + ". Use o formato AAAA-MM-DD");
        verifyNoInteractions(service);
    }

    @Test
    void returnsSummaryOfAllApplicationsFound() {
        var acmeLabs = new JobApplication("Acme Labs", "Analyst", "Another description", "WhatsApp", null, LocalDate.of(2026, 11, 20), List.of());
        when(service.search("acme")).thenReturn(List.of(acme, acmeLabs));

        assertThat(tools.searchJobApplications("acme")).containsExactly(
                new JobApplicationSummary(acme.id(), "Acme", "Java Developer", "Gupy", "https://acme.com/jobs/1",
                        LocalDate.of(2026, 11, 20), List.of()),
                new JobApplicationSummary(acmeLabs.id(), "Acme Labs", "Analyst", "WhatsApp", null,
                        LocalDate.of(2026, 11, 20), List.of()));
    }

    @Test
    void updatesApplicationAndConfirms() {
        var updated = new JobApplication(acme.id(), "Beta", "Analyst", "New description", "WhatsApp",
                "https://beta.com", LocalDate.of(2026, 11, 21), List.of());
        when(service.update("id-1", new JobApplicationChanges("Beta", "Analyst", "New description", "WhatsApp",
                "https://beta.com", LocalDate.of(2026, 11, 21), null))).thenReturn(updated);

        assertThat(tools.updateJobApplication("id-1", "Beta", "Analyst", "New description", "WhatsApp",
                "https://beta.com", "2026-11-21", null)).isEqualTo("Candidatura atualizada: Beta - Analyst");
    }

    @Test
    void sendsOnlyTheGivenFields() {
        when(service.update("id-1", new JobApplicationChanges(null, null, null, "Gupy", "", null, null))).thenReturn(acme);

        assertThat(tools.updateJobApplication("id-1", null, null, null, "Gupy", "", null, null))
                .isEqualTo("Candidatura atualizada: Acme - Java Developer");
    }

    @Test
    void rejectsInvalidDateAndUpdatesNothing() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.updateJobApplication("id-1", null, null, null, null, null, "20/11/2026", null))
                .withMessage("Data inválida: 20/11/2026. Use o formato AAAA-MM-DD");
        verifyNoInteractions(service);
    }

    // Em branco não é "manter": seria confirmar uma alteração que não aconteceu
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankDateAndUpdatesNothing(String appliedOn) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.updateJobApplication("id-1", "Beta", null, null, null, null, appliedOn, null))
                .withMessage("Campo obrigatório: appliedOn");
        verifyNoInteractions(service);
    }

    @Test
    void deletesApplicationAndConfirms() {
        when(service.delete("id-1")).thenReturn(acme);

        assertThat(tools.deleteJobApplication("id-1")).isEqualTo("Candidatura removida: Acme - Java Developer");
    }

    @Test
    void returnsCompleteApplication() {
        when(service.get("id-1")).thenReturn(acme);

        assertThat(tools.getJobApplication("id-1")).isEqualTo(acme);
    }

    @Test
    void registersRequirements() {
        var requirements = List.of(new Requirement("Java", "Avançado", Importance.REQUIRED),
                new Requirement("Inglês", "C1", Importance.DESIRABLE));

        tools.registerJobApplication("Acme", "Java Developer", "Description", "Gupy", null, "2026-11-20",
                List.of(new RequirementInput(" Java ", "Avançado", Importance.REQUIRED),
                        new RequirementInput("Inglês", "C1", Importance.DESIRABLE)));

        var registered = ArgumentCaptor.forClass(JobApplication.class);
        verify(service).register(registered.capture());
        assertThat(registered.getValue().requirements()).isEqualTo(requirements);
    }

    @Test
    void sendsRequirementsToUpdate() {
        var requirements = List.of(new Requirement("Kafka", null, Importance.DESIRABLE));
        when(service.update("id-1", new JobApplicationChanges(null, null, null, null, null, null, requirements)))
                .thenReturn(acme);

        assertThat(tools.updateJobApplication("id-1", null, null, null, null, null, null, 
                List.of(new RequirementInput("Kafka", " ", Importance.DESIRABLE))))
                .isEqualTo("Candidatura atualizada: Acme - Java Developer");
    }

    @Test
    void summaryIncludesRequirements() {
        var requirements = List.of(new Requirement("Java", "Avançado", Importance.REQUIRED));
        var withRequirements = new JobApplication(acme.id(), "Acme", "Java Developer", "Full description", "Gupy",
                null, LocalDate.of(2026, 11, 20), requirements);
        when(service.search("acme")).thenReturn(List.of(withRequirements));

        assertThat(tools.searchJobApplications("acme")).containsExactly(new JobApplicationSummary(
                acme.id(), "Acme", "Java Developer", "Gupy", null, LocalDate.of(2026, 11, 20), requirements));
    }
}
