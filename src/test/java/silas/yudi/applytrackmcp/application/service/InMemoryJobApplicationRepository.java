package silas.yudi.applytrackmcp.application.service;

import silas.yudi.applytrackmcp.application.port.out.JobApplicationRepository;
import silas.yudi.applytrackmcp.domain.model.JobApplication;

import java.util.ArrayList;
import java.util.List;

/** Fake usado só nos testes. */
class InMemoryJobApplicationRepository implements JobApplicationRepository {

    private final List<JobApplication> applications = new ArrayList<>();

    @Override
    public void save(JobApplication application) {
        applications.add(application);
    }

    @Override
    public List<JobApplication> findAll() {
        return List.copyOf(applications);
    }
}
