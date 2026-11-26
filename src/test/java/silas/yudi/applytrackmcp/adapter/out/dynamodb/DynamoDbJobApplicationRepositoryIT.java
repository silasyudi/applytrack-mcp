package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import silas.yudi.applytrackmcp.domain.model.ApplicationProcess;
import silas.yudi.applytrackmcp.domain.model.Importance;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.JobPosting;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.Requirement;
import silas.yudi.applytrackmcp.domain.model.Situation;
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

import static org.assertj.core.api.Assertions.assertThat;

/** Roda contra o DynamoDB Local no Docker. */
class DynamoDbJobApplicationRepositoryIT {

    private final JobApplication acme = new JobApplication(
                new JobPosting("Acme", null, "Java Developer", null, null, null, "Acme description", "https://acme.com/jobs/1", List.of()),
                ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
    private final JobApplication beta = new JobApplication(
                new JobPosting("Soluções Beta", null, "Pessoa Desenvolvedora Sênior", null, null, null, "Descrição com\nduas linhas e 🚀", null, List.of()),
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
        repository = new DynamoDbJobApplicationRepository(table);
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
        void keepsAccentsLineBreaksEmojiAndMissingLink() {
            repository.save(beta);

            assertThat(repository.findAll()).containsExactly(beta);
        }

        @Test
        void doesNotOverwriteAnEqualApplicationWithAnotherId() {
            var sameData = new JobApplication(
                    new JobPosting("Acme", null, "Java Developer", null, null, null, "Acme description", "https://acme.com/jobs/1", List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            repository.save(acme);
            repository.save(sameData);

            assertThat(repository.findAll()).containsExactlyInAnyOrder(acme, sameData);
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
        void replacesApplicationWithTheSameIdAndRemovesTheLink() {
            var changed = new JobApplication(acme.id(),
                    new JobPosting("Acme", null, "Analyst", null, null, null, "New description", null, List.of()),
                    ApplicationProcess.start("LinkedIn", LocalDate.of(2026, 11, 21)));
            repository.save(acme);
            repository.save(beta);
            repository.save(changed);

            assertThat(repository.findAll()).containsExactlyInAnyOrder(changed, beta);
        }

        @Test
        void savesAndReadsRequirementsInOrderWithEveryImportanceAndMissingLevel() {
            var withRequirements = new JobApplication(
                    new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", null, List.of(
                            new Requirement("Java", "Avançado", Importance.REQUIRED),
                            new Requirement("Inglês", "C1", Importance.DESIRABLE),
                            new Requirement("Node.js", null, Importance.UNSPECIFIED))),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            repository.save(withRequirements);

            assertThat(repository.findById(withRequirements.id())).contains(withRequirements);
        }

        @Test
        void savesAndReadsTheWholeProcess() {
            var application = new JobApplication(
                    new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", null, List.of()),
                    new ApplicationProcess("Gupy", LocalDate.of(2026, 11, 20), "Entrevista técnica realizada, aguardando feedback",
                            LocalDate.of(2026, 11, 24), Situation.OFFER, List.of(
                                    new ProcessStep("Candidatura", LocalDate.of(2026, 11, 20), true, null),
                                    new ProcessStep("Entrevista técnica", LocalDate.of(2026, 11, 24), true, "Kafka"),
                                    new ProcessStep("Entrevista com líder", null, false, null))));
            repository.save(application);

            assertThat(repository.findById(application.id())).contains(application);
        }

        @Test
        void savesAndReadsPostingFields() {
            var application = new JobApplication(
                    new JobPosting("Acme", "Banco Alfa", "Java Developer", "Sênior", "PJ", "R$ 15.000 a R$ 18.000",
                            "Description", null, List.of()),
                    ApplicationProcess.start("Gupy", LocalDate.of(2026, 11, 20)));
            repository.save(application);

            assertThat(repository.findById(application.id())).contains(application);
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
                    new JobPosting("Company " + i, null, "Java Developer", null, null, null, "x".repeat(300_000), null, List.of()),
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
                            "status", AttributeValue.fromS("Candidatura enviada"),
                            "statusUpdatedOn", AttributeValue.fromS("2026-11-20"),
                            "situation", AttributeValue.fromS("ACTIVE"))))));

            assertThat(repository.findAll()).containsExactly(new JobApplication("1",
                    new JobPosting("Acme", null, "Java Developer", null, null, null, "Description", null, List.of()),
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
}
