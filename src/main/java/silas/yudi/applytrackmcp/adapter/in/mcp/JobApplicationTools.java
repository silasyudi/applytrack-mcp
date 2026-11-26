package silas.yudi.applytrackmcp.adapter.in.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import silas.yudi.applytrackmcp.application.service.JobApplicationService;
import silas.yudi.applytrackmcp.domain.model.ApplicationProcess;
import silas.yudi.applytrackmcp.domain.model.ContentChange;
import silas.yudi.applytrackmcp.domain.model.JobApplication;
import silas.yudi.applytrackmcp.domain.model.JobApplicationChanges;
import silas.yudi.applytrackmcp.domain.model.JobPosting;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.Requirement;
import silas.yudi.applytrackmcp.domain.model.Situation;
import silas.yudi.applytrackmcp.domain.model.StatusChange;

import java.util.List;

/** As descrições são lidas pela IA para decidir qual tool usar e como preencher os parâmetros. */
@Component
public class JobApplicationTools {

    private static final String SITUATIONS = "ACTIVE (em andamento), OFFER (proposta), HIRED (contratado), "
            + "REJECTED (reprovado), WITHDRAWN (desistiu) ou CLOSED (vaga fechada)";

    private final JobApplicationService service;

    public JobApplicationTools(JobApplicationService service) {
        this.service = service;
    }

