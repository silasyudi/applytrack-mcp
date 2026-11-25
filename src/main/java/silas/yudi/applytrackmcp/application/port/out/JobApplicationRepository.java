package silas.yudi.applytrackmcp.application.port.out;

import silas.yudi.applytrackmcp.domain.model.JobApplication;

import java.util.List;

/** Porta de saída: o adaptador do banco vai implementar esta interface. */
public interface JobApplicationRepository {

    void save(JobApplication application);

    List<JobApplication> findAll();
}
