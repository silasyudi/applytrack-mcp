package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Campos a alterar. Nulo mantém o valor atual; link em branco remove o link.
 * Listas (requirements, steps), quando enviadas, substituem a lista inteira; lista vazia remove todos.
 */
public record JobApplicationChanges(String company, String jobTitle, String description, String platform, String link,
                                    LocalDate appliedOn, List<Requirement> requirements, String status,
                                    LocalDate statusUpdatedOn, Situation situation, List<ProcessStep> steps) {

    boolean isEmpty() {
        return Stream.of(company, jobTitle, description, platform, link, appliedOn, requirements,
                        status, statusUpdatedOn, situation, steps)
                .allMatch(Objects::isNull);
    }
}
