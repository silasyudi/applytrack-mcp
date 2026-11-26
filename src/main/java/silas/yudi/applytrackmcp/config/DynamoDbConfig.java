package silas.yudi.applytrackmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import silas.yudi.applytrackmcp.adapter.out.dynamodb.DynamoDbJobApplicationRepository;
import silas.yudi.applytrackmcp.adapter.out.dynamodb.JobApplicationItem;
import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.config.DynamoDbConfig.DynamoDbProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(DynamoDbProperties.class)
public class DynamoDbConfig {

    /** Sem endpoint, o SDK usa o DynamoDB da AWS e as credenciais do ambiente. */
    @ConfigurationProperties("applytrack.dynamodb")
    public record DynamoDbProperties(String endpoint, String region, String tableName, boolean createTable) {
    }

    @Bean
    DynamoDbClient dynamoDbClient(DynamoDbProperties properties) {
        var builder = DynamoDbClient.builder().region(Region.of(properties.region()));

        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            // O DynamoDB Local exige credenciais, mas aceita qualquer valor
            builder.endpointOverride(URI.create(properties.endpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local", "local")));
        }

        return builder.build();
    }

    @Bean
    JobApplicationRepository jobApplicationRepository(DynamoDbClient client, DynamoDbProperties properties) {
        var table = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build()
                .table(properties.tableName(), TableSchema.fromBean(JobApplicationItem.class));
        return new DynamoDbJobApplicationRepository(table, properties.createTable(), properties.endpoint());
    }
}
