package silas.yudi.applytrackmcp.adapter.in.mcp;

import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.JobPosting;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.Situation;
import silas.yudi.applytrackmcp.domain.model.WaitingFor;

import java.time.LocalDate;
import java.util.List;

/**
 * Candidatura completa, como a IA recebe no detalhe. Inclui os campos calculados do processo
 * (status, quem precisa agir e última atividade), que o domínio calcula e não guarda.
 */
public record JobApplicationDetail(String id, JobPosting posting, Process process) {

    public record Process(String platform,
                          LocalDate appliedOn,
                          Situation situation,
                          String notes,
                          String status,
                          WaitingFor waitingFor,
                          LocalDate lastActivityOn,
                          List<ProcessStep> steps) {
    }

    static JobApplicationDetail from(JobApplication application) {
        var process = application.process();
        return new JobApplicationDetail(application.id(), application.posting(), new Process(
                process.platform(),
                process.appliedOn(),
                process.situation(),
                process.notes(),
                process.status(),
                process.waitingFor(),
                process.lastActivityOn(),
                process.steps()
        ));
    }
}
