package silas.yudi.applytrackmcp.adapter.in.mcp;

import org.springframework.ai.mcp.annotation.McpToolParam;
import silas.yudi.applytrackmcp.domain.model.Importance;
import silas.yudi.applytrackmcp.domain.model.Requirement;

/** Requisito como a IA envia; as anotações montam o schema da tool sem tocar no domínio. */
public record RequirementInput(
        @McpToolParam(description = "Nome oficial: Java, Spring Boot, Kafka, Inglês")
        String name,
        @McpToolParam(description = "Nível pedido, como está no texto: Avançado, Sênior, C1. Omita se o texto não diz",
                required = false)
        String level,
        @McpToolParam(description = "REQUIRED (obrigatório), DESIRABLE (desejável, diferencial, plus) "
                + "ou UNSPECIFIED (o texto não diz)")
        Importance importance) {

    Requirement toDomain() {
        return new Requirement(name, level, importance);
    }
}
