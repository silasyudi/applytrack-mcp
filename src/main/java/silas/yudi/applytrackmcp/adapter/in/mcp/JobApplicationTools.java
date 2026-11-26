package silas.yudi.applytrackmcp.adapter.in.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import silas.yudi.applytrackmcp.adapter.in.mcp.JobApplicationToolDescriptions.Register;
import silas.yudi.applytrackmcp.adapter.in.mcp.JobApplicationToolDescriptions.Search;
import silas.yudi.applytrackmcp.adapter.in.mcp.JobApplicationToolDescriptions.Update;
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

import static silas.yudi.applytrackmcp.adapter.in.mcp.JobApplicationToolDescriptions.DELETE;
import static silas.yudi.applytrackmcp.adapter.in.mcp.JobApplicationToolDescriptions.GET;
import static silas.yudi.applytrackmcp.adapter.in.mcp.JobApplicationToolDescriptions.ID;
import static silas.yudi.applytrackmcp.adapter.in.mcp.JobApplicationToolDescriptions.REGISTER;
import static silas.yudi.applytrackmcp.adapter.in.mcp.JobApplicationToolDescriptions.SEARCH;
import static silas.yudi.applytrackmcp.adapter.in.mcp.JobApplicationToolDescriptions.UPDATE;

/**
 * As tools que a IA chama. Os textos que ela lê estão em {@link JobApplicationToolDescriptions}.
 */
@Component
public class JobApplicationTools {

    private final JobApplicationService service;

    public JobApplicationTools(JobApplicationService service) {
        this.service = service;
    }

    @McpTool(name = "register_job_application",
            description = REGISTER,
            annotations = @McpTool.McpAnnotations(destructiveHint = false, openWorldHint = false))
    public String registerJobApplication(
            @McpToolParam(description = Register.COMPANY) String company,
            @McpToolParam(description = Register.FINAL_CLIENT, required = false) String finalClient,
            @McpToolParam(description = Register.JOB_TITLE) String jobTitle,
            @McpToolParam(description = Register.SENIORITY, required = false) String seniority,
            @McpToolParam(description = Register.CONTRACT_TYPE, required = false) String contractType,
            @McpToolParam(description = Register.SALARY_RANGE, required = false) String salaryRange,
            @McpToolParam(description = Register.LINK, required = false) String link,
            @McpToolParam(description = Register.DESCRIPTION) String description,
            @McpToolParam(description = Register.REQUIREMENTS, required = false) List<RequirementInput> requirements,
            @McpToolParam(description = Register.PLATFORM) String platform,
            @McpToolParam(description = Register.APPLIED_ON) String appliedOn,
            @McpToolParam(description = Register.STATUS, required = false) String status,
            @McpToolParam(description = Register.STATUS_UPDATED_ON, required = false) String statusUpdatedOn,
            @McpToolParam(description = Register.SITUATION, required = false) Situation situation,
            @McpToolParam(description = Register.STEPS, required = false) List<ProcessStepInput> steps
    ) {
        var application = new JobApplication(
                new JobPosting(
                        company,
                        finalClient,
                        jobTitle,
                        seniority,
                        contractType,
                        salaryRange,
                        link,
                        description,
                        toRequirements(requirements)
                ),
                ApplicationProcess.start(
                        platform,
                        DateParam.required("appliedOn", appliedOn),
                        status,
                        DateParam.optional("statusUpdatedOn", statusUpdatedOn),
                        situation,
                        toSteps(steps)
                )
        );
        service.register(application);
        return confirmation("registrada", application);
    }

    @McpTool(name = "search_job_applications",
            description = SEARCH,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public List<JobApplicationSummary> searchJobApplications(@McpToolParam(description = Search.TERM, required = false) String term) {
        return toSummaries(service.search(term));
    }

    @McpTool(name = "get_job_application",
            description = GET,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public JobApplication getJobApplication(@McpToolParam(description = ID) String id) {
        return service.get(id);
    }

    @McpTool(name = "update_job_application",
            description = UPDATE,
            annotations = @McpTool.McpAnnotations(idempotentHint = true, openWorldHint = false))
    public String updateJobApplication(
            @McpToolParam(description = ID) String id,
            @McpToolParam(description = Update.COMPANY, required = false) String company,
            @McpToolParam(description = Update.FINAL_CLIENT, required = false) String finalClient,
            @McpToolParam(description = Update.JOB_TITLE, required = false) String jobTitle,
            @McpToolParam(description = Update.SENIORITY, required = false) String seniority,
            @McpToolParam(description = Update.CONTRACT_TYPE, required = false) String contractType,
            @McpToolParam(description = Update.SALARY_RANGE, required = false) String salaryRange,
            @McpToolParam(description = Update.LINK, required = false) String link,
            @McpToolParam(description = Update.DESCRIPTION, required = false) String description,
            @McpToolParam(description = Update.REQUIREMENTS, required = false) List<RequirementInput> requirements,
            @McpToolParam(description = Update.PLATFORM, required = false) String platform,
            @McpToolParam(description = Update.APPLIED_ON, required = false) String appliedOn,
            @McpToolParam(description = Update.STATUS, required = false) String status,
            @McpToolParam(description = Update.STATUS_UPDATED_ON, required = false) String statusUpdatedOn,
            @McpToolParam(description = Update.SITUATION, required = false) Situation situation,
            @McpToolParam(description = Update.STEPS, required = false) List<ProcessStepInput> steps
    ) {
        var changes = JobApplicationChanges.builder()
                .company(company)
                .finalClient(finalClient)
                .jobTitle(jobTitle)
                .seniority(seniority)
                .contractType(contractType)
                .salaryRange(salaryRange)
                .link(link)
                .content(ContentChange.of(description, toRequirements(requirements)))
                .platform(platform)
                .appliedOn(DateParam.optional("appliedOn", appliedOn))
                .statusChange(StatusChange.of(status, DateParam.optional("statusUpdatedOn", statusUpdatedOn), situation))
                .steps(toSteps(steps))
                .build();
        return confirmation("atualizada", service.update(id, changes));
    }

    @McpTool(name = "delete_job_application",
            description = DELETE,
            annotations = @McpTool.McpAnnotations(openWorldHint = false))
    public String deleteJobApplication(@McpToolParam(description = ID) String id) {
        var deleted = service.delete(id);
        return "Candidatura removida: %s - %s".formatted(deleted.posting().company(), deleted.posting().jobTitle());
    }

    private static String confirmation(String action, JobApplication application) {
        return "Candidatura %s: %s - %s (id: %s)".formatted(
                action,
                application.posting().company(),
                application.posting().jobTitle(),
                application.id());
    }

    private static List<JobApplicationSummary> toSummaries(List<JobApplication> applications) {
        return applications.stream().map(JobApplicationSummary::from).toList();
    }

    // Nulo continua nulo: na edição, significa "não enviado", e a lista atual fica como está
    private static List<Requirement> toRequirements(List<RequirementInput> requirements) {
        return requirements == null ? null : requirements.stream().map(RequirementInput::toDomain).toList();
    }

    private static List<ProcessStep> toSteps(List<ProcessStepInput> steps) {
        return steps == null ? null : steps.stream().map(ProcessStepInput::toDomain).toList();
    }
}
