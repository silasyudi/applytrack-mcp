package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.application.port.out.StorageUnavailableException;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cria a tabela no primeiro uso, não na subida: assim o servidor sobe mesmo com o banco fora,
 * e a IA recebe uma mensagem que explica o problema em vez de não ver as tools.
 */
public class DynamoDbJobApplicationRepository implements JobApplicationRepository {

    private final DynamoDbTable<JobApplicationItem> table;
    private final String unavailableMessage;
    private boolean tableReady;

    /**
     * @param createTable cria a tabela se ela não existir
     * @param endpoint    DynamoDB Local; nulo ou em branco é o DynamoDB da AWS
     */
    public DynamoDbJobApplicationRepository(DynamoDbTable<JobApplicationItem> table, boolean createTable, String endpoint) {
        this.table = table;
        this.tableReady = !createTable;
        this.unavailableMessage = endpoint == null || endpoint.isBlank()
                ? "Banco de dados indisponível. Tente novamente em instantes"
                : ("Banco de dados indisponível em %s. Confira se o Docker está aberto e se o contêiner do DynamoDB "
                        + "está rodando (docker compose up -d na pasta do compose.yaml)").formatted(endpoint);
    }

    @Override
    public void save(JobApplication application) {
        run(() -> table.putItem(JobApplicationItem.from(application)));
    }

    @Override
    public Optional<JobApplication> findById(String id) {
        var key = Key.builder().partitionValue(id).build();
        return call(() -> Optional.ofNullable(table.getItem(request -> request.key(key).consistentRead(true)))
                .map(JobApplicationItem::toDomain));
    }

    // O scan lê a tabela toda (o SDK busca as páginas); basta para centenas de candidaturas
    @Override
    public List<JobApplication> findAll() {
        return call(() -> table.scan(request -> request.consistentRead(true))
                .items()
                .stream()
                .map(JobApplicationItem::toDomain)
                .toList());
    }

    @Override
    public void deleteById(String id) {
        run(() -> table.deleteItem(Key.builder().partitionValue(id).build()));
    }

    private void run(Runnable operation) {
        call(() -> {
            operation.run();
            return null;
        });
    }

    // SdkClientException: o pedido nem chegou ao banco (conexão recusada, tempo esgotado)
    private <T> T call(Supplier<T> operation) {
        try {
            createTableIfNeeded();
            return operation.get();
        } catch (SdkClientException e) {
            throw new StorageUnavailableException(unavailableMessage, e);
        }
    }

    // Só marca como pronta depois de conseguir: se o banco estava fora, tenta de novo na próxima operação
    private void createTableIfNeeded() {
        if (tableReady) {
            return;
        }

        try {
            table.createTable();
        } catch (ResourceInUseException e) {
            // A tabela já existe
        }

        tableReady = true;
    }
}
