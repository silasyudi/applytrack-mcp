package silas.yudi.applytrackmcp.adapter.in.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import silas.yudi.applytrackmcp.application.service.JobApplicationService;
import silas.yudi.applytrackmcp.domain.model.JobApplication;

import java.util.List;

/** As descrições são lidas pela IA para decidir qual tool usar e como preencher os parâmetros. */
@Component
public class JobApplicationTools {

    private final JobApplicationService service;

    public JobApplicationTools(JobApplicationService service) {
        this.service = service;
    }

    @McpTool(name = "register_job_application",
            description = "Registra uma candidatura que o usuário fez a uma vaga de emprego. "
                    + "Não invente dados: se faltar algum campo obrigatório, pergunte ao usuário.")
    public String registerJobApplication(
            @McpToolParam(description = "Nome da empresa") String company,
            @McpToolParam(description = "Nome do cargo, como no anúncio") String jobTitle,
            @McpToolParam(description = "Descrição completa da vaga, sem resumir") String description,
            @McpToolParam(description = "Por onde se candidatou: Gupy, LinkedIn, e-mail, WhatsApp etc.") String platform,
            @McpToolParam(description = "Link da vaga, se houver", required = false) String link) {
        var application = new JobApplication(company, jobTitle, description, platform, link);
        service.register(application);
        return "Candidatura registrada: %s - %s".formatted(application.company(), application.jobTitle());
    }

    @McpTool(name = "search_job_applications",
            description = "Busca candidaturas já registradas, por exemplo para responder \"já me candidatei nesta empresa?\". "
                    + "Ignora acentos e maiúsculas. Sem termo, lista todas.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public List<JobApplication> searchJobApplications(
            @McpToolParam(description = "Trecho do nome da empresa ou do cargo", required = false) String term) {
        return service.search(term);
    }
}
