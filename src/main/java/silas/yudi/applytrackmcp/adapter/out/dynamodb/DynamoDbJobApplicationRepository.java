package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import org.springframework.stereotype.Repository;
import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.List;

@Repository
public class DynamoDbJobApplicationRepository implements JobApplicationRepository {

    private final DynamoDbTable<JobApplicationItem> table;

    public DynamoDbJobApplicationRepository(DynamoDbTable<JobApplicationItem> table) {
        this.table = table;
    }

    @Override
    public void save(JobApplication application) {
        table.putItem(JobApplicationItem.from(application));
    }

    // O scan lê a tabela toda (o SDK busca as páginas); basta para centenas de candidaturas
    @Override
    public List<JobApplication> findAll() {
        return table.scan(request -> request.consistentRead(true))
                .items().stream()
                .map(JobApplicationItem::toDomain)
                .toList();
    }
}
