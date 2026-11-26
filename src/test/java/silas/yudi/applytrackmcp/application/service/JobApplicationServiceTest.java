package silas.yudi.applytrackmcp.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.domain.exception.JobApplicationNotFoundException;
import silas.yudi.applytrackmcp.domain.model.ApplicationProcess;
import silas.yudi.applytrackmcp.domain.model.ContentChange;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.JobApplicationChanges;
import silas.yudi.applytrackmcp.domain.model.JobPosting;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.StatusChange;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository repository;

    // 01:00 UTC do dia 26 ainda é dia 25 no Brasil
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-26T01:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private JobApplicationService service;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(repository, clock);
    }

    private final JobApplication acme = new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Full description", "https://acme.com/jobs/1", List.of()),
                ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
    private final JobApplication solucoes = new JobApplication(
                new JobPosting("Soluções Beta", null, "Pessoa Desenvolvedora Sênior", null, null, null, "Full description", null, List.of()),
                ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 20)));

    @Nested
    class Register {

        @Test
        void registersApplicationOnce() {
            service.register(acme);

            verify(repository).save(acme);
            verifyNoMoreInteractions(repository);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-11-24", "2026-11-25"})
        void registersApplicationFromYesterdayOrToday(LocalDate date) {
            var application = datedOn(date);

            service.register(application);

            verify(repository).save(application);
            verifyNoMoreInteractions(repository);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-11-26", "2099-01-01"})
        void doesNotRegisterApplicationWithFutureDate(LocalDate date) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.register(datedOn(date)))
                    .withMessage("A data da candidatura não pode ser futura: " + date);
            verifyNoMoreInteractions(repository);
        }

        @Test
        void doesNotRegisterApplicationWithFutureStatusDate() {
            var application = new JobApplication(
                    new JobPosting("Acme", null, "Java Developer", null, null, null, "Full description", null, List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20), "Entrevista marcada",
                            LocalDate.of(2026, 11, 26), null, null));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.register(application))
                    .withMessage("A data do status não pode ser futura: 2026-11-26");
            verifyNoMoreInteractions(repository);
        }

        // Hoje é 2026-11-25 pelo relógio fixo
        @Test
        void doesNotRegisterADoneStepInTheFuture() {
            var application = withSteps(new ProcessStep("Entrevista técnica", LocalDate.of(2026, 11, 26), true, null));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.register(application))
                    .withMessage("Etapa marcada como feita não pode ter data futura: Entrevista técnica (2026-11-26)");
            verifyNoMoreInteractions(repository);
        }

        @Test
        void registersDoneStepTodayAndPendingStepInTheFuture() {
            var application = withSteps(new ProcessStep("Fit cultural", LocalDate.of(2026, 11, 25), true, null),
                    new ProcessStep("Entrevista técnica", LocalDate.of(2026, 12, 3), false, null));

            service.register(application);

            verify(repository).save(application);
        }
    }

    @Nested
    class Search {

        // Em cada teste, o termo está em um único campo de uma candidatura; a acme não o tem em nenhum
        @ParameterizedTest
        @ValueSource(strings = {"Soluções Beta", "solucoes", "SOLUÇÕES", " beta "})
        void findsTermOnlyInCompanyIgnoringCaseAndAccents(String term) {
            var inCompany = new JobApplication(
                    new JobPosting("Soluções Beta", null, "Analista", null, null, null, "Vaga administrativa", null, List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            when(repository.findAll()).thenReturn(List.of(acme, inCompany));

            assertThat(service.search(term)).containsExactly(inCompany);
        }

        @ParameterizedTest
        @ValueSource(strings = {"senior", "SÊNIOR", "desenvolvedora", " Pessoa Desenvolvedora "})
        void findsTermOnlyInJobTitleIgnoringCaseAndAccents(String term) {
            var inTitle = new JobApplication(
                    new JobPosting("Gama", null, "Pessoa Desenvolvedora Sênior", null, null, null, "Vaga administrativa", null, List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            when(repository.findAll()).thenReturn(List.of(acme, inTitle));

            assertThat(service.search(term)).containsExactly(inTitle);
        }

        // Consultorias alocam em clientes: o cliente final também entra na busca
        @ParameterizedTest
        @ValueSource(strings = {"Banco Alfa", "banco", "ALFA"})
        void findsTermOnlyInFinalClient(String term) {
            var inFinalClient = new JobApplication(
                    new JobPosting("Gama", "Banco Alfa", "Analista", null, null, null, "Vaga administrativa", null, List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            when(repository.findAll()).thenReturn(List.of(acme, inFinalClient));

            assertThat(service.search(term)).containsExactly(inFinalClient);
        }

        // O título pode ser genérico, e a stack aparecer só na descrição
        @ParameterizedTest
        @ValueSource(strings = {"Kafka", "kafka", "microsservicos", "MICROSSERVIÇOS", " Spring Boot "})
        void findsTermOnlyInDescriptionIgnoringCaseAndAccents(String term) {
            var inDescription = new JobApplication(
                    new JobPosting("Gama", null, "Desenvolvedor de Software Sênior", null, null, null, "Requisitos:\n- Java com Spring Boot\n- Microsserviços e Kafka", null, List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            when(repository.findAll()).thenReturn(List.of(acme, inDescription));

            assertThat(service.search(term)).containsExactly(inDescription);
        }

        @Test
        void returnsOnceAnApplicationThatMatchesInEveryField() {
            var everywhere = new JobApplication(
                    new JobPosting("Java House", null, "Java Developer", null, null, null, "Java 25", null, List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            when(repository.findAll()).thenReturn(List.of(everywhere, solucoes));

            assertThat(service.search("java")).containsExactly(everywhere);
        }

        @Test
        void returnsOnceAnApplicationThatRepeatsTheTermInTheSameField() {
            var repeated = new JobApplication(
                    new JobPosting("Gama", null, "Analista", null, null, null, "Kafka, Kafka Streams e Kafka Connect", null, List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            when(repository.findAll()).thenReturn(List.of(repeated, acme));

            assertThat(service.search("kafka")).containsExactly(repeated);
        }

        @ParameterizedTest
        @ValueSource(strings = {"Gupy", "https", "2026"})
        void doesNotSearchByOtherFields(String term) {
            when(repository.findAll()).thenReturn(List.of(acme));

            assertThat(service.search(term)).isEmpty();
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

        @Test
        void sortsByMostRecent() {
            var older = datedOn(LocalDate.of(2026, 10, 31));
            var newer = datedOn(LocalDate.of(2026, 11, 1));
            when(repository.findAll()).thenReturn(List.of(older, newer));

            assertThat(service.search("acme")).containsExactly(newer, older);
        }
    }

    @Nested
    class Get {

        @Test
        void getsApplicationById() {
            when(repository.findById(acme.id())).thenReturn(Optional.of(acme));

            assertThat(service.get(acme.id())).isEqualTo(acme);
        }

        @Test
        void reportsUnknownIdWhenGetting() {
            when(repository.findById("unknown")).thenReturn(Optional.empty());

            assertThatExceptionOfType(JobApplicationNotFoundException.class)
                    .isThrownBy(() -> service.get("unknown"))
                    .withMessage("Candidatura não encontrada: unknown");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void getRequiresId(String id) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.get(id))
                    .withMessage("Campo obrigatório: id");
            verifyNoInteractions(repository);
        }
    }

    @Nested
    class Update {

        @Test
        void updatesAndSavesApplication() {
            var expected = new JobApplication(acme.id(),
                    new JobPosting("Acme", null, "Analyst", null, null, null, "Full description", "https://acme.com/jobs/1", List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            when(repository.findById(acme.id())).thenReturn(Optional.of(acme));

            assertThat(service.update(acme.id(), titleChange("Analyst"))).isEqualTo(expected);
            verify(repository).save(expected);
            verifyNoMoreInteractions(repository);
        }

        @Test
        void doesNotSaveWhenApplicationDoesNotExist() {
            when(repository.findById("unknown")).thenReturn(Optional.empty());

            assertThatExceptionOfType(JobApplicationNotFoundException.class)
                    .isThrownBy(() -> service.update("unknown", titleChange("Analyst")))
                    .withMessage("Candidatura não encontrada: unknown");
            verifyNoMoreInteractions(repository);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void updateRequiresId(String id) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.update(id, titleChange("Analyst")))
                    .withMessage("Campo obrigatório: id");
            verifyNoMoreInteractions(repository);
        }

        @Test
        void doesNotSaveInvalidChange() {
            when(repository.findById(acme.id())).thenReturn(Optional.of(acme));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.update(acme.id(), titleChange(" ")))
                    .withMessage("Campo obrigatório: jobTitle");
            verifyNoMoreInteractions(repository);
        }

        @Test
        void doesNotSaveWhenThereAreNoChanges() {
            when(repository.findById(acme.id())).thenReturn(Optional.of(acme));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.update(
                            acme.id(), new JobApplicationChanges(null, null, null, null, null, null, null, null, null, null, null, null)))
                    .withMessage("Informe ao menos um campo para alterar");
            verifyNoMoreInteractions(repository);
        }

        @Test
        void updatesAllFieldsAndSaves() {
            var expected = new JobApplication(acme.id(),
                    new JobPosting("Beta", null, "Analyst", null, null, null, "New description", "https://beta.com", List.of()),
                    ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 21)));
            when(repository.findById(acme.id())).thenReturn(Optional.of(acme));

            assertThat(service.update(acme.id(), new JobApplicationChanges("Beta", null, "Analyst", null, null, null, "WhatsApp", "https://beta.com", LocalDate.of(2026, 11, 21), new ContentChange("New description", List.of()), null, null))).isEqualTo(expected);
            verify(repository).save(expected);
            verifyNoMoreInteractions(repository);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-11-24", "2026-11-25"})
        void updatesDateToYesterdayOrToday(LocalDate date) {
            var expected = new JobApplication(acme.id(),
                    new JobPosting("Acme", null, "Java Developer", null, null, null, "Full description", "https://acme.com/jobs/1", List.of()),
                    ApplicationProcess.start("Gupy", date));
            when(repository.findById(acme.id())).thenReturn(Optional.of(acme));

            assertThat(service.update(acme.id(), dateChange(date))).isEqualTo(expected);
            verify(repository).save(expected);
            verifyNoMoreInteractions(repository);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-11-26", "2099-01-01"})
        void doesNotUpdateToFutureDate(LocalDate date) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.update(acme.id(), dateChange(date)))
                    .withMessage("A data da candidatura não pode ser futura: " + date);
            verify(repository, never()).save(any());
        }

        // Um registro que já esteja com data errada não pode impedir a correção dos outros campos
        @Test
        void updatesOtherFieldsOfApplicationThatAlreadyHasFutureDate() {
            var wrong = datedOn(LocalDate.of(2099, 1, 1));
            var expected = new JobApplication(wrong.id(),
                    new JobPosting("Acme", null, "Analyst", null, null, null, "Full description", null, List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2099, 1, 1)));
            when(repository.findById(wrong.id())).thenReturn(Optional.of(wrong));

            assertThat(service.update(wrong.id(), titleChange("Analyst"))).isEqualTo(expected);
            verify(repository).save(expected);
        }

        @Test
        void doesNotUpdateToFutureStatusDate() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.update(acme.id(), new JobApplicationChanges(null, null, null, null, null, null, null, null, null, null, new StatusChange("Entrevista marcada", LocalDate.of(2026, 11, 26), null), null)))
                    .withMessage("A data do status não pode ser futura: 2026-11-26");
            verify(repository, never()).save(any());
        }

        // O relógio fixo marca 01:00 UTC do dia 26, que ainda é dia 25 no Brasil
        @Test
        void usesTodayInBrazilWhenStatusChanges() {
            when(repository.findById(acme.id())).thenReturn(Optional.of(acme));

            var updated = service.update(acme.id(), new JobApplicationChanges(null, null, null, null, null, null, null, null, null, null, new StatusChange("Entrevista marcada", null, null), null));

            assertThat(updated.process().statusUpdatedOn()).isEqualTo(LocalDate.of(2026, 11, 25));
            verify(repository).save(updated);
        }

        @Test
        void doesNotUpdateWithADoneStepInTheFuture() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.update(acme.id(), new JobApplicationChanges(null, null, null, null, null, null, null, null, null, null, null, List.of(new ProcessStep("Entrevista técnica", LocalDate.of(2026, 11, 26), true, null)))))
                    .withMessage("Etapa marcada como feita não pode ter data futura: Entrevista técnica (2026-11-26)");
            verify(repository, never()).save(any());
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesAndReturnsApplication() {
            when(repository.findById(acme.id())).thenReturn(Optional.of(acme));

            assertThat(service.delete(acme.id())).isEqualTo(acme);
            verify(repository).deleteById(acme.id());
            verifyNoMoreInteractions(repository);
        }

        @Test
        void doesNotDeleteWhenApplicationDoesNotExist() {
            when(repository.findById("unknown")).thenReturn(Optional.empty());

            assertThatExceptionOfType(JobApplicationNotFoundException.class)
                    .isThrownBy(() -> service.delete("unknown"))
                    .withMessage("Candidatura não encontrada: unknown");
            verifyNoMoreInteractions(repository);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void deleteRequiresId(String id) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.delete(id))
                    .withMessage("Campo obrigatório: id");
            verifyNoInteractions(repository);
        }
    }

    private static JobApplicationChanges dateChange(LocalDate date) {
        return new JobApplicationChanges(null, null, null, null, null, null, null, null, date, null, null, null);
    }

    private static JobApplication withSteps(ProcessStep... steps) {
        return new JobApplication(new JobPosting("Acme", null, "Java Developer", null, null, null, "Full description", null, List.of()),
                ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20), null, null, null, List.of(steps)));
    }

    private static JobApplicationChanges titleChange(String jobTitle) {
        return new JobApplicationChanges(null, null, jobTitle, null, null, null, null, null, null, null, null, null);
    }

    private static JobApplication datedOn(LocalDate date) {
        return new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Full description", null, List.of()),
                ApplicationProcess.start("Gupy", date));
    }
}
