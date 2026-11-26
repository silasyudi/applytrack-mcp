package silas.yudi.applytrackmcp.adapter.in.mcp;

import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.Requirement;
import silas.yudi.applytrackmcp.domain.model.Situation;

import java.time.LocalDate;
import java.util.List;

/**
 * Candidatura resumida, para a busca não gastar o contexto da IA: sem a descrição, as etapas e a faixa salarial.
 * Os requisitos resumem a vaga; o status, o processo.
 */
public record JobApplicationSummary(String id, String company, String finalClient, String jobTitle,
                                    String seniority, String contractType, String platform, String link,
                                    LocalDate appliedOn, List<Requirement> requirements, String status,
                                    LocalDate statusUpdatedOn, Situation situation) {

    static JobApplicationSummary from(JobApplication application) {
        var posting = application.posting();
        var process = application.process();
        return new JobApplicationSummary(application.id(), posting.company(), posting.finalClient(), posting.jobTitle(),
                posting.seniority(), posting.contractType(),
                process.platform(), posting.link(), process.appliedOn(), posting.requirements(),
                process.status(), process.statusUpdatedOn(), process.situation());
    }
}
