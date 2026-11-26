package silas.yudi.applytrackmcp.adapter.in.mcp;

import org.springframework.ai.mcp.annotation.McpToolParam;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;
import silas.yudi.applytrackmcp.domain.model.StepState;

/** Etapa como a IA envia; as anotações montam o schema da tool sem tocar no domínio. */
public record ProcessStepInput(
        @McpToolParam(description = JobApplicationToolDescriptions.Step.NAME)
        String name,
        @McpToolParam(description = JobApplicationToolDescriptions.Step.STATE)
        StepState state,
        @McpToolParam(description = JobApplicationToolDescriptions.Step.SCHEDULED_FOR, required = false)
        String scheduledFor,
        @McpToolParam(description = JobApplicationToolDescriptions.Step.COMPLETED_ON, required = false)
        String completedOn,
        @McpToolParam(description = JobApplicationToolDescriptions.Step.FEEDBACK_EXPECTED_BY, required = false)
        String feedbackExpectedBy,
        @McpToolParam(description = JobApplicationToolDescriptions.Step.NOTES, required = false)
        String notes) {

    ProcessStep toDomain() {
        return new ProcessStep(
                name,
                state,
                DateParam.blankAsNull(scheduledFor),
                DateParam.blankAsNull(completedOn),
                DateParam.blankAsNull(feedbackExpectedBy),
                notes
        );
    }
}
