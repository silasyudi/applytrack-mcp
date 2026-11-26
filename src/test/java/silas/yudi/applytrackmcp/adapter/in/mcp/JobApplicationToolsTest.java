package silas.yudi.applytrackmcp.adapter.in.mcp;

import org.junit.jupiter.api.Nested;
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
import silas.yudi.applytrackmcp.domain.model.ApplicationProcess;
import silas.yudi.applytrackmcp.domain.model.ContentChange;
import silas.yudi.applytrackmcp.domain.model.Importance;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.JobApplicationChanges;
import silas.yudi.applytrackmcp.domain.model.JobPosting;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.Requirement;
import silas.yudi.applytrackmcp.domain.model.Situation;
import silas.yudi.applytrackmcp.domain.model.StatusChange;

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
                new JobPosting("Acme", null, "Java Developer", null, null, null, "https://acme.com/jobs/1",
                        "Full description", List.of()),
                ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));

    @Nested
    class Register {

        @Test
        void registersApplicationAndConfirms() {
            var message = tools.registerJobApplication(acme.posting().company(), null, acme.posting().jobTitle(),
                    null, null, null, acme.posting().link(), acme.posting().description(), null,
                    acme.process().platform(), "2026-11-20", null, null, null, null);

            var registered = ArgumentCaptor.forClass(JobApplication.class);
            verify(service).register(registered.capture());
            assertThat(registered.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(acme);
            assertThat(message).isEqualTo("Candidatura registrada: Acme - Java Developer");
        }

        @Test
        void registersPostingFields() {
            tools.registerJobApplication("Acme", "Banco Alfa", "Java Developer", "Sênior", "PJ", "R$ 15.000", null,
                    "Description", null, "Gupy", "2026-11-20", null, null, null, null);

            var registered = ArgumentCaptor.forClass(JobApplication.class);
            verify(service).register(registered.capture());
            assertThat(registered.getValue().posting()).isEqualTo(new JobPosting("Acme", "Banco Alfa",
                    "Java Developer", "Sênior", "PJ", "R$ 15.000", null, "Description", List.of()));
        }

        @Test
        void registersRequirements() {
            var requirements = List.of(new Requirement("Java", "Avançado", Importance.REQUIRED),
                    new Requirement("Inglês", "C1", Importance.DESIRABLE));

            tools.registerJobApplication("Acme", null, "Java Developer", null, null, null, null, "Description",
                    List.of(new RequirementInput(" Java ", "Avançado", Importance.REQUIRED),
                            new RequirementInput("Inglês", "C1", Importance.DESIRABLE)), "Gupy", "2026-11-20", null,
                    null, null, null);

            var registered = ArgumentCaptor.forClass(JobApplication.class);
            verify(service).register(registered.capture());
            assertThat(registered.getValue().posting().requirements()).isEqualTo(requirements);
        }

        @Test
        void registersWithDefaultProcessWhenProcessFieldsAreMissing() {
            tools.registerJobApplication("Acme", null, "Java Developer", null, null, null, null, "Description", null,
                    "Gupy", "2026-11-20", null, null, null, null);

            var registered = ArgumentCaptor.forClass(JobApplication.class);
            verify(service).register(registered.capture());
            assertThat(registered.getValue().process()).isEqualTo(ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
        }

        @Test
        void registersProcessFields() {
            tools.registerJobApplication("Acme", null, "Java Developer", null, null, null, null, "Description", null,
                    "Gupy", "2026-11-20", "Entrevista técnica realizada, aguardando feedback", "2026-11-24",
                    Situation.ACTIVE, List.of(
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

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void treatsBlankStepDateAsNoDate(String blank) {
            tools.registerJobApplication("Acme", null, "Java Developer", null, null, null, null, "Description", null,
                    "Gupy", "2026-11-20", null, null, null,
                    List.of(new ProcessStepInput("Entrevista técnica", blank, false, null)));

            var registered = ArgumentCaptor.forClass(JobApplication.class);
            verify(service).register(registered.capture());
            assertThat(registered.getValue().process().steps())
                    .containsExactly(new ProcessStep("Entrevista técnica", null, false, null));
        }

        @Test
        void doesNotRegisterInvalidApplication() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.registerJobApplication(" ", null, "Java Developer", null, null, null,
                            null, "Description", null, "Gupy", "2026-11-20", null, null, null, null))
                    .withMessage("Campo obrigatório: company");
            verifyNoInteractions(service);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void requiresDateAndRegistersNothing(String appliedOn) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.registerJobApplication("Acme", null, "Java Developer", null, null, null,
                            null, "Description", null, "Gupy", appliedOn, null, null, null, null))
                    .withMessage("Campo obrigatório: appliedOn");
            verifyNoInteractions(service);
        }

        @ParameterizedTest
        @ValueSource(strings = {"20/11/2026", "2026-02-30", "ontem"})
        void rejectsInvalidDateAndRegistersNothing(String appliedOn) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.registerJobApplication("Acme", null, "Java Developer", null, null, null,
                            null, "Description", null, "Gupy", appliedOn, null, null, null, null))
                    .withMessage("Data inválida: " + appliedOn + ". Use o formato AAAA-MM-DD");
            verifyNoInteractions(service);
        }

        @ParameterizedTest
        @ValueSource(strings = {"24/11/2026", "2026-02-30"})
        void rejectsInvalidStatusOrStepDateAndRegistersNothing(String date) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.registerJobApplication("Acme", null, "Java Developer", null, null, null,
                            null, "Description", null, "Gupy", "2026-11-20", null, date, null, null))
                    .withMessage("Data inválida: " + date + ". Use o formato AAAA-MM-DD");
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.registerJobApplication("Acme", null, "Java Developer", null, null, null,
                            null, "Description", null, "Gupy", "2026-11-20", null, null, null,
                            List.of(new ProcessStepInput("Técnica", date, null, null))))
                    .withMessage("Data inválida: " + date + ". Use o formato AAAA-MM-DD");
            verifyNoInteractions(service);
        }

        @Test
        void rejectsRegistrationWithStatusDateOrSituationWithoutStatus() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.registerJobApplication("Acme", null, "Java Developer", null, null, null,
                            null, "Description", null, "Gupy", "2026-11-20", null, "2026-11-22", null, null))
                    .withMessage("Para informar a data do status, envie também o status");
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.registerJobApplication("Acme", null, "Java Developer", null, null, null,
                            null, "Description", null, "Gupy", "2026-11-20", null, null, Situation.CLOSED, null))
                    .withMessage("Para informar a situação, envie também o status");
            verifyNoInteractions(service);
        }
    }

    @Nested
    class Search {

        @Test
        void returnsSummaryOfAllApplicationsFound() {
            var acmeLabs = new JobApplication(
                    new JobPosting("Acme Labs", null, "Analyst", null, null, null, null, "Another description",
                            List.of()),
                    ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 20)));
            when(service.search("acme")).thenReturn(List.of(acme, acmeLabs));

            assertThat(tools.searchJobApplications("acme")).containsExactly(
                    new JobApplicationSummary(acme.id(), "Acme", null, "Java Developer", null, null,
                            "https://acme.com/jobs/1", List.of(), "Gupy", LocalDate.of(2026, 11, 20),
                            "Candidatura enviada", LocalDate.of(2026, 11, 20), Situation.ACTIVE),
                    new JobApplicationSummary(acmeLabs.id(), "Acme Labs", null, "Analyst", null, null, null,
                            List.of(), "WhatsApp", LocalDate.of(2026, 11, 20), "Candidatura enviada",
                            LocalDate.of(2026, 11, 20), Situation.ACTIVE));
        }

        @Test
        void summaryIncludesFinalClientSeniorityAndContractButNotSalary() {
            var posted = new JobApplication(acme.id(), new JobPosting("Acme", "Banco Alfa", "Java Developer",
                    "Sênior", "PJ", "R$ 15.000", null, "Full description", List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            when(service.search("acme")).thenReturn(List.of(posted));

            assertThat(tools.searchJobApplications("acme")).containsExactly(new JobApplicationSummary(acme.id(),
                    "Acme", "Banco Alfa", "Java Developer", "Sênior", "PJ", null, List.of(), "Gupy",
                    LocalDate.of(2026, 11, 20), "Candidatura enviada", LocalDate.of(2026, 11, 20), Situation.ACTIVE));
        }

        @Test
        void summaryIncludesRequirements() {
            var requirements = List.of(new Requirement("Java", "Avançado", Importance.REQUIRED));
            var withRequirements = new JobApplication(acme.id(),
                    new JobPosting("Acme", null, "Java Developer", null, null, null, null, "Full description",
                            requirements),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            when(service.search("acme")).thenReturn(List.of(withRequirements));

            assertThat(tools.searchJobApplications("acme")).containsExactly(new JobApplicationSummary(acme.id(),
                    "Acme", null, "Java Developer", null, null, null, requirements, "Gupy",
                    LocalDate.of(2026, 11, 20), "Candidatura enviada", LocalDate.of(2026, 11, 20), Situation.ACTIVE));
        }
    }

    @Nested
    class Get {

        @Test
        void returnsCompleteApplication() {
            when(service.get("id-1")).thenReturn(acme);

            assertThat(tools.getJobApplication("id-1")).isEqualTo(acme);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesApplicationAndConfirms() {
            var updated = new JobApplication(acme.id(),
                    new JobPosting("Beta", null, "Analyst", null, null, null, "https://beta.com", "New description",
                            List.of()),
                    ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 21)));
            when(service.update("id-1", new JobApplicationChanges("Beta", null, "Analyst", null, null, null,
                    "https://beta.com", new ContentChange("New description", List.of()), "WhatsApp",
                    LocalDate.of(2026, 11, 21), null, null))).thenReturn(updated);

            assertThat(tools.updateJobApplication("id-1", "Beta", null, "Analyst", null, null, null,
                    "https://beta.com", "New description", List.of(), "WhatsApp", "2026-11-21", null, null, null, null))
                    .isEqualTo("Candidatura atualizada: Beta - Analyst");
        }

        @Test
        void sendsOnlyTheGivenFields() {
            when(service.update("id-1", new JobApplicationChanges(null, null, null, null, null, null, "", null,
                    "Gupy", null, null, null))).thenReturn(acme);

            assertThat(tools.updateJobApplication("id-1", null, null, null, null, null, null, "", null, null, "Gupy",
                    null, null, null, null, null))
                    .isEqualTo("Candidatura atualizada: Acme - Java Developer");
        }

        @Test
        void sendsPostingFieldsToUpdate() {
            when(service.update("id-1", new JobApplicationChanges(null, "Banco Alfa", null, "Sênior", "PJ", "",
                    null, null, null, null, null, null))).thenReturn(acme);

            assertThat(tools.updateJobApplication("id-1", null, "Banco Alfa", null, "Sênior", "PJ", "", null, null,
                    null, null, null, null, null, null, null)).isEqualTo("Candidatura atualizada: Acme - Java Developer");
        }

        @Test
        void sendsRequirementsToUpdate() {
            var requirements = List.of(new Requirement("Kafka", null, Importance.DESIRABLE));
            when(service.update("id-1", new JobApplicationChanges(null, null, null, null, null, null, null,
                    new ContentChange(null, requirements), null, null, null, null)))
                    .thenReturn(acme);

            assertThat(tools.updateJobApplication("id-1", null, null, null, null, null, null, null, null,
                    List.of(new RequirementInput("Kafka", " ", Importance.DESIRABLE)), null, null, null, null, null,
                    null))
                    .isEqualTo("Candidatura atualizada: Acme - Java Developer");
        }

        @Test
        void sendsProcessFieldsToUpdate() {
            when(service.update("id-1", new JobApplicationChanges(null, null, null, null, null, null, null, null,
                    null, null,
                    new StatusChange("Reprovado na técnica", LocalDate.of(2026, 11, 24), Situation.REJECTED),
                    List.of(new ProcessStep("Entrevista técnica", LocalDate.of(2026, 11, 23), true, null)))))
                    .thenReturn(acme);

            assertThat(tools.updateJobApplication("id-1", null, null, null, null, null, null, null, null, null, null,
                    null, "Reprovado na técnica", "2026-11-24", Situation.REJECTED,
                    List.of(new ProcessStepInput("Entrevista técnica", "2026-11-23", true, null))))
                    .isEqualTo("Candidatura atualizada: Acme - Java Developer");
        }

        @Test
        void rejectsUpdateOfDescriptionWithoutRequirements() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.updateJobApplication("id-1", null, null, null, null, null, null, null,
                            "Nova descrição", null, null, null, null, null, null, null))
                    .withMessage("Para informar a descrição, envie também os requisitos (lista vazia se a vaga não citar nenhum)");
            verifyNoInteractions(service);
        }

        @Test
        void rejectsInvalidDateAndUpdatesNothing() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.updateJobApplication("id-1", null, null, null, null, null, null, null,
                            null, null, null, "20/11/2026", null, null, null, null))
                    .withMessage("Data inválida: 20/11/2026. Use o formato AAAA-MM-DD");
            verifyNoInteractions(service);
        }

        // Em branco não é "manter": seria confirmar uma alteração que não aconteceu
        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejectsBlankDateAndUpdatesNothing(String appliedOn) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.updateJobApplication("id-1", "Beta", null, null, null, null, null, null,
                            null, null, null, appliedOn, null, null, null, null))
                    .withMessage("Campo obrigatório: appliedOn");
            verifyNoInteractions(service);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejectsBlankStatusOrStatusDateOnUpdate(String blank) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.updateJobApplication("id-1", null, null, null, null, null, null, null,
                            null, null, null, null, blank, null, null, null))
                    .withMessage("Campo obrigatório: status");
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.updateJobApplication("id-1", null, null, null, null, null, null, null,
                            null, null, null, null, "Entrevista", blank, null, null))
                    .withMessage("Campo obrigatório: statusUpdatedOn");
            verifyNoInteractions(service);
        }

        @Test
        void rejectsUpdateOfStatusDateOrSituationWithoutStatus() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.updateJobApplication("id-1", null, null, null, null, null, null, null,
                            null, null, null, null, null, "2026-11-24", null, null))
                    .withMessage("Para informar a data do status, envie também o status");
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.updateJobApplication("id-1", null, null, null, null, null, null, null,
                            null, null, null, null, null, null, Situation.REJECTED, null))
                    .withMessage("Para informar a situação, envie também o status");
            verifyNoInteractions(service);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesApplicationAndConfirms() {
            when(service.delete("id-1")).thenReturn(acme);

            assertThat(tools.deleteJobApplication("id-1")).isEqualTo("Candidatura removida: Acme - Java Developer");
        }
    }
}
