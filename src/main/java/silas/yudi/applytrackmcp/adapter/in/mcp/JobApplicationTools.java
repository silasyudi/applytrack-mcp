package silas.yudi.applytrackmcp.adapter.in.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import silas.yudi.applytrackmcp.application.service.JobApplicationService;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.JobApplicationChanges;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
            @McpToolParam(description = "Link da vaga, se houver", required = false) String link,
            @McpToolParam(description = "Data em que o usuário se candidatou, no formato AAAA-MM-DD. Se ele não disse quando, pergunte") String appliedOn) {
        var application = new JobApplication(company, jobTitle, description, platform, link, parseRequiredDate(appliedOn));
        service.register(application);
        return "Candidatura registrada: %s - %s".formatted(application.company(), application.jobTitle());
    }

    @McpTool(name = "search_job_applications",
            description = "Busca candidaturas já registradas, por exemplo para responder \"já me candidatei nesta empresa?\" "
                    + "ou \"quais vagas pedem Kafka?\". "
                    + "Ignora acentos e maiúsculas. Sem termo, lista todas. "
                    + "Não traz a descrição da vaga: para lê-la, use get_job_application.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public List<JobApplicationSummary> searchJobApplications(
            @McpToolParam(description = "Um único trecho da empresa, do cargo ou da descrição. Para tecnologias, envie uma por busca", required = false) String term) {
        return service.search(term).stream().map(JobApplicationSummary::from).toList();
    }

    @McpTool(name = "get_job_application",
            description = "Traz uma candidatura completa, com a descrição da vaga. Use quando precisar dos detalhes; "
                    + "para listar ou saber se uma vaga existe, search_job_applications basta.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public JobApplication getJobApplication(
            @McpToolParam(description = "Id da candidatura, obtido na busca") String id) {
        return service.get(id);
    }

    @McpTool(name = "update_job_application",
            description = "Altera uma candidatura já registrada. Antes, use search_job_applications para obter o id; "
                    + "se mais de uma servir, pergunte ao usuário qual é. Envie só os campos que ele pediu para mudar: "
                    + "os omitidos ficam como estão.",
            annotations = @McpTool.McpAnnotations(destructiveHint = true, idempotentHint = true))
    public String updateJobApplication(
            @McpToolParam(description = "Id da candidatura, obtido na busca") String id,
            @McpToolParam(description = "Novo nome da empresa", required = false) String company,
            @McpToolParam(description = "Novo nome do cargo", required = false) String jobTitle,
            @McpToolParam(description = "Nova descrição completa da vaga, sem resumir", required = false) String description,
            @McpToolParam(description = "Nova plataforma", required = false) String platform,
            @McpToolParam(description = "Novo link; texto vazio remove o link", required = false) String link,
            @McpToolParam(description = "Nova data da candidatura, no formato AAAA-MM-DD", required = false) String appliedOn) {
        var date = appliedOn == null ? null : parseRequiredDate(appliedOn);
        var updated = service.update(
                id, new JobApplicationChanges(company, jobTitle, description, platform, link, date));
        return "Candidatura atualizada: %s - %s".formatted(updated.company(), updated.jobTitle());
    }

    @McpTool(name = "delete_job_application",
            description = "Remove definitivamente uma candidatura; não dá para desfazer. Antes, use "
                    + "search_job_applications para obter o id, mostre ao usuário a empresa, o cargo e a data, e só "
                    + "remova depois que ele confirmar. Para corrigir dados, use update_job_application.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true))
    public String deleteJobApplication(
            @McpToolParam(description = "Id da candidatura, obtido na busca") String id) {
        var deleted = service.delete(id);
        return "Candidatura removida: %s - %s".formatted(deleted.company(), deleted.jobTitle());
    }

    private static LocalDate parseRequiredDate(String date) {
        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: appliedOn");
        }
        try {
            return LocalDate.parse(date.strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: " + date + ". Use o formato AAAA-MM-DD");
        }
    }
}
