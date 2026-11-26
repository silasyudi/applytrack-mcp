package silas.yudi.applytrackmcp.adapter.in.mcp;

import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.Requirement;
import silas.yudi.applytrackmcp.domain.model.Situation;

import java.time.LocalDate;
import java.util.List;

/**
 * Candidatura resumida, para a busca não gastar o contexto da IA: sem a descrição e sem as etapas.
 * Os requisitos resumem a vaga; o status, o processo.
 */
public record JobApplicationSummary(String id, String company, String jobTitle, String platform, String link,
                                    LocalDate appliedOn, List<Requirement> requirements, String status,
                                    LocalDate statusUpdatedOn, Situation situation) {

    static JobApplicationSummary from(JobApplication application) {
        var posting = application.posting();
        var process = application.process();
        return new JobApplicationSummary(application.id(), posting.company(), posting.jobTitle(),
                process.platform(), posting.link(), process.appliedOn(), posting.requirements(),
                process.status(), process.statusUpdatedOn(), process.situation());
    }
}
