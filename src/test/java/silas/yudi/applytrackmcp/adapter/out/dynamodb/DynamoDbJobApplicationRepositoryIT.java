package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import silas.yudi.applytrackmcp.DynamoDbLocalContainer;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
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
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Roda contra o DynamoDB Local no Docker. */
class DynamoDbJobApplicationRepositoryIT {

    private final JobApplication acme = new JobApplication(
            "Acme", "Java Developer", "Acme description", "Gupy", "https://acme.com/jobs/1", LocalDate.of(2026, 11, 20));
    private final JobApplication beta = new JobApplication(
            "Soluções Beta", "Pessoa Desenvolvedora Sênior", "Descrição com\nduas linhas e 🚀", "WhatsApp", null, LocalDate.of(2026, 11, 20));

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

    @Test
    void findsNothingInAnEmptyTable() {
        assertThat(repository.findAll()).isEmpty();
    }

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
    void readsAllSavedApplications() {
        repository.save(acme);
        repository.save(beta);

        assertThat(repository.findAll()).containsExactlyInAnyOrder(acme, beta);
    }

    @Test
    void doesNotOverwriteAnEqualApplication() {
        repository.save(acme);
        repository.save(acme);

        assertThat(repository.findAll()).containsExactly(acme, acme);
    }

    // Cada resposta do scan traz até 1 MB; aqui são cerca de 1,5 MB
    @Test
    void readsMoreThanOnePage() {
        var large = IntStream.range(0, 5)
                .mapToObj(i -> new JobApplication("Company " + i, "Java Developer", "x".repeat(300_000), "Gupy", null, LocalDate.of(2026, 11, 20)))
                .toList();
        large.forEach(repository::save);

        assertThat(table.scan().stream().count()).isGreaterThan(1);
        assertThat(repository.findAll()).containsExactlyInAnyOrderElementsOf(large);
    }

    @Test
    void storesDateAsIsoText() {
        repository.save(acme);

        var item = client.scan(request -> request.tableName(table.tableName())).items().getFirst();
        assertThat(item.get("appliedOn")).isEqualTo(AttributeValue.fromS("2026-11-20"));
    }
}
