package silas.yudi.applytrackmcp.application.port.out;

import silas.yudi.applytrackmcp.domain.model.JobApplication;

import java.util.List;
import java.util.Optional;

/** Toda operação lança {@link StorageUnavailableException} quando o banco não responde. */
public interface JobApplicationRepository {

    /** Grava uma candidatura nova ou substitui a que tem o mesmo id. */
    void save(JobApplication application);

    Optional<JobApplication> findById(String id);

    List<JobApplication> findAll();

    void deleteById(String id);
}
