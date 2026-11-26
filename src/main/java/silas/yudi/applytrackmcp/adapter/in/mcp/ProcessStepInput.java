package silas.yudi.applytrackmcp.adapter.in.mcp;

import org.springframework.ai.mcp.annotation.McpToolParam;
import silas.yudi.applytrackmcp.domain.model.ProcessStep;

/** Etapa como a IA envia; as anotações montam o schema da tool sem tocar no domínio. */
public record ProcessStepInput(
        @McpToolParam(description = "Nome da etapa: Candidatura, Fit cultural, Entrevista técnica, Entrevista com líder")
        String name,
        @McpToolParam(description = "Data marcada ou realizada, no formato AAAA-MM-DD. Pode ser futura", required = false)
        String date,
        @McpToolParam(description = "true se a etapa já aconteceu", required = false)
        Boolean done,
        @McpToolParam(description = "Observações livres", required = false)
        String notes) {

    ProcessStep toDomain() {
        return new ProcessStep(name, DateParam.blankAsNull(date), Boolean.TRUE.equals(done), notes);
    }
}
