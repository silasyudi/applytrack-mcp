package silas.yudi.applytrackmcp.domain.model;

/** Validações de texto usadas pelos objetos do domínio. */
final class Fields {

    private Fields() {
    }

    static String required(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: " + field);
        }
        return value.strip();
    }

    /** Em branco vira nulo. */
    static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
