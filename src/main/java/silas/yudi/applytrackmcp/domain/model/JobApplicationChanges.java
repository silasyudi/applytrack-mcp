package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Campos a alterar. Nulo mantém o valor atual; link em branco remove o link.
 * Requirements, quando enviada, substitui a lista inteira; lista vazia remove todos.
 */
public record JobApplicationChanges(String company, String jobTitle, String description, String platform, String link,
                                    LocalDate appliedOn, List<Requirement> requirements) {

    boolean isEmpty() {
        return Stream.of(company, jobTitle, description, platform, link, appliedOn, requirements).allMatch(Objects::isNull);
    }
}
