package silas.yudi.applytrackmcp.adapter.in.mcp;

import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.Requirement;
import silas.yudi.applytrackmcp.domain.model.Situation;
import silas.yudi.applytrackmcp.domain.model.WaitingFor;

import java.time.LocalDate;
import java.util.List;

/**
 * Candidatura resumida, para a busca não gastar o contexto da IA: sem a descrição, as etapas, a faixa salarial e
 * as observações. Os requisitos resumem a vaga; status, quem precisa agir e última atividade resumem o processo.
 */
public record JobApplicationSummary(String id,
                                    String company,
                                    String finalClient,
                                    String jobTitle,
                                    String seniority,
                                    String contractType,
                                    String link,
                                    List<Requirement> requirements,
                                    String platform,
                                    LocalDate appliedOn,
                                    Situation situation,
                                    String status,
                                    WaitingFor waitingFor,
                                    LocalDate lastActivityOn) {

    static JobApplicationSummary from(JobApplication application) {
        var posting = application.posting();
        var process = application.process();
        return new JobApplicationSummary(application.id(),
                posting.company(),
                posting.finalClient(),
                posting.jobTitle(),
                posting.seniority(),
                posting.contractType(),
                posting.link(),
                posting.requirements(),
                process.platform(),
                process.appliedOn(),
                process.situation(),
                process.status(),
                process.waitingFor(),
                process.lastActivityOn());
    }
}
