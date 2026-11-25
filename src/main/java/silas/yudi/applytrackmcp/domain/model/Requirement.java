package silas.yudi.applytrackmcp.domain.model;

/** Requisito citado na vaga. Ex.: Java / Avançado / REQUIRED, Inglês / C1 / DESIRABLE. Level é texto livre. */
public record Requirement(String name, String level, Importance importance) {

    public Requirement {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: requirement.name");
        }
        name = name.strip();
        level = level == null || level.isBlank() ? null : level.strip();
        importance = importance == null ? Importance.UNSPECIFIED : importance;
    }
}
