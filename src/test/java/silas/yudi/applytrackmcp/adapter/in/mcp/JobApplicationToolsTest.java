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
import silas.yudi.applytrackmcp.domain.model.ApplicationProcess;
import silas.yudi.applytrackmcp.domain.model.JobPosting;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.Importance;
import silas.yudi.applytrackmcp.domain.model.JobApplicationChanges;
import silas.yudi.applytrackmcp.domain.model.Requirement;
import silas.yudi.applytrackmcp.domain.model.Situation;

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
                new JobPosting("Acme", "Java Developer", "Full description", "https://acme.com/jobs/1", List.of()),
                ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));

    @Test
    void registersApplicationAndConfirms() {
        var message = tools.registerJobApplication(
                acme.posting().company(), acme.posting().jobTitle(), acme.posting().description(), acme.process().platform(), acme.posting().link(), "2026-11-20", null, null, null, null, null);

        var registered = ArgumentCaptor.forClass(JobApplication.class);
        verify(service).register(registered.capture());
        assertThat(registered.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(acme);
        assertThat(message).isEqualTo("Candidatura registrada: Acme - Java Developer");
    }

    @Test
    void doesNotRegisterInvalidApplication() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.registerJobApplication(" ", "Java Developer", "Description", "Gupy", null, "2026-11-20", null, null, null, null, null))
                .withMessage("Campo obrigatório: company");
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void requiresDateAndRegistersNothing(String appliedOn) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.registerJobApplication(
                        "Acme", "Java Developer", "Description", "Gupy", null, appliedOn, null, null, null, null, null))
                .withMessage("Campo obrigatório: appliedOn");
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"20/11/2026", "2026-02-30", "ontem"})
    void rejectsInvalidDateAndRegistersNothing(String appliedOn) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.registerJobApplication(
                        "Acme", "Java Developer", "Description", "Gupy", null, appliedOn, null, null, null, null, null))
                .withMessage("Data inválida: " + appliedOn + ". Use o formato AAAA-MM-DD");
        verifyNoInteractions(service);
    }

    @Test
    void returnsSummaryOfAllApplicationsFound() {
        var acmeLabs = new JobApplication(
                new JobPosting("Acme Labs", "Analyst", "Another description", null, List.of()),
                ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 20)));
        when(service.search("acme")).thenReturn(List.of(acme, acmeLabs));

        assertThat(tools.searchJobApplications("acme")).containsExactly(
                new JobApplicationSummary(acme.id(), "Acme", "Java Developer", "Gupy", "https://acme.com/jobs/1",
                        LocalDate.of(2026, 11, 20), List.of(),
                        "Candidatura enviada", LocalDate.of(2026, 11, 20), Situation.ACTIVE),
                new JobApplicationSummary(acmeLabs.id(), "Acme Labs", "Analyst", "WhatsApp", null,
                        LocalDate.of(2026, 11, 20), List.of(),
                        "Candidatura enviada", LocalDate.of(2026, 11, 20), Situation.ACTIVE));
    }

    @Test
    void updatesApplicationAndConfirms() {
        var updated = new JobApplication(acme.id(),
                new JobPosting("Beta", "Analyst", "New description", "https://beta.com", List.of()),
                ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 21)));
        when(service.update("id-1", new JobApplicationChanges("Beta", "Analyst", "New description", "WhatsApp", "https://beta.com", LocalDate.of(2026, 11, 21), null, null, null, null, null))).thenReturn(updated);

        assertThat(tools.updateJobApplication("id-1", "Beta", "Analyst", "New description", "WhatsApp",
                "https://beta.com", "2026-11-21", null, null, null, null, null)).isEqualTo("Candidatura atualizada: Beta - Analyst");
    }

    @Test
    void sendsOnlyTheGivenFields() {
        when(service.update("id-1", new JobApplicationChanges(null, null, null, "Gupy", "", null, null, null, null, null, null))).thenReturn(acme);

        assertThat(tools.updateJobApplication("id-1", null, null, null, "Gupy", "", null, null, null, null, null, null))
                .isEqualTo("Candidatura atualizada: Acme - Java Developer");
    }

    @Test
    void rejectsInvalidDateAndUpdatesNothing() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.updateJobApplication("id-1", null, null, null, null, null, "20/11/2026", null, null, null, null, null))
                .withMessage("Data inválida: 20/11/2026. Use o formato AAAA-MM-DD");
        verifyNoInteractions(service);
    }

    // Em branco não é "manter": seria confirmar uma alteração que não aconteceu
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankDateAndUpdatesNothing(String appliedOn) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.updateJobApplication("id-1", "Beta", null, null, null, null, appliedOn, null, null, null, null, null))
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
                        new RequirementInput("Inglês", "C1", Importance.DESIRABLE)), null, null, null, null);

        var registered = ArgumentCaptor.forClass(JobApplication.class);
        verify(service).register(registered.capture());
        assertThat(registered.getValue().posting().requirements()).isEqualTo(requirements);
    }

    @Test
    void sendsRequirementsToUpdate() {
        var requirements = List.of(new Requirement("Kafka", null, Importance.DESIRABLE));
        when(service.update("id-1", new JobApplicationChanges(null, null, null, null, null, null, requirements, null, null, null, null)))
                .thenReturn(acme);

        assertThat(tools.updateJobApplication("id-1", null, null, null, null, null, null, 
                List.of(new RequirementInput("Kafka", " ", Importance.DESIRABLE)), null, null, null, null))
                .isEqualTo("Candidatura atualizada: Acme - Java Developer");
    }

    @Test
    void summaryIncludesRequirements() {
        var requirements = List.of(new Requirement("Java", "Avançado", Importance.REQUIRED));
        var withRequirements = new JobApplication(acme.id(),
                new JobPosting("Acme", "Java Developer", "Full description", null, requirements),
                ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
        when(service.search("acme")).thenReturn(List.of(withRequirements));

        assertThat(tools.searchJobApplications("acme")).containsExactly(new JobApplicationSummary(
                acme.id(), "Acme", "Java Developer", "Gupy", null, LocalDate.of(2026, 11, 20), requirements,
                        "Candidatura enviada", LocalDate.of(2026, 11, 20), Situation.ACTIVE));
    }

    @Test
    void registersWithDefaultProcessWhenProcessFieldsAreMissing() {
        tools.registerJobApplication("Acme", "Java Developer", "Description", "Gupy", null, "2026-11-20", null,
                null, null, null, null);

        var registered = ArgumentCaptor.forClass(JobApplication.class);
        verify(service).register(registered.capture());
        assertThat(registered.getValue().process()).isEqualTo(ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
    }

    @Test
    void registersProcessFields() {
        tools.registerJobApplication("Acme", "Java Developer", "Description", "Gupy", null, "2026-11-20", null,
                "Entrevista técnica realizada, aguardando feedback", "2026-11-24", Situation.ACTIVE, List.of(
                        new ProcessStepInput("Candidatura", "2026-11-20", true, null),
                        new ProcessStepInput("Entrevista técnica", "2026-11-24", true, "Perguntaram sobre Kafka"),
                        new ProcessStepInput("Entrevista com líder", null, null, null)));

        var registered = ArgumentCaptor.forClass(JobApplication.class);
        verify(service).register(registered.capture());
        assertThat(registered.getValue().process()).isEqualTo(new ApplicationProcess("Gupy", LocalDate.of(2026, 11, 20),
                "Entrevista técnica realizada, aguardando feedback", LocalDate.of(2026, 11, 24), Situation.ACTIVE, List.of(
                        new ProcessStep("Candidatura", LocalDate.of(2026, 11, 20), true, null),
                        new ProcessStep("Entrevista técnica", LocalDate.of(2026, 11, 24), true, "Perguntaram sobre Kafka"),
                        new ProcessStep("Entrevista com líder", null, false, null))));
    }

    @Test
    void sendsProcessFieldsToUpdate() {
        when(service.update("id-1", new JobApplicationChanges(null, null, null, null, null, null, null,
                "Reprovado na técnica", LocalDate.of(2026, 11, 24), Situation.REJECTED,
                List.of(new ProcessStep("Entrevista técnica", LocalDate.of(2026, 11, 23), true, null)))))
                .thenReturn(acme);

        assertThat(tools.updateJobApplication("id-1", null, null, null, null, null, null, null,
                "Reprovado na técnica", "2026-11-24", Situation.REJECTED,
                List.of(new ProcessStepInput("Entrevista técnica", "2026-11-23", true, null))))
                .isEqualTo("Candidatura atualizada: Acme - Java Developer");
    }

    @ParameterizedTest
    @ValueSource(strings = {"24/11/2026", "2026-02-30"})
    void rejectsInvalidStatusOrStepDateAndRegistersNothing(String date) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.registerJobApplication("Acme", "Java Developer", "Description", "Gupy", null,
                        "2026-11-20", null, null, date, null, null))
                .withMessage("Data inválida: " + date + ". Use o formato AAAA-MM-DD");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.registerJobApplication("Acme", "Java Developer", "Description", "Gupy", null,
                        "2026-11-20", null, null, null, null, List.of(new ProcessStepInput("Técnica", date, null, null))))
                .withMessage("Data inválida: " + date + ". Use o formato AAAA-MM-DD");
        verifyNoInteractions(service);
    }
}