    @McpTool(name = "register_job_application",
            description = "Registra uma candidatura que o usuário fez a uma vaga de emprego e devolve o id. "
                    + "Antes, use search_job_applications com o nome da empresa: se houver candidatura parecida "
                    + "(mesma empresa e cargo, ou descrição muito semelhante), mostre-a ao usuário e pergunte se é "
                    + "outra vaga. Cadastros repetidos são permitidos: a decisão é do usuário. "
                    + "Preencha os campos da vaga com o que está no anúncio, sem inventar; "
                    + "se faltar algum campo obrigatório, pergunte ao usuário.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false,
                    idempotentHint = false, openWorldHint = false))
    public String registerJobApplication(
            @McpToolParam(description = "Nome da empresa que anunciou a vaga") String company,
            @McpToolParam(description = "Empresa onde se trabalha de fato, quando a vaga é de consultoria ou "
                    + "alocação. Omita se a vaga não citar", required = false) String finalClient,
            @McpToolParam(description = "Nome do cargo, como no anúncio") String jobTitle,
            @McpToolParam(description = "Senioridade como está na vaga: Júnior, Pleno, Sênior, Especialista. "
                    + "Omita se a vaga não citar", required = false) String seniority,
            @McpToolParam(description = "Tipo de contrato como está na vaga: CLT, PJ, Cooperado. "
                    + "Obrigatório quando houver faixa salarial", required = false) String contractType,
            @McpToolParam(description = "Faixa salarial anunciada, como está no texto (ex.: R$ 10.000 a R$ 12.000). "
                    + "Exige contractType. Omita se a vaga não citar", required = false) String salaryRange,
            @McpToolParam(description = "Link da vaga, se houver", required = false) String link,
            @McpToolParam(description = "Descrição completa da vaga, sem resumir") String description,
            @McpToolParam(description = "Requisitos citados na descrição: tecnologias, idiomas, certificações. "
                    + "Extraia só o que está escrito, sem inventar", required = false)
            List<RequirementInput> requirements,
            @McpToolParam(description = "Por onde se candidatou: Gupy, LinkedIn, e-mail, WhatsApp etc.")
            String platform,
            @McpToolParam(description = "Data em que o usuário se candidatou, no formato AAAA-MM-DD. "
                    + "Se ele não disse quando, pergunte") String appliedOn,
            @McpToolParam(description = "Status atual em texto livre, como o usuário descreveria: \"Entrevista "
                    + "técnica realizada, aguardando feedback\". Omita no cadastro inicial: o padrão é "
                    + "\"Candidatura enviada\"", required = false) String status,
            @McpToolParam(description = "Data do status, no formato AAAA-MM-DD. Exige status. "
                    + "Omita para usar a data da candidatura", required = false) String statusUpdatedOn,
            @McpToolParam(description = SITUATIONS + ". Exige status. Omita para ACTIVE",
                    required = false) Situation situation,
            @McpToolParam(description = "Etapas do processo, em ordem: candidatura, fit cultural, entrevista "
                    + "técnica etc.", required = false) List<ProcessStepInput> steps) {
        var application = new JobApplication(
                new JobPosting(company, finalClient, jobTitle, seniority, contractType, salaryRange, link, description,
                        toDomain(requirements)),
                ApplicationProcess.start(platform, DateParam.required("appliedOn", appliedOn), status,
                        DateParam.optional("statusUpdatedOn", statusUpdatedOn), situation, toSteps(steps)));
        service.register(application);
        return confirmation("registrada", application);
    }

    @McpTool(name = "search_job_applications",
            description = "Busca candidaturas já registradas, por exemplo para responder \"já me candidatei nesta "
                    + "empresa?\" ou \"quais vagas pedem Kafka?\". Procura o termo como trecho na empresa, no "
                    + "cliente final, no cargo, na descrição da vaga e nos nomes dos requisitos, ignorando acentos e "
                    + "maiúsculas; por ser "
                    + "trecho, \"java\" também encontra \"JavaScript\". Sem termo, lista todas. "
                    + "Resultados da candidatura mais recente para a mais antiga. "
                    + "Cada resultado traz id, vaga (sem descrição e sem faixa salarial), requisitos e status; "
                    + "para a descrição, a faixa salarial e as etapas, use get_job_application.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    openWorldHint = false))
    public List<JobApplicationSummary> searchJobApplications(
            @McpToolParam(description = "Um único trecho a procurar na empresa, no cliente final, no cargo, na "
                    + "descrição ou nos nomes dos requisitos. Para várias tecnologias, faça uma busca para cada",
                    required = false) String term) {
        return service.search(term).stream().map(JobApplicationSummary::from).toList();
    }

    @McpTool(name = "get_job_application",
            description = "Traz uma candidatura completa: vaga com descrição e faixa salarial, e processo com "
                    + "etapas. Use quando precisar dos detalhes; para listar ou saber se uma vaga existe, "
                    + "search_job_applications basta.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    openWorldHint = false))
    public JobApplication getJobApplication(
            @McpToolParam(description = "Id da candidatura, obtido na busca ou no cadastro") String id) {
        return service.get(id);
    }

    @McpTool(name = "update_job_application",
            description = "Altera uma candidatura já registrada e devolve o id. Antes, use search_job_applications "
                    + "para obter o id; se mais de uma servir, pergunte ao usuário qual é. Envie só os campos que "
                    + "ele pediu para mudar: os omitidos ficam como estão. Exceções, porque alguns campos andam "
                    + "juntos: descrição nova exige requirements (lista vazia se a vaga não citar nenhum); data do "
                    + "status e situação exigem status (reenvie o atual, se ele não mudou).",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
                    idempotentHint = true, openWorldHint = false))
    public String updateJobApplication(
            @McpToolParam(description = "Id da candidatura, obtido na busca ou no cadastro") String id,
            @McpToolParam(description = "Novo nome da empresa", required = false) String company,
            @McpToolParam(description = "Novo cliente final; texto vazio remove", required = false) String finalClient,
            @McpToolParam(description = "Novo nome do cargo", required = false) String jobTitle,
            @McpToolParam(description = "Nova senioridade; texto vazio remove", required = false) String seniority,
            @McpToolParam(description = "Novo tipo de contrato; texto vazio remove (não é possível manter faixa "
                    + "salarial sem contrato)", required = false) String contractType,
            @McpToolParam(description = "Nova faixa salarial; exige contrato na candidatura (o atual ou um novo); "
                    + "texto vazio remove", required = false) String salaryRange,
            @McpToolParam(description = "Novo link; texto vazio remove", required = false) String link,
            @McpToolParam(description = "Nova descrição completa da vaga, sem resumir. Exige requirements",
                    required = false) String description,
            @McpToolParam(description = "Requisitos citados na descrição: tecnologias, idiomas, certificações. "
                    + "Extraia só o que está escrito, sem inventar. Substitui a lista inteira; lista vazia remove "
                    + "todos. Pode ser enviada sozinha, para corrigir a lista", required = false)
            List<RequirementInput> requirements,
            @McpToolParam(description = "Nova plataforma", required = false) String platform,
            @McpToolParam(description = "Nova data da candidatura, no formato AAAA-MM-DD", required = false)
            String appliedOn,
            @McpToolParam(description = "Status atual em texto livre, como o usuário descreveria: \"Entrevista "
                    + "técnica realizada, aguardando feedback\". Ao mudar, a data do status passa a ser hoje, se "
                    + "não for informada", required = false) String status,
            @McpToolParam(description = "Data do status, no formato AAAA-MM-DD. Exige status. Informe quando a "
                    + "mudança não foi hoje", required = false) String statusUpdatedOn,
            @McpToolParam(description = SITUATIONS + ". Exige status. Ao mudar, a data do status passa a ser "
                    + "hoje, se não for informada", required = false) Situation situation,
            @McpToolParam(description = "Etapas do processo, em ordem: candidatura, fit cultural, entrevista "
                    + "técnica etc. Substitui a lista inteira; lista vazia remove todas", required = false)
            List<ProcessStepInput> steps) {
        var updated = service.update(id, new JobApplicationChanges(
                company, finalClient, jobTitle, seniority, contractType, salaryRange, link,
                ContentChange.of(description, toDomain(requirements)),
                platform, DateParam.optional("appliedOn", appliedOn),
                StatusChange.of(status, DateParam.optional("statusUpdatedOn", statusUpdatedOn), situation),
                toSteps(steps)));
        return confirmation("atualizada", updated);
    }

    @McpTool(name = "delete_job_application",
            description = "Remove definitivamente uma candidatura; não dá para desfazer. Antes, use "
                    + "search_job_applications para obter o id, mostre ao usuário a empresa, o cargo e a data, e só "
                    + "remova depois que ele confirmar. Para corrigir dados, use update_job_application.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true,
                    openWorldHint = false))
    public String deleteJobApplication(
            @McpToolParam(description = "Id da candidatura, obtido na busca ou no cadastro") String id) {
        var deleted = service.delete(id);
        return "Candidatura removida: %s - %s".formatted(deleted.posting().company(), deleted.posting().jobTitle());
    }

    private static String confirmation(String action, JobApplication application) {
        return "Candidatura %s: %s - %s (id: %s)".formatted(action, application.posting().company(),
                application.posting().jobTitle(), application.id());
    }

    private static List<Requirement> toDomain(List<RequirementInput> requirements) {
        return requirements == null ? null : requirements.stream().map(RequirementInput::toDomain).toList();
    }

    private static List<ProcessStep> toSteps(List<ProcessStepInput> steps) {
        return steps == null ? null : steps.stream().map(ProcessStepInput::toDomain).toList();
    }
}
