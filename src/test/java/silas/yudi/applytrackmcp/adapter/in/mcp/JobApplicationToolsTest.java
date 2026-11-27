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
import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.Requirement;
import silas.yudi.applytrackmcp.domain.model.Situation;
import silas.yudi.applytrackmcp.domain.model.StepChange;
import silas.yudi.applytrackmcp.domain.model.StepState;
import silas.yudi.applytrackmcp.domain.model.WaitingFor;
import silas.yudi.applytrackmcp.domain.model.WorkModel;

import java.time.LocalDate;
import java.util.List;

import static silas.yudi.applytrackmcp.support.JobPostingBuilder.aPosting;
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
            aPosting().link("https://acme.com/jobs/1").description("Full description").build(),
            ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20))
    );

    @Nested
    class Register {

        @Test
        void registersApplicationAndConfirms() {
            var message = tools.registerJobApplication(acme.posting().company(), null, acme.posting().jobTitle(), null, null, null, null, null, acme.posting().link(), acme.posting().description(), null, acme.process().platform(), "2026-11-20", null, null, null);

            var registered = ArgumentCaptor.forClass(JobApplication.class);
            verify(service).register(registered.capture());
            assertThat(registered.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(acme);
            assertThat(message).isEqualTo("Candidatura registrada: Acme - Java Developer (id: " + registered.getValue().id() + ")");
        }

        @Test
        void registersPostingFields() {
            tools.registerJobApplication("Acme", "Banco Alfa", "Java Developer", "Sênior", "PJ", "R$ 15.000", WorkModel.HYBRID, "São Paulo, SP", null, "Description", null, "Gupy", "2026-11-20", null, null, null);

            var registered = ArgumentCaptor.forClass(JobApplication.class);
            verify(service).register(registered.capture());
            assertThat(registered.getValue().posting()).isEqualTo(aPosting()
                    .finalClient("Banco Alfa")
                    .seniority("Sênior")
                    .contractType("PJ")
                    .salaryRange("R$ 15.000")
                    .workModel(WorkModel.HYBRID)
                    .location("São Paulo, SP")
                    .build());
        }

        @Test
        void registersRequirements() {
            var requirements = List.of(new Requirement("Java", "Avançado", Importance.REQUIRED),
                    new Requirement("Inglês", "C1", Importance.DESIRABLE));

            tools.registerJobApplication("Acme", null, "Java Developer", null, null, null, null, null, null, "Description", List.of(new RequirementInput(" Java ", "Avançado", Importance.REQUIRED),
                            new RequirementInput("Inglês", "C1", Importance.DESIRABLE)), "Gupy", "2026-11-20", null, null, null);

            var registered = ArgumentCaptor.forClass(JobApplication.class);
            verify(service).register(registered.capture());
            assertThat(registered.getValue().posting().requirements()).isEqualTo(requirements);
        }

        @Test
        void registersWithDefaultProcessWhenProcessFieldsAreMissing() {
            tools.registerJobApplication("Acme", null, "Java Developer", null, null, null, null, null, null, "Description", null, "Gupy", "2026-11-20", null, null, null);

            var registered = ArgumentCaptor.forClass(JobApplication.class);
            verify(service).register(registered.capture());
            assertThat(registered.getValue().process()).isEqualTo(ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
        }

        @Test
        void registersProcessFields() {
            tools.registerJobApplication("Acme", null, "Java Developer", null, null, null, null, null, null, "Description", null, "Gupy", "2026-11-20", Situation.ACTIVE, "Indicação do João", List.of(
                            new ProcessStepInput("Triagem", StepState.PASSED, null, "2026-11-21", null, null),
                            new ProcessStepInput("Entrevista técnica", StepState.AWAITING_FEEDBACK, "2026-11-24",
                                    "2026-11-24", "2026-12-01", "Perguntaram sobre Kafka"),
                            new ProcessStepInput("Entrevista com líder", StepState.SCHEDULED, null, null, null, null)));

            var registered = ArgumentCaptor.forClass(JobApplication.class);
            verify(service).register(registered.capture());
            assertThat(registered.getValue().process()).isEqualTo(new ApplicationProcess("Gupy",
                    LocalDate.of(2026, 11, 20), Situation.ACTIVE, "Indicação do João", List.of(
                    new ProcessStep("Triagem", StepState.PASSED, null, LocalDate.of(2026, 11, 21), null, null),
                    new ProcessStep("Entrevista técnica", StepState.AWAITING_FEEDBACK, LocalDate.of(2026, 11, 24),
                            LocalDate.of(2026, 11, 24), LocalDate.of(2026, 12, 1), "Perguntaram sobre Kafka"),
                    new ProcessStep("Entrevista com líder", StepState.SCHEDULED, null, null, null, null))));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void treatsBlankStepDatesAsNoDate(String blank) {
            tools.registerJobApplication("Acme", null, "Java Developer", null, null, null, null, null, null, "Description", null, "Gupy", "2026-11-20", null, null, List.of(new ProcessStepInput("Entrevista técnica", StepState.SCHEDULED, blank, blank, blank, null)));

            var registered = ArgumentCaptor.forClass(JobApplication.class);
            verify(service).register(registered.capture());
            assertThat(registered.getValue().process().steps()).containsExactly(
                    new ProcessStep("Entrevista técnica", StepState.SCHEDULED, null, null, null, null));
        }

        @Test
        void doesNotRegisterInvalidApplication() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.registerJobApplication(" ", null, "Java Developer", null, null, null, null, null, null, "Description", null, "Gupy", "2026-11-20", null, null, null))
                    .withMessage("Campo obrigatório: company");
            verifyNoInteractions(service);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void requiresDateAndRegistersNothing(String appliedOn) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.registerJobApplication("Acme", null, "Java Developer", null, null, null, null, null, null, "Description", null, "Gupy", appliedOn, null, null, null))
                    .withMessage("Campo obrigatório: appliedOn");
            verifyNoInteractions(service);
        }

        @ParameterizedTest
        @ValueSource(strings = {"20/11/2026", "2026-02-30", "ontem"})
        void rejectsInvalidDateAndRegistersNothing(String appliedOn) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.registerJobApplication("Acme", null, "Java Developer", null, null, null, null, null, null, "Description", null, "Gupy", appliedOn, null, null, null))
                    .withMessage("Data inválida: " + appliedOn + ". Use o formato AAAA-MM-DD");
            verifyNoInteractions(service);
        }

        @ParameterizedTest
        @ValueSource(strings = {"24/11/2026", "2026-02-30"})
        void rejectsInvalidStepDateAndRegistersNothing(String date) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.registerJobApplication("Acme", null, "Java Developer", null, null, null, null, null, null, "Description", null, "Gupy", "2026-11-20", null, null, List.of(new ProcessStepInput("Técnica", StepState.AWAITING_FEEDBACK, null, date, null, null))))
                    .withMessage("Data inválida: " + date + ". Use o formato AAAA-MM-DD");
            verifyNoInteractions(service);
        }

    }

    @Nested
    class Search {

        @Test
        void returnsSummaryOfAllApplicationsFound() {
            var acmeLabs = new JobApplication(
                    aPosting()
                            .company("Acme Labs")
                            .jobTitle("Analyst")
                            .description("Another description")
                            .build(),
                    ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 20)));
            when(service.search("acme")).thenReturn(List.of(acme, acmeLabs));

            assertThat(tools.searchJobApplications("acme")).containsExactly(
                    new JobApplicationSummary(acme.id(), "Acme", null, "Java Developer", null, null, null, null, "https://acme.com/jobs/1", List.of(), "Gupy", LocalDate.of(2026, 11, 20), Situation.ACTIVE, "Aguardando contato após a candidatura", WaitingFor.COMPANY, LocalDate.of(2026, 11, 20)),
                    new JobApplicationSummary(acmeLabs.id(), "Acme Labs", null, "Analyst", null, null, null, null, null, List.of(), "WhatsApp", LocalDate.of(2026, 11, 20), Situation.ACTIVE, "Aguardando contato após a candidatura", WaitingFor.COMPANY, LocalDate.of(2026, 11, 20)));
        }

        @Test
        void summaryIncludesPostingFieldsButNotSalary() {
            var posted = new JobApplication(acme.id(), aPosting()
                    .finalClient("Banco Alfa")
                    .seniority("Sênior")
                    .contractType("PJ")
                    .salaryRange("R$ 15.000")
                    .workModel(WorkModel.REMOTE)
                    .location("Brasil")
                    .description("Full description")
                    .build(),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            when(service.search("acme")).thenReturn(List.of(posted));

            assertThat(tools.searchJobApplications("acme")).containsExactly(new JobApplicationSummary(acme.id(), "Acme", "Banco Alfa", "Java Developer", "Sênior", "PJ", WorkModel.REMOTE, "Brasil", null, List.of(), "Gupy", LocalDate.of(2026, 11, 20), Situation.ACTIVE, "Aguardando contato após a candidatura", WaitingFor.COMPANY, LocalDate.of(2026, 11, 20)));
        }

        @Test
        void summaryIncludesRequirements() {
            var requirements = List.of(new Requirement("Java", "Avançado", Importance.REQUIRED));
            var withRequirements = new JobApplication(acme.id(),
                    aPosting().description("Full description").requirements(requirements).build(),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            when(service.search("acme")).thenReturn(List.of(withRequirements));

            assertThat(tools.searchJobApplications("acme")).containsExactly(new JobApplicationSummary(acme.id(), "Acme", null, "Java Developer", null, null, null, null, null, requirements, "Gupy", LocalDate.of(2026, 11, 20), Situation.ACTIVE, "Aguardando contato após a candidatura", WaitingFor.COMPANY, LocalDate.of(2026, 11, 20)));
        }
    }

    @Nested
    class Get {

        @Test
        void returnsCompleteApplicationWithCalculatedProcessFields() {
            when(service.get("id-1")).thenReturn(acme);

            assertThat(tools.getJobApplication("id-1")).isEqualTo(new JobApplicationDetail(acme.id(), acme.posting(),
                    new JobApplicationDetail.Process("Gupy", LocalDate.of(2026, 11, 20), Situation.ACTIVE, null,
                            "Aguardando contato após a candidatura", WaitingFor.COMPANY, LocalDate.of(2026, 11, 20),
                            List.of())));
        }
    }

    @Nested
    class Update {

        @Test
        void updatesApplicationAndConfirms() {
            var updated = new JobApplication(acme.id(),
                    aPosting()
                            .company("Beta")
                            .jobTitle("Analyst")
                            .link("https://beta.com")
                            .description("New description")
                            .build(),
                    ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 21)));
            when(service.update("id-1", JobApplicationChanges.builder()
                    .company("Beta")
                    .jobTitle("Analyst")
                    .link("https://beta.com")
                    .content(new ContentChange("New description", List.of()))
                    .platform("WhatsApp")
                    .appliedOn(LocalDate.of(2026, 11, 21))
                    .build())).thenReturn(updated);

            assertThat(tools.updateJobApplication("id-1", "Beta", null, "Analyst", null, null, null, null, null, "https://beta.com", "New description", List.of(), "WhatsApp", "2026-11-21", null, null, null))
                    .isEqualTo("Candidatura atualizada: Beta - Analyst (id: " + acme.id() + ")");
        }

        @Test
        void sendsOnlyTheGivenFields() {
            when(service.update("id-1", JobApplicationChanges.builder().link("").platform("Gupy").build())).thenReturn(acme);

            assertThat(tools.updateJobApplication("id-1", null, null, null, null, null, null, null, null, "", null, null, "Gupy", null, null, null, null))
                    .isEqualTo("Candidatura atualizada: Acme - Java Developer (id: " + acme.id() + ")");
        }

        @Test
        void sendsPostingFieldsToUpdate() {
            when(service.update("id-1", JobApplicationChanges.builder()
                    .finalClient("Banco Alfa")
                    .seniority("Sênior")
                    .contractType("PJ")
                    .salaryRange("")
                    .workModel(WorkModel.ON_SITE)
                    .location("")
                    .build())).thenReturn(acme);

            assertThat(tools.updateJobApplication("id-1", null, "Banco Alfa", null, "Sênior", "PJ", "", WorkModel.ON_SITE, "", null, null, null, null, null, null, null, null)).isEqualTo("Candidatura atualizada: Acme - Java Developer (id: " + acme.id() + ")");
        }

        @Test
        void sendsRequirementsToUpdate() {
            var requirements = List.of(new Requirement("Kafka", null, Importance.DESIRABLE));
            when(service.update("id-1", JobApplicationChanges.builder()
                    .content(new ContentChange(null, requirements))
                    .build()))
                    .thenReturn(acme);

            assertThat(tools.updateJobApplication("id-1", null, null, null, null, null, null, null, null, null, null, List.of(new RequirementInput("Kafka", " ", Importance.DESIRABLE)), null, null, null, null, null))
                    .isEqualTo("Candidatura atualizada: Acme - Java Developer (id: " + acme.id() + ")");
        }

        @Test
        void sendsProcessFieldsToUpdate() {
            when(service.update("id-1", JobApplicationChanges.builder()
                    .situation(Situation.REJECTED)
                    .notes("Reprovado na técnica")
                    .steps(List.of(new ProcessStep("Entrevista técnica", StepState.FAILED, null,
                            LocalDate.of(2026, 11, 23), null, null)))
                    .build()))
                    .thenReturn(acme);

            assertThat(tools.updateJobApplication("id-1", null, null, null, null, null, null, null, null, null, null, null, null, null, Situation.REJECTED, "Reprovado na técnica", List.of(new ProcessStepInput("Entrevista técnica", StepState.FAILED, null, "2026-11-23", null, null))))
                    .isEqualTo("Candidatura atualizada: Acme - Java Developer (id: " + acme.id() + ")");
        }

        @Test
        void rejectsUpdateOfDescriptionWithoutRequirements() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.updateJobApplication("id-1", null, null, null, null, null, null, null, null, null, "Nova descrição", null, null, null, null, null, null))
                    .withMessage("Para informar a descrição, envie também os requisitos (lista vazia se a vaga não citar nenhum)");
            verifyNoInteractions(service);
        }

        @Test
        void rejectsInvalidDateAndUpdatesNothing() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.updateJobApplication("id-1", null, null, null, null, null, null, null, null, null, null, null, null, "20/11/2026", null, null, null))
                    .withMessage("Data inválida: 20/11/2026. Use o formato AAAA-MM-DD");
            verifyNoInteractions(service);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejectsBlankDateAndUpdatesNothing(String appliedOn) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.updateJobApplication("id-1", "Beta", null, null, null, null, null, null, null, null, null, null, null, appliedOn, null, null, null))
                    .withMessage("Campo obrigatório: appliedOn");
            verifyNoInteractions(service);
        }


    }

    @Nested
    class AddStep {

        @Test
        void addsTheStepAndConfirmsWithTheNewStatus() {
            var interview = new ProcessStep("Entrevista técnica", StepState.SCHEDULED, LocalDate.of(2026, 12, 3), null,
                    null, "Com o tech lead");
            when(service.addStep("id-1", interview)).thenReturn(acme.addStep(interview));

            assertThat(tools.addJobApplicationStep("id-1", "Entrevista técnica", StepState.SCHEDULED, "2026-12-03",
                    null, null, "Com o tech lead"))
                    .isEqualTo("Etapa incluída: Acme - Java Developer. Status: Próxima etapa: Entrevista técnica em "
                            + "2026-12-03 (id: " + acme.id() + ")");
        }

        @Test
        void rejectsInvalidDateAndAddsNothing() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.addJobApplicationStep("id-1", "Entrevista técnica", StepState.SCHEDULED,
                            "03/12/2026", null, null, null))
                    .withMessage("Data inválida: 03/12/2026. Use o formato AAAA-MM-DD");
            verifyNoInteractions(service);
        }
    }

    @Nested
    class UpdateStep {

        @Test
        void sendsOnlyTheGivenFieldsAndConfirmsWithTheNewStatus() {
            var interview = new ProcessStep("Entrevista técnica", StepState.SCHEDULED, LocalDate.of(2026, 11, 25), null,
                    null, null);
            var change = new StepChange(StepState.AWAITING_FEEDBACK, null, LocalDate.of(2026, 11, 25),
                    LocalDate.of(2026, 12, 1), null);
            when(service.updateStep("id-1", "Entrevista técnica", change))
                    .thenReturn(acme.addStep(interview).updateStep("Entrevista técnica", change));

            assertThat(tools.updateJobApplicationStep("id-1", "Entrevista técnica", StepState.AWAITING_FEEDBACK, null,
                    "2026-11-25", "2026-12-01", null))
                    .isEqualTo("Etapa atualizada: Acme - Java Developer. Status: Aguardando feedback de Entrevista "
                            + "técnica (prometido até 2026-12-01) (id: " + acme.id() + ")");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejectsBlankDateAndUpdatesNothing(String blank) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> tools.updateJobApplicationStep("id-1", "Entrevista técnica", null, null, blank,
                            null, null))
                    .withMessage("Campo obrigatório: completedOn");
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
