package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import silas.yudi.applytrackmcp.application.port.out.StorageUnavailableException;
import silas.yudi.applytrackmcp.domain.model.ApplicationProcess;
import silas.yudi.applytrackmcp.domain.model.Importance;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.Requirement;
import silas.yudi.applytrackmcp.domain.model.Situation;
import silas.yudi.applytrackmcp.domain.model.StepState;
import silas.yudi.applytrackmcp.domain.model.WorkModel;
import silas.yudi.applytrackmcp.support.DynamoDbLocalContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static silas.yudi.applytrackmcp.support.JobPostingBuilder.aPosting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Roda contra o DynamoDB Local no Docker.
 */
class DynamoDbJobApplicationRepositoryIT {

    private final JobApplication acme = new JobApplication(
            aPosting().link("https://acme.com/jobs/1").description("Acme description").build(),
            ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
    private final JobApplication beta = new JobApplication(
            aPosting()
                    .company("Soluções Beta")
                    .jobTitle("Pessoa Desenvolvedora Sênior")
                    .description("Descrição com\nduas linhas e 🚀")
                    .build(),
            ApplicationProcess.start("WhatsApp", LocalDate.of(2026, 11, 20)));

    private DynamoDbClient client;
    private DynamoDbTable<JobApplicationItem> table;
    private DynamoDbJobApplicationRepository repository;

    // Uma tabela nova por teste: cada um começa vazio
    @BeforeEach
    void createTable() {
        client = DynamoDbClient.builder()
                .endpointOverride(URI.create(DynamoDbLocalContainer.endpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")))
                .build();
        table = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build()
                .table("test-" + UUID.randomUUID(), TableSchema.fromBean(JobApplicationItem.class));
        table.createTable();
        repository = new DynamoDbJobApplicationRepository(table, false, DynamoDbLocalContainer.endpoint());
    }

    @AfterEach
    void deleteTable() {
        table.deleteTable();
        client.close();
    }

    @Nested
    class Save {

        @Test
        void savesAndReadsAllFields() {
            repository.save(acme);

            assertThat(repository.findAll()).containsExactly(acme);
        }

        @Test
        void savesAndReadsPostingFields() {
            var application = new JobApplication(
                    aPosting()
                            .finalClient("Banco Alfa")
                            .seniority("Sênior")
                            .contractType("PJ")
                            .salaryRange("R$ 15.000 a R$ 18.000")
                            .workModel(WorkModel.HYBRID)
                            .location("São Paulo, SP")
                            .build(),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            repository.save(application);

            assertThat(repository.findById(application.id())).contains(application);
        }

        @Test
        void savesAndReadsRequirementsInOrderWithEveryImportanceAndMissingLevel() {
            var withRequirements = new JobApplication(
                    aPosting()
                            .requirements(List.of(
                                new Requirement("Java", "Avançado", Importance.REQUIRED),
                                new Requirement("Inglês", "C1", Importance.DESIRABLE),
                                new Requirement("Node.js", null, Importance.UNSPECIFIED)))
                            .build(),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            repository.save(withRequirements);

            assertThat(repository.findById(withRequirements.id())).contains(withRequirements);
        }

        @Test
        void savesAndReadsTheWholeProcess() {
            var application = new JobApplication(
                    aPosting().build(),
                    new ApplicationProcess("Gupy", LocalDate.of(2026, 11, 20), Situation.OFFER, "Indicação do João",
                            List.of(
                                    new ProcessStep("Triagem", StepState.PASSED, null, LocalDate.of(2026, 11, 21), null,
                                            null),
                                    new ProcessStep("Entrevista técnica", StepState.AWAITING_FEEDBACK,
                                            LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 24),
                                            LocalDate.of(2026, 12, 1), "Kafka"),
                                    new ProcessStep("Entrevista com líder", StepState.SCHEDULED, null, null, null,
                                            null))));
            repository.save(application);

            assertThat(repository.findById(application.id())).contains(application);
        }

        @Test
        void keepsAccentsLineBreaksEmojiAndMissingLink() {
            repository.save(beta);

            assertThat(repository.findAll()).containsExactly(beta);
        }

        @Test
        void storesPostingAndProcessAsMapsAndDateAsIsoText() {
            repository.save(acme);

            var item = client.scan(request -> request.tableName(table.tableName())).items().getFirst();
            assertThat(item).containsOnlyKeys("id", "posting", "process");
            assertThat(item.get("posting").m()).containsEntry("company", AttributeValue.fromS("Acme"));
            assertThat(item.get("process").m()).containsEntry("appliedOn", AttributeValue.fromS("2026-11-20"));
        }

        @Test
        void doesNotOverwriteAnEqualApplicationWithAnotherId() {
            var sameData = new JobApplication(
                    aPosting().link("https://acme.com/jobs/1").description("Acme description").build(),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            repository.save(acme);
            repository.save(sameData);

            assertThat(repository.findAll()).containsExactlyInAnyOrder(acme, sameData);
        }

        @Test
        void replacesApplicationWithTheSameIdAndRemovesTheLink() {
            var changed = new JobApplication(acme.id(),
                    aPosting().jobTitle("Analyst").description("New description").build(),
                    ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 21)));
            repository.save(acme);
            repository.save(beta);
            repository.save(changed);

            assertThat(repository.findAll()).containsExactlyInAnyOrder(changed, beta);
        }
    }

    @Nested
    class FindAll {

        @Test
        void findsNothingInAnEmptyTable() {
            assertThat(repository.findAll()).isEmpty();
        }

        @Test
        void readsAllSavedApplications() {
            repository.save(acme);
            repository.save(beta);

            assertThat(repository.findAll()).containsExactlyInAnyOrder(acme, beta);
        }

        // Cada resposta do scan traz até 1 MB; aqui são cerca de 1,5 MB
        @Test
        void readsMoreThanOnePage() {
            var large = IntStream.range(0, 5)
                    .mapToObj(i -> new JobApplication(
                            aPosting().company("Company " + i).description("x".repeat(300_000)).build(),
                            ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20))))
                    .toList();
            large.forEach(repository::save);

            assertThat(table.scan().stream().count()).isGreaterThan(1);
            assertThat(repository.findAll()).containsExactlyInAnyOrderElementsOf(large);
        }

        // Item gravado sem as listas (requisitos e etapas)
        @Test
        void readsItemSavedWithoutLists() {
            client.putItem(request -> request.tableName(table.tableName()).item(Map.of(
                    "id", AttributeValue.fromS("1"),
                    "posting", AttributeValue.fromM(Map.of(
                            "company", AttributeValue.fromS("Acme"),
                            "jobTitle", AttributeValue.fromS("Java Developer"),
                            "description", AttributeValue.fromS("Description"))),
                    "process", AttributeValue.fromM(Map.of(
                            "platform", AttributeValue.fromS("Gupy"),
                            "appliedOn", AttributeValue.fromS("2026-11-20"),
                            "situation", AttributeValue.fromS("ACTIVE"))))));

            assertThat(repository.findAll()).containsExactly(new JobApplication("1",
                    aPosting().build(),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20))));
        }
    }

    @Nested
    class FindById {

        @Test
        void findsOnlyTheApplicationWithTheId() {
            repository.save(acme);
            repository.save(beta);

            assertThat(repository.findById(beta.id())).contains(beta);
        }

        @Test
        void findsNothingWithUnknownId() {
            repository.save(acme);

            assertThat(repository.findById("unknown")).isEmpty();
        }
    }

    @Nested
    class DeleteById {

        @Test
        void deletesOnlyTheApplicationWithTheId() {
            repository.save(acme);
            repository.save(beta);

            repository.deleteById(acme.id());

            assertThat(repository.findAll()).containsExactly(beta);
            assertThat(repository.findById(acme.id())).isEmpty();
        }

        @Test
        void deletingUnknownIdKeepsEverything() {
            repository.save(acme);

            repository.deleteById("unknown");

            assertThat(repository.findAll()).containsExactly(acme);
        }
    }

    @Nested
    class TableCreation {

        // O servidor sobe mesmo com o banco fora: a tabela só é criada quando é usada
        @Test
        void createsTheMissingTableOnFirstUse() {
            var missing = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build()
                    .table("test-" + UUID.randomUUID(), TableSchema.fromBean(JobApplicationItem.class));
            var creating = new DynamoDbJobApplicationRepository(missing, true, DynamoDbLocalContainer.endpoint());

            creating.save(acme);

            assertThat(creating.findAll()).containsExactly(acme);
            missing.deleteTable();
        }

        @Test
        void usesTheTableThatAlreadyExists() {
            repository.save(acme);
            var creating = new DynamoDbJobApplicationRepository(table, true, DynamoDbLocalContainer.endpoint());

            assertThat(creating.findAll()).containsExactly(acme);
        }
    }

    // A mensagem chega à IA como resposta da tool: precisa dizer o que fazer
    @Nested
    class UnavailableDatabase {

        private static final String CLOSED = "http://localhost:1";
        private static final String LOCAL_HINT = "Banco de dados indisponível em http://localhost:1. Confira se o Docker "
                + "está aberto e se o contêiner do DynamoDB está rodando (docker compose up -d na pasta do compose.yaml)";

        private DynamoDbClient closedClient;

        @BeforeEach
        void connectToClosedPort() {
            closedClient = DynamoDbClient.builder()
                    .endpointOverride(URI.create(CLOSED))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")))
                    .build();
        }

        @AfterEach
        void close() {
            closedClient.close();
        }

        @Test
        void everyOperationExplainsHowToStartTheLocalDatabase() {
            var unavailable = new DynamoDbJobApplicationRepository(closedTable(), false, CLOSED);

            assertUnavailable(() -> unavailable.save(acme), LOCAL_HINT);
            assertUnavailable(() -> unavailable.findById("id-1"), LOCAL_HINT);
            assertUnavailable(unavailable::findAll, LOCAL_HINT);
            assertUnavailable(() -> unavailable.deleteById("id-1"), LOCAL_HINT);
        }

        @Test
        void creatingTheTableAlsoExplains() {
            var unavailable = new DynamoDbJobApplicationRepository(closedTable(), true, CLOSED);

            assertUnavailable(unavailable::findAll, LOCAL_HINT);
        }

        // Sem endpoint é o DynamoDB da AWS: Docker não faz sentido
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void withoutEndpointDoesNotMentionDocker(String endpoint) {
            var unavailable = new DynamoDbJobApplicationRepository(closedTable(), false, endpoint);

            assertUnavailable(unavailable::findAll, "Banco de dados indisponível. Tente novamente em instantes");
        }

        private DynamoDbTable<JobApplicationItem> closedTable() {
            return DynamoDbEnhancedClient.builder().dynamoDbClient(closedClient).build()
                    .table("job-applications", TableSchema.fromBean(JobApplicationItem.class));
        }

        private static void assertUnavailable(ThrowingCallable operation, String message) {
            assertThatExceptionOfType(StorageUnavailableException.class)
                    .isThrownBy(operation)
                    .withMessage(message);
        }
    }
}
