package silas.yudi.applytrackmcp.adapter.in.mcp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Datas chegam da IA como texto AAAA-MM-DD. */
final class DateParam {

    private DateParam() {
    }

    /** Em branco não é "manter": seria confirmar algo que não aconteceu. */
    static LocalDate required(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório: " + field);
        }
        return parse(value);
    }

    /** Nulo continua nulo; em branco também falha, como em required. */
    static LocalDate optional(String field, String value) {
        return value == null ? null : required(field, value);
    }

    /** Para campos dentro de objetos (etapas): ausente ou em branco vira nulo. */
    static LocalDate blankAsNull(String value) {
        return value == null || value.isBlank() ? null : parse(value);
    }

    private static LocalDate parse(String value) {
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: " + value + ". Use o formato AAAA-MM-DD");
        }
    }
}
