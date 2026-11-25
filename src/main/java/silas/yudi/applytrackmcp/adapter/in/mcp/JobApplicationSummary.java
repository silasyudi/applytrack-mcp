package silas.yudi.applytrackmcp.adapter.in.mcp;

import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.Requirement;

import java.time.LocalDate;
import java.util.List;

/** Candidatura sem a descrição, para a busca não gastar o contexto da IA. Os requisitos resumem a vaga. */
public record JobApplicationSummary(String id, String company, String jobTitle, String platform, String link,
                                    LocalDate appliedOn, List<Requirement> requirements) {

    static JobApplicationSummary from(JobApplication application) {
        return new JobApplicationSummary(application.id(), application.company(), application.jobTitle(),
                application.platform(), application.link(), application.appliedOn(), application.requirements());
    }
}
