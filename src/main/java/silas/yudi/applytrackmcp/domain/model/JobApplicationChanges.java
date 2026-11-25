package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

/** Campos a alterar. Nulo mantém o valor atual; link em branco remove o link. */
public record JobApplicationChanges(String company, String jobTitle, String description, String platform, String link,
                                    LocalDate appliedOn) {

    boolean isEmpty() {
        return Stream.of(company, jobTitle, description, platform, link, appliedOn).allMatch(Objects::isNull);
    }
}
