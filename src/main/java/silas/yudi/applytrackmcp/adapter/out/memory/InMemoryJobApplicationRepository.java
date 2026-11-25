package silas.yudi.applytrackmcp.adapter.out.memory;

import org.springframework.stereotype.Repository;
import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.domain.model.JobApplication;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Provisório, até o DynamoDB: os dados somem quando o processo encerra. */
@Repository
public class InMemoryJobApplicationRepository implements JobApplicationRepository {

    private final List<JobApplication> applications = new CopyOnWriteArrayList<>();

    @Override
    public void save(JobApplication application) {
        applications.add(application);
    }

    @Override
    public List<JobApplication> findAll() {
        return List.copyOf(applications);
    }
}
