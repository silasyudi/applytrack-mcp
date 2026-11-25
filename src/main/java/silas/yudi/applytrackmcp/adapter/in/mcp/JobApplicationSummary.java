package silas.yudi.applytrackmcp.adapter.in.mcp;

import silas.yudi.applytrackmcp.domain.model.JobApplication;

import java.time.LocalDate;

/** Candidatura sem a descrição, para a busca não gastar o contexto da IA. */
public record JobApplicationSummary(String id, String company, String jobTitle, String platform, String link,
                                    LocalDate appliedOn) {

    static JobApplicationSummary from(JobApplication application) {
        return new JobApplicationSummary(application.id(), application.company(), application.jobTitle(),
                application.platform(), application.link(), application.appliedOn());
    }
}
