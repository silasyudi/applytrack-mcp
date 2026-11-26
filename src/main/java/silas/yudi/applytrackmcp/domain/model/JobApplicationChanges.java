package silas.yudi.applytrackmcp.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Campos a alterar: primeiro os da vaga, depois os do processo.
 * Nulo mantém o valor atual; texto em branco remove os campos opcionais
 * (cliente final, senioridade, contrato, faixa salarial e link).
 * Campos que andam juntos vêm agrupados: conteúdo (descrição + requisitos) e status (status + data + situação).
 * Steps, quando enviada, substitui a lista inteira; lista vazia remove todas.
 */
public record JobApplicationChanges(String company, String finalClient, String jobTitle, String seniority,
                                    String contractType, String salaryRange, String link, ContentChange content,
                                    String platform, LocalDate appliedOn, StatusChange statusChange,
                                    List<ProcessStep> steps) {

    boolean isEmpty() {
        return Stream.of(company, finalClient, jobTitle, seniority, contractType, salaryRange, link, content,
                        platform, appliedOn, statusChange, steps)
                .allMatch(Objects::isNull);
    }
}
